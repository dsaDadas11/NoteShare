package com.example.noteshare.repository;

import com.example.noteshare.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 查询笔记下的顶级评论（parent_id 为 null），按时间倒序 */
    Page<Comment> findByNoteIdAndParentIdIsNullOrderByCreatedAtDesc(Long noteId, Pageable pageable);

    /** 查询某条评论的回复，按时间正序 */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    /** 批量查询多条评论的回复，按时间正序 */
    List<Comment> findByParentIdInOrderByCreatedAtAsc(List<Long> parentIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    void incrementReplyCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount - 1 WHERE c.id = :id AND c.replyCount > 0")
    void decrementReplyCount(@Param("id") Long id);

    /** 查询某笔记下的所有评论（用于级联删除） */
    List<Comment> findByNoteId(Long noteId);

    /** 删除某笔记下的所有评论 */
    @Modifying(clearAutomatically = true)
    void deleteByNoteId(Long noteId);
}
