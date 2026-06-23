package com.example.noteshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过 2000 字符")
    private String content;

    /** 父评论 ID，为 null 表示顶级评论 */
    private Long parentId;

    /** 回复目标作者昵称（楼中楼展示用，如 "回复 @张三"） */
    @Size(max = 50, message = "回复作者名称不能超过 50 字符")
    private String replyToAuthor;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getReplyToAuthor() { return replyToAuthor; }
    public void setReplyToAuthor(String replyToAuthor) { this.replyToAuthor = replyToAuthor; }
}
