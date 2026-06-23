package com.example.noteshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateNoteRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 字符")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 5000, message = "正文不能超过 5000 字符")
    private String content;

    @Size(max = 3, message = "最多上传 3 张图片")
    private List<String> imageUrls;

    private String videoUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}
