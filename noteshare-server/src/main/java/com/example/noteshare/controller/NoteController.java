package com.example.noteshare.controller;

import com.example.noteshare.common.ApiResponse;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.CreateNoteRequest;
import com.example.noteshare.dto.request.CreateCommentRequest;
import com.example.noteshare.dto.response.*;
import com.example.noteshare.security.SecurityUtil;
import com.example.noteshare.service.CommentService;
import com.example.noteshare.service.LikeService;
import com.example.noteshare.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记控制器：CRUD + 搜索 + 点赞 + 评论
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final LikeService likeService;
    private final CommentService commentService;

    public NoteController(NoteService noteService,
                          LikeService likeService,
                          CommentService commentService) {
        this.noteService = noteService;
        this.likeService = likeService;
        this.commentService = commentService;
    }

    /** 发布笔记（需认证） */
    @PostMapping
    public ApiResponse<NoteResponse> createNote(@Valid @RequestBody CreateNoteRequest req) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(noteService.createNote(userId, req));
    }

    /** 首页笔记列表（公开，分页） */
    @GetMapping
    public ApiResponse<PageResponse<NoteResponse>> listNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        return ApiResponse.ok(noteService.listNotes(page, size));
    }

    /** 搜索笔记（公开，关键词模糊匹配） */
    @GetMapping("/search")
    public ApiResponse<PageResponse<NoteResponse>> searchNotes(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        keyword = keyword.trim();
        if (keyword.isEmpty()) {
            return ApiResponse.ok(new PageResponse<>());
        }
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        return ApiResponse.ok(noteService.searchNotes(keyword, page, size));
    }

    /** 笔记详情（公开，含图片、作者、是否已赞） */
    @GetMapping("/{id}")
    public ApiResponse<NoteDetailResponse> getNoteDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.currentUserIdOrNull();
        return ApiResponse.ok(noteService.getNoteDetail(id, currentUserId));
    }

    /** 删除笔记（需认证，仅作者可删） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNote(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        noteService.deleteNote(id, userId);
        return ApiResponse.ok();
    }

    /** 点赞（需认证） */
    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        likeService.like(userId, id);
        return ApiResponse.ok();
    }

    /** 取消点赞（需认证） */
    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlike(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        likeService.unlike(userId, id);
        return ApiResponse.ok();
    }

    /** 评论列表（公开，分页） */
    @GetMapping("/{id}/comments")
    public ApiResponse<PageResponse<CommentResponse>> listComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        Long currentUserId = SecurityUtil.currentUserIdOrNull();
        return ApiResponse.ok(commentService.listComments(id, currentUserId, page, size));
    }

    /** 发表评论（需认证） */
    @PostMapping("/{id}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest req) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(commentService.createComment(userId, id, req));
    }

    /** 删除评论（需认证，仅评论作者可删） */
    @DeleteMapping("/{id}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id,
                                           @PathVariable Long commentId) {
        Long userId = SecurityUtil.currentUserId();
        commentService.deleteComment(userId, id, commentId);
        return ApiResponse.ok();
    }

    /** 获取评论的回复列表（公开） */
    @GetMapping("/{id}/comments/{commentId}/replies")
    public ApiResponse<List<CommentResponse>> listReplies(
            @PathVariable Long id,
            @PathVariable Long commentId) {
        commentService.validateCommentBelongsToNote(commentId, id);
        Long currentUserId = SecurityUtil.currentUserIdOrNull();
        return ApiResponse.ok(commentService.listReplies(commentId, currentUserId));
    }

    /** 点赞评论（需认证） */
    @PostMapping("/{id}/comments/{commentId}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long id,
                                         @PathVariable Long commentId) {
        commentService.validateCommentBelongsToNote(commentId, id);
        Long userId = SecurityUtil.currentUserId();
        commentService.likeComment(userId, commentId);
        return ApiResponse.ok();
    }

    /** 取消点赞评论（需认证） */
    @DeleteMapping("/{id}/comments/{commentId}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long id,
                                            @PathVariable Long commentId) {
        commentService.validateCommentBelongsToNote(commentId, id);
        Long userId = SecurityUtil.currentUserId();
        commentService.unlikeComment(userId, commentId);
        return ApiResponse.ok();
    }
}
