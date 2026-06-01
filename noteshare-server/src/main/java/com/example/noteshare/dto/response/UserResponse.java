package com.example.noteshare.dto.response;

import com.example.noteshare.entity.User;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
public class UserResponse {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;

    // 查看他人资料时的额外字段
    private Long followerCount;
    private Long followingCount;
    private Boolean followed;
    private Integer noteCount;

    public static UserResponse from(User user) {
        UserResponse resp = new UserResponse();
        resp.id = user.getId();
        resp.username = user.getUsername();
        resp.nickname = user.getNickname();
        resp.avatarUrl = user.getAvatarUrl();
        resp.bio = user.getBio();
        resp.createdAt = user.getCreatedAt();
        return resp;
    }

    // getters & setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }

    public Long getFollowingCount() { return followingCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }

    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }

    public Integer getNoteCount() { return noteCount; }
    public void setNoteCount(Integer noteCount) { this.noteCount = noteCount; }
}
