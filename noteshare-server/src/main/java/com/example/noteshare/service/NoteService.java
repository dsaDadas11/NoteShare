package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateNoteRequest;
import com.example.noteshare.dto.response.*;
import com.example.noteshare.entity.Note;
import com.example.noteshare.entity.NoteImage;
import com.example.noteshare.entity.Comment;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteImageRepository noteImageRepository;
    private final NoteVideoRepository noteVideoRepository;
    private final UserRepository userRepository;
    private final LikeRelRepository likeRelRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRelRepository commentLikeRelRepository;
    private final FollowService followService;

    public NoteService(NoteRepository noteRepository,
                       NoteImageRepository noteImageRepository,
                       NoteVideoRepository noteVideoRepository,
                       UserRepository userRepository,
                       LikeRelRepository likeRelRepository,
                       CommentRepository commentRepository,
                       CommentLikeRelRepository commentLikeRelRepository,
                       FollowService followService) {
        this.noteRepository = noteRepository;
        this.noteImageRepository = noteImageRepository;
        this.noteVideoRepository = noteVideoRepository;
        this.userRepository = userRepository;
        this.likeRelRepository = likeRelRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRelRepository = commentLikeRelRepository;
        this.followService = followService;
    }

    /**
     * 发布笔记
     */
    @Transactional
    public NoteResponse createNote(Long userId, CreateNoteRequest req) {
        // 保存笔记
        Note note = new Note();
        note.setAuthorId(userId);
        note.setTitle(req.getTitle());
        note.setContent(req.getContent());
        noteRepository.save(note);

        // 保存图片
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size() && i < 3; i++) {
                NoteImage img = new NoteImage();
                img.setNoteId(note.getId());
                img.setUrl(req.getImageUrls().get(i));
                img.setSort(i);
                noteImageRepository.save(img);
            }
        }

        // 保存视频
        if (req.getVideoUrl() != null && !req.getVideoUrl().isBlank()) {
            com.example.noteshare.entity.NoteVideo video = new com.example.noteshare.entity.NoteVideo();
            video.setNoteId(note.getId());
            video.setUrl(req.getVideoUrl());
            noteVideoRepository.save(video);
        }

        return buildNoteResponse(note, null);
    }

    /**
     * 首页笔记列表（分页，按时间倒序）
     */
    public PageResponse<NoteResponse> listNotes(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Note> notePage = noteRepository.findAll(pageable);

        List<NoteResponse> items = buildNoteResponses(notePage.getContent(), null);

        PageResponse<NoteResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(notePage.getTotalElements());
        resp.setHasMore(notePage.hasNext());
        return resp;
    }

    /**
     * 笔记详情（含图片、作者、是否已赞）
     */
    public NoteDetailResponse getNoteDetail(Long noteId, Long currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        NoteDetailResponse resp = new NoteDetailResponse();
        resp.setId(note.getId());
        resp.setTitle(note.getTitle());
        resp.setContent(note.getContent());
        resp.setLikeCount(note.getLikeCount());
        resp.setCommentCount(note.getCommentCount());
        resp.setCreatedAt(note.getCreatedAt());

        // 作者信息
        resp.setAuthor(buildUserBrief(note.getAuthorId()));

        // 图片列表
        resp.setImages(buildImageInfoList(noteId));

        // 视频
        resp.setVideoUrl(getVideoUrl(noteId));

        // 是否已赞
        if (currentUserId != null) {
            resp.setLiked(likeRelRepository.existsByUserIdAndNoteId(currentUserId, noteId));
            boolean authorSelf = currentUserId.equals(note.getAuthorId());
            resp.setAuthorSelf(authorSelf);
            resp.setAuthorFollowed(!authorSelf && followService.isFollowing(currentUserId, note.getAuthorId()));
        } else {
            resp.setAuthorSelf(false);
            resp.setAuthorFollowed(false);
        }

        return resp;
    }

    /**
     * 搜索笔记（关键词模糊匹配标题+正文）
     */
    public PageResponse<NoteResponse> searchNotes(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Note> notePage = noteRepository.searchByKeyword(keyword, pageable);

        List<NoteResponse> items = buildNoteResponses(notePage.getContent(), null);

        PageResponse<NoteResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(notePage.getTotalElements());
        resp.setHasMore(notePage.hasNext());
        return resp;
    }

    /**
     * 删除笔记（仅作者可删）
     */
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_FORBIDDEN);
        }

        // 手动删除所有关联数据
        // 1. 删除评论的点赞（comment_like_rel）
        List<Long> commentIds = commentRepository.findByNoteId(noteId)
                .stream().map(Comment::getId).toList();
        if (!commentIds.isEmpty()) {
            commentLikeRelRepository.deleteByCommentIdIn(commentIds);
        }
        // 2. 删除评论
        commentRepository.deleteByNoteId(noteId);
        // 3. 删除点赞关系
        likeRelRepository.deleteByNoteId(noteId);
        // 4. 删除视频
        noteVideoRepository.deleteByNoteId(noteId);
        // 5. 删除图片
        noteImageRepository.deleteByNoteId(noteId);
        // 6. 删除笔记
        noteRepository.delete(note);
    }

    /**
     * 查询用户发布的笔记（分页）
     */
    public PageResponse<NoteResponse> listUserNotes(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Note> notePage = noteRepository.findByAuthorId(authorId, pageable);

        List<NoteResponse> items = buildNoteResponses(notePage.getContent(), null);

        PageResponse<NoteResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(notePage.getTotalElements());
        resp.setHasMore(notePage.hasNext());
        return resp;
    }

    // ===== 内部辅助方法 =====

    private NoteResponse buildNoteResponse(Note note, Long currentUserId) {
        NoteResponse resp = new NoteResponse();
        resp.setId(note.getId());
        resp.setTitle(note.getTitle());
        resp.setContent(note.getContent());
        resp.setLikeCount(note.getLikeCount());
        resp.setCommentCount(note.getCommentCount());
        resp.setCreatedAt(note.getCreatedAt());
        resp.setAuthor(buildUserBrief(note.getAuthorId()));
        resp.setImages(buildImageInfoList(note.getId()));
        resp.setVideoUrl(getVideoUrl(note.getId()));
        return resp;
    }

    /**
     * 批量构建笔记响应（避免 N+1 查询）
     * 对列表接口统一使用此方法，一次性加载所有关联数据。
     */
    private List<NoteResponse> buildNoteResponses(List<Note> notes, Long currentUserId) {
        if (notes.isEmpty()) return List.of();

        List<Long> noteIds = notes.stream().map(Note::getId).toList();

        // 1. 批量加载作者
        List<Long> authorIds = notes.stream().map(Note::getAuthorId).distinct().toList();
        Map<Long, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 2. 批量加载图片（按 noteId 分组，保持 sort 顺序）
        Map<Long, List<NoteImage>> imageMap = noteImageRepository.findByNoteIdInOrderByNoteIdAscSortAsc(noteIds).stream()
                .collect(Collectors.groupingBy(NoteImage::getNoteId));

        // 3. 批量加载视频
        Map<Long, com.example.noteshare.entity.NoteVideo> videoMap = noteVideoRepository.findByNoteIdIn(noteIds).stream()
                .collect(Collectors.toMap(com.example.noteshare.entity.NoteVideo::getNoteId, v -> v, (a, b) -> a));

        return notes.stream().map(note -> {
            NoteResponse resp = new NoteResponse();
            resp.setId(note.getId());
            resp.setTitle(note.getTitle());
            resp.setContent(note.getContent());
            resp.setLikeCount(note.getLikeCount());
            resp.setCommentCount(note.getCommentCount());
            resp.setCreatedAt(note.getCreatedAt());

            // 作者
            User author = authorMap.get(note.getAuthorId());
            if (author != null) {
                resp.setAuthor(new UserBrief(author.getId(), author.getUsername(), author.getNickname(), author.getAvatarUrl()));
            } else {
                resp.setAuthor(new UserBrief(note.getAuthorId(), "unknown", "已注销用户", null));
            }

            // 图片
            List<NoteImage> images = imageMap.getOrDefault(note.getId(), List.of());
            resp.setImages(images.stream()
                    .map(img -> new ImageInfo(img.getId(), img.getUrl(), img.getSort()))
                    .toList());

            // 视频
            com.example.noteshare.entity.NoteVideo video = videoMap.get(note.getId());
            resp.setVideoUrl(video != null ? video.getUrl() : null);

            return resp;
        }).toList();
    }

    private UserBrief buildUserBrief(Long userId) {
        return userRepository.findById(userId)
                .map(u -> new UserBrief(u.getId(), u.getUsername(), u.getNickname(), u.getAvatarUrl()))
                .orElse(new UserBrief(userId, "unknown", "已注销用户", null));
    }

    private List<ImageInfo> buildImageInfoList(Long noteId) {
        return noteImageRepository.findByNoteIdOrderBySortAsc(noteId).stream()
                .map(img -> new ImageInfo(img.getId(), img.getUrl(), img.getSort()))
                .toList();
    }

    private String getVideoUrl(Long noteId) {
        return noteVideoRepository.findByNoteId(noteId)
                .map(com.example.noteshare.entity.NoteVideo::getUrl)
                .orElse(null);
    }
}
