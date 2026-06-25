package com.example.noteshare.repository;

import com.example.noteshare.entity.LikeRel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRelRepository extends JpaRepository<LikeRel, Long> {

    boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    long countByNoteId(Long noteId);

    long deleteByUserIdAndNoteId(Long userId, Long noteId);

    long deleteByNoteId(Long noteId);
}
