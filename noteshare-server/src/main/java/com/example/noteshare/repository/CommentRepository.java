package com.example.noteshare.repository;

import com.example.noteshare.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByNoteIdOrderByCreatedAtDesc(Long noteId, Pageable pageable);
}
