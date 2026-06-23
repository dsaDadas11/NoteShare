package com.example.noteshare.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应（支持楼中楼回复 + 评论点赞）
 */
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private UserBrief author;
    private boolean mine;
    private Long parentId;
    private int likeCount;
    private boolean liked;
    private int replyCount;
    private List<CommentResponse> replies;
    /** 回复目标作者昵称（楼中楼展示用） */
    private String replyToAuthor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public UserBrief getAuthor() { return author; }
    public void setAuthor(UserBrief author) { this.author = author; }
    public boolean isMine() { return mine; }
    public void setMine(boolean mine) { this.mine = mine; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
    public List<CommentResponse> getReplies() { return replies; }
    public void setReplies(List<CommentResponse> replies) { this.replies = replies; }
    public String getReplyToAuthor() { return replyToAuthor; }
    public void setReplyToAuthor(String replyToAuthor) { this.replyToAuthor = replyToAuthor; }
}
