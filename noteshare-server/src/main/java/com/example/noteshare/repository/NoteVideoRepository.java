package com.example.noteshare.repository;

import com.example.noteshare.entity.NoteVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteVideoRepository extends JpaRepository<NoteVideo, Long> {

    Optional<NoteVideo> findByNoteId(Long noteId);

    List<NoteVideo> findByNoteIdIn(List<Long> noteIds);

    void deleteByNoteId(Long noteId);
}
