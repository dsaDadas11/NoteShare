package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateNoteRequest;
import com.example.noteshare.dto.response.*;
import com.example.noteshare.entity.Note;
import com.example.noteshare.entity.NoteImage;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteImageRepository noteImageRepository;
    private final UserRepository userRepository;
    private final LikeRelRepository likeRelRepository;
    private final FollowService followService;

    public NoteService(NoteRepository noteRepository,
                       NoteImageRepository noteImageRepository,
                       UserRepository userRepository,
                       LikeRelRepository likeRelRepository,
                       FollowService followService) {
        this.noteRepository = noteRepository;
        this.noteImageRepository = noteImageRepository;
        this.userRepository = userRepository;
        this.likeRelRepository = likeRelRepository;
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

        return buildNoteResponse(note, null);
    }

    /**
     * 首页笔记列表（分页，按时间倒序）
     */
    public PageResponse<NoteResponse> listNotes(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Note> notePage = noteRepository.findAll(pageable);

        List<NoteResponse> items = notePage.getContent().stream()
                .map(note -> buildNoteResponse(note, null))
                .toList();

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

        List<NoteResponse> items = notePage.getContent().stream()
                .map(note -> buildNoteResponse(note, null))
                .toList();

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

        // 手动删除关联数据
        noteImageRepository.deleteByNoteId(noteId);
        noteRepository.delete(note);
    }

    /**
     * 查询用户发布的笔记（分页）
     */
    public PageResponse<NoteResponse> listUserNotes(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Note> notePage = noteRepository.findByAuthorId(authorId, pageable);

        List<NoteResponse> items = notePage.getContent().stream()
                .map(note -> buildNoteResponse(note, null))
                .toList();

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
        return resp;
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
}
