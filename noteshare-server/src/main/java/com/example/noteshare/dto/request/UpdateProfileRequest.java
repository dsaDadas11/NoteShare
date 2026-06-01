package com.example.noteshare.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称不能超过 50 字符")
    private String nickname;

    @Size(max = 500, message = "头像 URL 不能超过 500 字符")
    private String avatarUrl;

    @Size(max = 500, message = "简介不能超过 500 字符")
    private String bio;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
