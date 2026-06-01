package com.example.noteshare.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类：从 SecurityContext 获取当前登录用户 ID
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户 ID（必须已认证，否则抛异常）
     */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("用户未登录");
        }
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    /**
     * 获取当前登录用户 ID（未登录返回 null，用于公开接口判断点赞状态等）
     */
    public static Long currentUserIdOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
