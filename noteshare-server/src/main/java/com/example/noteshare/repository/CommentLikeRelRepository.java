package com.example.noteshare.repository;

import com.example.noteshare.entity.CommentLikeRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentLikeRelRepository extends JpaRepository<CommentLikeRel, Long> {

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT c.commentId FROM CommentLikeRel c WHERE c.userId = :userId AND c.commentId IN :commentIds")
    List<Long> findCommentIdByUserIdAndCommentIdIn(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentLikeRel c WHERE c.userId = :userId AND c.commentId = :commentId")
    @Transactional
    int deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Transactional
    void deleteByCommentId(Long commentId);

    @Transactional
    void deleteByCommentIdIn(List<Long> commentIds);
}
