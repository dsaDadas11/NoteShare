package com.example.noteshare.repository;

import com.example.noteshare.entity.LikeRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface LikeRelRepository extends JpaRepository<LikeRel, Long> {

    boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    @Transactional
    void deleteByUserIdAndNoteId(Long userId, Long noteId);

    @Transactional
    void deleteByNoteId(Long noteId);
}
