package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.response.CommentResponse;
import com.example.noteshare.entity.Comment;
import com.example.noteshare.repository.CommentRepository;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.example.noteshare.repository.CommentLikeRelRepository commentLikeRelRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteComment_OwnerSuccess() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setNoteId(100L);
        comment.setUserId(1L);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, 100L, 10L);

        verify(commentRepository).delete(comment);
        verify(commentRepository).flush();
        verify(noteRepository).decrementCommentCount(100L);
    }

    @Test
    void deleteComment_NotOwnerForbidden() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setNoteId(100L);
        comment.setUserId(2L);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.deleteComment(1L, 100L, 10L));

        assertEquals(ErrorCode.COMMENT_FORBIDDEN, exception.getErrorCode());
        verify(commentRepository, never()).delete(any());
        verify(noteRepository, never()).decrementCommentCount(any());
    }

    @Test
    void listComments_MarksCurrentUsersComment() {
        Comment ownComment = new Comment();
        ownComment.setId(10L);
        ownComment.setNoteId(100L);
        ownComment.setUserId(1L);
        ownComment.setContent("自己的评论");

        Comment otherComment = new Comment();
        otherComment.setId(11L);
        otherComment.setNoteId(100L);
        otherComment.setUserId(2L);
        otherComment.setContent("别人的评论");

        when(commentRepository.findByNoteIdAndParentIdIsNullOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownComment, otherComment)));

        PageResponse<CommentResponse> response = commentService.listComments(100L, 1L, 1, 20);

        assertTrue(response.getItems().get(0).isMine());
        assertFalse(response.getItems().get(1).isMine());
    }

    @Test
    void likeComment_ConcurrentDuplicate_doesNotIncrementCount() {
        when(commentRepository.existsById(10L)).thenReturn(true);
        when(commentLikeRelRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate comment like"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.likeComment(1L, 10L));

        assertEquals(ErrorCode.COMMENT_LIKE_ALREADY, exception.getErrorCode());
        verify(commentRepository, never()).incrementLikeCount(10L);
    }

    @Test
    void unlikeComment_NoDeletedRow_doesNotDecrementCount() {
        when(commentLikeRelRepository.deleteByUserIdAndCommentId(1L, 10L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.unlikeComment(1L, 10L));

        assertEquals(ErrorCode.COMMENT_LIKE_NOT_FOUND, exception.getErrorCode());
        verify(commentRepository, never()).decrementLikeCount(10L);
    }
}
