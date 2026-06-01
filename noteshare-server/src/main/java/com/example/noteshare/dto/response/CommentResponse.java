package com.example.noteshare.dto.response;

import java.time.LocalDateTime;

/**
 * 评论响应
 */
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private UserBrief author;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public UserBrief getAuthor() { return author; }
    public void setAuthor(UserBrief author) { this.author = author; }
}
