package com.example.noteshare.service;

import com.example.noteshare.dto.request.CreateCommentRequest;
import com.example.noteshare.entity.Note;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.CommentRepository;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommentDeleteIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long noteId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("delete_test_" + System.nanoTime());
        user.setPasswordHash("hash");
        user.setNickname("Tester");
        userId = userRepository.save(user).getId();

        Note note = new Note();
        note.setAuthorId(userId);
        note.setTitle("Test Note");
        note.setContent("Content");
        noteId = noteRepository.save(note).getId();
    }

    @Test
    @Transactional
    void deleteComment_removesRowAndDecrementsCount() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("to be deleted");
        var created = commentService.createComment(userId, noteId, req);
        Long commentId = created.getId();

        assertTrue(commentRepository.existsById(commentId));
        assertEquals(1, noteRepository.findById(noteId).orElseThrow().getCommentCount());

        commentService.deleteComment(userId, noteId, commentId);

        assertFalse(commentRepository.existsById(commentId));
        assertEquals(0, noteRepository.findById(noteId).orElseThrow().getCommentCount());
    }

    @Test
    @Transactional
    void deleteComment_withReplies_removesAllDescendants() {
        CreateCommentRequest parentReq = new CreateCommentRequest();
        parentReq.setContent("parent");
        Long parentId = commentService.createComment(userId, noteId, parentReq).getId();

        CreateCommentRequest replyReq = new CreateCommentRequest();
        replyReq.setContent("reply");
        replyReq.setParentId(parentId);
        Long replyId = commentService.createComment(userId, noteId, replyReq).getId();

        assertEquals(2, noteRepository.findById(noteId).orElseThrow().getCommentCount());

        commentService.deleteComment(userId, noteId, parentId);

        assertFalse(commentRepository.existsById(parentId));
        assertFalse(commentRepository.existsById(replyId));
        assertEquals(0, noteRepository.findById(noteId).orElseThrow().getCommentCount());
    }
}
