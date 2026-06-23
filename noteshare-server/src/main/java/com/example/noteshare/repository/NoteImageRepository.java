package com.example.noteshare.repository;

import com.example.noteshare.entity.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

    List<NoteImage> findByNoteIdOrderBySortAsc(Long noteId);

    List<NoteImage> findByNoteIdInOrderByNoteIdAscSortAsc(List<Long> noteIds);

    void deleteByNoteId(Long noteId);
}
