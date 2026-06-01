package com.example.noteshare.controller;

import com.example.noteshare.common.ApiResponse;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.request.UpdateProfileRequest;
import com.example.noteshare.dto.response.NoteResponse;
import com.example.noteshare.dto.response.UserResponse;
import com.example.noteshare.security.SecurityUtil;
import com.example.noteshare.service.FollowService;
import com.example.noteshare.service.NoteService;
import com.example.noteshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final NoteService noteService;

    public UserController(UserService userService,
                          FollowService followService,
                          NoteService noteService) {
        this.userService = userService;
        this.followService = followService;
        this.noteService = noteService;
    }

    /** 当前用户资料（需认证） */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(userService.getCurrentUser(userId));
    }

    /** 编辑资料（需认证） */
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(userService.updateProfile(userId, req));
    }

    /** 查看他人资料（公开，含关注/粉丝数、是否已关注） */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserProfile(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.currentUserIdOrNull();
        return ApiResponse.ok(userService.getUserProfile(id, currentUserId));
    }

    /** 获取用户发布的笔记列表（公开） */
    @GetMapping("/{id}/notes")
    public ApiResponse<PageResponse<NoteResponse>> getUserNotes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        return ApiResponse.ok(noteService.listUserNotes(id, page, size));
    }

    /** 关注（需认证） */
    @PostMapping("/{id}/follow")
    public ApiResponse<Void> follow(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        followService.follow(userId, id);
        return ApiResponse.ok();
    }

    /** 取消关注（需认证） */
    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        followService.unfollow(userId, id);
        return ApiResponse.ok();
    }
}
