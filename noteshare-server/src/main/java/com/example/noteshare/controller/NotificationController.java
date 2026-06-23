package com.example.noteshare.controller;

import com.example.noteshare.common.ApiResponse;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.response.NotificationResponse;
import com.example.noteshare.dto.response.UnreadCountResponse;
import com.example.noteshare.security.SecurityUtil;
import com.example.noteshare.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 获取通知列表（分页） */
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.currentUserId();
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        return ApiResponse.ok(notificationService.listNotifications(userId, page, size));
    }

    /** 获取未读通知数量 */
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = SecurityUtil.currentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok();
    }
}
