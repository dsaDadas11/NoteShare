package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateCommentRequest;
import com.example.noteshare.dto.response.CommentResponse;
import com.example.noteshare.dto.response.UserBrief;
import com.example.noteshare.entity.Comment;
import com.example.noteshare.repository.CommentRepository;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          NoteRepository noteRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    /**
     * 发表评论
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long noteId, CreateCommentRequest req) {
        // 校验笔记存在
        if (!noteRepository.existsById(noteId)) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setNoteId(noteId);
        comment.setContent(req.getContent());
        commentRepository.save(comment);

        // 更新评论计数
        noteRepository.incrementCommentCount(noteId);

        return buildCommentResponse(comment);
    }

    /**
     * 评论列表（分页）
     */
    public PageResponse<CommentResponse> listComments(Long noteId, int page, int size) {
        Page<Comment> commentPage = commentRepository.findByNoteIdOrderByCreatedAtDesc(
                noteId, PageRequest.of(page - 1, size));

        List<CommentResponse> items = commentPage.getContent().stream()
                .map(this::buildCommentResponse)
                .toList();

        PageResponse<CommentResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(commentPage.getTotalElements());
        resp.setHasMore(commentPage.hasNext());
        return resp;
    }

    private CommentResponse buildCommentResponse(Comment comment) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setContent(comment.getContent());
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setAuthor(userRepository.findById(comment.getUserId())
                .map(u -> new UserBrief(u.getId(), u.getUsername(), u.getNickname(), u.getAvatarUrl()))
                .orElse(new UserBrief(comment.getUserId(), "unknown", "已注销用户", null)));
        return resp;
    }
}
