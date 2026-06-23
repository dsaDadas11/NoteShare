package com.example.noteshare.repository;

import com.example.noteshare.entity.CommentLikeRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentLikeRelRepository extends JpaRepository<CommentLikeRel, Long> {

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    List<Long> findCommentIdByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    @Transactional
    void deleteByCommentId(Long commentId);

    @Transactional
    void deleteByCommentIdIn(List<Long> commentIds);
}
