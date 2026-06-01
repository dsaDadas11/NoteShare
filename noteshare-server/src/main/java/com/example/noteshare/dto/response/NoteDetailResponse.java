package com.example.noteshare.dto.response;

/**
 * 笔记详情响应（含点赞和作者关注状态）
 */
public class NoteDetailResponse extends NoteResponse {

    private Boolean liked;
    private Boolean authorFollowed;
    private Boolean authorSelf;

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }

    public Boolean getAuthorFollowed() { return authorFollowed; }
    public void setAuthorFollowed(Boolean authorFollowed) { this.authorFollowed = authorFollowed; }

    public Boolean getAuthorSelf() { return authorSelf; }
    public void setAuthorSelf(Boolean authorSelf) { this.authorSelf = authorSelf; }
}
