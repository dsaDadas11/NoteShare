package com.example.noteshare.common;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // ===== 通用 =====
    SUCCESS(0, "success"),
    INTERNAL_ERROR(50000, "服务器内部错误"),
    PARAM_INVALID(40000, "参数校验失败"),

    // ===== 认证 (401xx) =====
    AUTH_TOKEN_MISSING(40100, "未提供认证令牌"),
    AUTH_TOKEN_INVALID(40101, "令牌无效或已过期"),
    AUTH_TOKEN_EXPIRED(40102, "令牌已过期"),
    AUTH_UNAUTHORIZED(40103, "未登录"),
    AUTH_FORBIDDEN(40104, "无权操作"),

    // ===== 注册 / 登录 (400xx) =====
    REGISTER_USERNAME_EXISTS(40010, "用户名已存在"),
    REGISTER_USERNAME_INVALID(40011, "用户名格式不正确（3-50字符，字母数字下划线）"),
    REGISTER_PASSWORD_INVALID(40012, "密码长度需 6-50 字符"),
    LOGIN_FAILED(40020, "用户名或密码错误"),

    // ===== 用户 (402xx) =====
    USER_NOT_FOUND(40200, "用户不存在"),
    FOLLOW_SELF(40210, "不能关注自己"),
    FOLLOW_ALREADY(40211, "已关注该用户"),
    FOLLOW_NOT_FOUND(40212, "未关注该用户"),

    // ===== 笔记 (403xx) =====
    NOTE_NOT_FOUND(40300, "笔记不存在"),
    NOTE_FORBIDDEN(40301, "无权操作该笔记"),
    NOTE_TITLE_REQUIRED(40310, "标题不能为空"),
    NOTE_TITLE_TOO_LONG(40311, "标题不能超过 100 字符"),
    NOTE_CONTENT_REQUIRED(40312, "正文不能为空"),

    // ===== 点赞 (404xx) =====
    LIKE_ALREADY(40400, "已经点赞过了"),
    LIKE_NOT_FOUND(40401, "未点赞"),

    // ===== 评论 (405xx) =====
    COMMENT_CONTENT_REQUIRED(40500, "评论内容不能为空"),

    // ===== 文件上传 (406xx) =====
    FILE_EMPTY(40600, "文件不能为空"),
    FILE_TYPE_NOT_ALLOWED(40601, "仅支持 jpg/png/gif/webp 格式"),
    FILE_TOO_LARGE(40602, "单张图片不能超过 5MB"),
    FILE_UPLOAD_FAILED(40603, "文件上传失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
