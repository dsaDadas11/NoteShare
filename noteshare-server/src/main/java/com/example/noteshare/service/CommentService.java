package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateCommentRequest;
import com.example.noteshare.dto.response.CommentResponse;
import com.example.noteshare.dto.response.UserBrief;
import com.example.noteshare.entity.Comment;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CommentLikeRelRepository commentLikeRelRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          NoteRepository noteRepository,
                          UserRepository userRepository,
                          CommentLikeRelRepository commentLikeRelRepository,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.commentLikeRelRepository = commentLikeRelRepository;
        this.notificationService = notificationService;
    }

    /**
     * 发表评论（支持回复）
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long noteId, CreateCommentRequest req) {
        if (!noteRepository.existsById(noteId)) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        // 如果是回复，校验父评论存在且属于同一笔记
        Long parentId = req.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_PARENT_NOT_FOUND));
            if (!parent.getNoteId().equals(noteId)) {
                throw new BusinessException(ErrorCode.COMMENT_PARENT_NOT_FOUND);
            }
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setNoteId(noteId);
        comment.setParentId(parentId);
        comment.setContent(req.getContent());
        comment.setReplyToAuthor(req.getReplyToAuthor());
        commentRepository.save(comment);

        // 更新计数
        noteRepository.incrementCommentCount(noteId);
        if (parentId != null) {
            commentRepository.incrementReplyCount(parentId);
        }

        // 触发评论通知（仅保存到数据库）
        notificationService.createCommentNotification(userId, noteId, req.getContent());

        return buildCommentResponse(comment, userId);
    }

    /**
     * 评论列表（分页，顶级评论 + 每条带最近3条回复）
     */
    public PageResponse<CommentResponse> listComments(Long noteId, Long currentUserId, int page, int size) {
        Page<Comment> commentPage = commentRepository.findByNoteIdAndParentIdIsNullOrderByCreatedAtDesc(
                noteId, PageRequest.of(page - 1, size));

        List<Comment> topComments = commentPage.getContent();
        List<Long> topIds = topComments.stream().map(Comment::getId).toList();

        // 批量加载所有顶级评论的回复（按时间正序）
        Map<Long, List<Comment>> replyMap = Collections.emptyMap();
        if (!topIds.isEmpty()) {
            List<Comment> allReplies = commentRepository.findByParentIdInOrderByCreatedAtAsc(topIds);
            replyMap = allReplies.stream()
                    .collect(Collectors.groupingBy(Comment::getParentId));
        }

        // 收集所有评论（顶级 + 回复）用于批量加载
        List<Comment> allComments = new ArrayList<>(topComments);
        replyMap.values().forEach(allComments::addAll);
        List<Long> allCommentIds = allComments.stream().map(Comment::getId).toList();

        // 批量加载用户信息
        Set<Long> userIds = allComments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 批量加载点赞状态
        Set<Long> likedCommentIds = Collections.emptySet();
        if (currentUserId != null && !allCommentIds.isEmpty()) {
            likedCommentIds = new HashSet<>(commentLikeRelRepository.findCommentIdByUserIdAndCommentIdIn(currentUserId, allCommentIds));
        }
        Set<Long> finalLikedCommentIds = likedCommentIds;

        Map<Long, List<Comment>> finalReplyMap = replyMap;
        List<CommentResponse> items = topComments.stream()
                .map(comment -> {
                    CommentResponse resp = buildCommentResponse(comment, currentUserId, userMap, finalLikedCommentIds);
                    List<Comment> replies = finalReplyMap.getOrDefault(comment.getId(), Collections.emptyList());
                    // 只返回最近3条回复用于预览
                    int size1 = replies.size();
                    int from = Math.max(0, size1 - 3);
                    List<Comment> lastReplies = new ArrayList<>(replies.subList(from, size1));
                    Collections.reverse(lastReplies);
                    List<CommentResponse> replyResponses = lastReplies.stream()
                            .map(r -> buildCommentResponse(r, currentUserId, userMap, finalLikedCommentIds))
                            .toList();
                    resp.setReplies(replyResponses);
                    return resp;
                })
                .toList();

        PageResponse<CommentResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(commentPage.getTotalElements());
        resp.setHasMore(commentPage.hasNext());
        return resp;
    }

    /**
     * 加载某条评论的所有回复
     */
    public List<CommentResponse> listReplies(Long commentId, Long currentUserId) {
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
                .map(r -> buildCommentResponse(r, currentUserId))
                .toList();
    }

    /**
     * 删除评论（仅评论作者可删）
     */
    @Transactional
    public void deleteComment(Long userId, Long noteId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getNoteId().equals(noteId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
        }

        // 递归收集所有子孙回复（广度优先）
        List<Comment> allDescendants = new ArrayList<>();
        collectDescendants(commentId, allDescendants);

        // 删除子孙回复的点赞关系
        if (!allDescendants.isEmpty()) {
            List<Long> descendantIds = allDescendants.stream()
                    .map(Comment::getId)
                    .toList();
            commentLikeRelRepository.deleteByCommentIdIn(descendantIds);
        }

        // 删除所有子孙回复
        if (!allDescendants.isEmpty()) {
            commentRepository.deleteAll(allDescendants);
        }

        // 删除评论本体的点赞关系
        commentLikeRelRepository.deleteByCommentId(commentId);

        // 删除评论本体
        commentRepository.delete(comment);

        // 递减笔记评论计数：评论本体 + 所有子孙回复
        int totalDecrement = 1 + allDescendants.size();
        for (int i = 0; i < totalDecrement; i++) {
            noteRepository.decrementCommentCount(noteId);
        }

        // 如果是回复，减少父评论的回复数
        if (comment.getParentId() != null) {
            commentRepository.decrementReplyCount(comment.getParentId());
        }
    }

    /**
     * 递归收集指定评论下的所有子孙回复
     */
    private void collectDescendants(Long parentId, List<Comment> result) {
        List<Comment> children = commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        if (!children.isEmpty()) {
            result.addAll(children);
            for (Comment child : children) {
                collectDescendants(child.getId(), result);
            }
        }
    }

    /**
     * 校验评论属于指定笔记（防止 IDOR 越权操作）
     */
    public void validateCommentBelongsToNote(Long commentId, Long noteId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getNoteId().equals(noteId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    /**
     * 点赞评论
     */
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (commentLikeRelRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new BusinessException(ErrorCode.COMMENT_LIKE_ALREADY);
        }
        commentLikeRelRepository.save(createCommentLike(userId, commentId));
        commentRepository.incrementLikeCount(commentId);
    }

    /**
     * 取消点赞评论
     */
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        if (!commentLikeRelRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new BusinessException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
        }
        commentLikeRelRepository.deleteByUserIdAndCommentId(userId, commentId);
        commentRepository.decrementLikeCount(commentId);
    }

    private com.example.noteshare.entity.CommentLikeRel createCommentLike(Long userId, Long commentId) {
        com.example.noteshare.entity.CommentLikeRel rel = new com.example.noteshare.entity.CommentLikeRel();
        rel.setUserId(userId);
        rel.setCommentId(commentId);
        return rel;
    }

    private CommentResponse buildCommentResponse(Comment comment, Long currentUserId,
                                                  Map<Long, User> userMap, Set<Long> likedCommentIds) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setContent(comment.getContent());
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setParentId(comment.getParentId());
        resp.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        resp.setReplyCount(comment.getReplyCount() != null ? comment.getReplyCount() : 0);
        resp.setReplyToAuthor(comment.getReplyToAuthor());
        resp.setMine(currentUserId != null && currentUserId.equals(comment.getUserId()));
        resp.setLiked(currentUserId != null && likedCommentIds.contains(comment.getId()));
        User user = userMap.get(comment.getUserId());
        resp.setAuthor(user != null
                ? new UserBrief(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl())
                : new UserBrief(comment.getUserId(), "unknown", "已注销用户", null));
        return resp;
    }

    private CommentResponse buildCommentResponse(Comment comment, Long currentUserId) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setContent(comment.getContent());
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setParentId(comment.getParentId());
        resp.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        resp.setReplyCount(comment.getReplyCount() != null ? comment.getReplyCount() : 0);
        resp.setReplyToAuthor(comment.getReplyToAuthor());
        resp.setMine(currentUserId != null && currentUserId.equals(comment.getUserId()));
        if (currentUserId != null) {
            resp.setLiked(commentLikeRelRepository.existsByUserIdAndCommentId(currentUserId, comment.getId()));
        }
        resp.setAuthor(userRepository.findById(comment.getUserId())
                .map(u -> new UserBrief(u.getId(), u.getUsername(), u.getNickname(), u.getAvatarUrl()))
                .orElse(new UserBrief(comment.getUserId(), "unknown", "已注销用户", null)));
        return resp;
    }
}
