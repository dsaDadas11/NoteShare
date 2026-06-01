package com.example.noteshare.repository;

import com.example.noteshare.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<Note> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Note n SET n.likeCount = n.likeCount + 1 WHERE n.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Note n SET n.likeCount = n.likeCount - 1 WHERE n.id = :id AND n.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Note n SET n.commentCount = n.commentCount + 1 WHERE n.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    Page<Note> findByAuthorId(Long authorId, Pageable pageable);

    long countByAuthorId(Long authorId);
}
