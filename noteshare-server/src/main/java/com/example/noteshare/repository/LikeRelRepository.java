package com.example.noteshare.repository;

import com.example.noteshare.entity.LikeRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LikeRelRepository extends JpaRepository<LikeRel, Long> {

    boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM LikeRel l WHERE l.userId = :userId AND l.noteId = :noteId")
    @Transactional
    int deleteByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Transactional
    void deleteByNoteId(Long noteId);
}
