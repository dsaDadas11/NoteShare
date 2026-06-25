package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.LikeRel;
import com.example.noteshare.repository.LikeRelRepository;
import com.example.noteshare.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeServiceTest {

    @Mock
    private LikeRelRepository likeRelRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void like_ConcurrentDuplicate_doesNotIncrementCount() {
        when(noteRepository.existsById(100L)).thenReturn(true);
        when(likeRelRepository.saveAndFlush(any(LikeRel.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate like"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> likeService.like(1L, 100L));

        assertEquals(ErrorCode.LIKE_ALREADY, exception.getErrorCode());
        verify(noteRepository, never()).incrementLikeCount(100L);
        verify(notificationService, never()).createLikeNotification(any(), any());
    }

    @Test
    void unlike_NoDeletedRow_doesNotDecrementCount() {
        when(likeRelRepository.deleteByUserIdAndNoteId(1L, 100L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> likeService.unlike(1L, 100L));

        assertEquals(ErrorCode.LIKE_NOT_FOUND, exception.getErrorCode());
        verify(noteRepository, never()).decrementLikeCount(100L);
    }

    @Test
    void unlike_DeletedRow_decrementsCount() {
        when(likeRelRepository.deleteByUserIdAndNoteId(1L, 100L)).thenReturn(1);

        likeService.unlike(1L, 100L);

        verify(noteRepository).decrementLikeCount(100L);
    }
}
