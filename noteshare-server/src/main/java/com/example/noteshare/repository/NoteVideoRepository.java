package com.example.noteshare.repository;

import com.example.noteshare.entity.NoteVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NoteVideoRepository extends JpaRepository<NoteVideo, Long> {

    Optional<NoteVideo> findByNoteId(Long noteId);

    List<NoteVideo> findByNoteIdIn(List<Long> noteIds);

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteByNoteId(Long noteId);
}
