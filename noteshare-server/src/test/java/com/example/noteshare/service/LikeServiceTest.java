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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
    void like_AlreadyExists_throwsWithoutCreatingDuplicate() {
        when(noteRepository.existsById(100L)).thenReturn(true);
        when(likeRelRepository.existsByUserIdAndNoteId(1L, 100L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> likeService.like(1L, 100L));

        assertEquals(ErrorCode.LIKE_ALREADY, exception.getErrorCode());
        verify(likeRelRepository, never()).saveAndFlush(any(LikeRel.class));
        verify(noteRepository, never()).setLikeCount(anyLong(), anyInt());
        verify(notificationService, never()).createLikeNotification(any(), any());
    }

    @Test
    void like_NewLike_syncsLikeCount() {
        when(noteRepository.existsById(100L)).thenReturn(true);
        when(likeRelRepository.existsByUserIdAndNoteId(1L, 100L)).thenReturn(false);
        when(likeRelRepository.countByNoteId(100L)).thenReturn(1L);

        likeService.like(1L, 100L);

        verify(likeRelRepository).saveAndFlush(any(LikeRel.class));
        verify(noteRepository).setLikeCount(100L, 1);
        verify(notificationService).createLikeNotification(1L, 100L);
    }

    @Test
    void unlike_NoDeletedRow_doesNotSyncCount() {
        when(likeRelRepository.deleteByUserIdAndNoteId(1L, 100L)).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> likeService.unlike(1L, 100L));

        assertEquals(ErrorCode.LIKE_NOT_FOUND, exception.getErrorCode());
        verify(noteRepository, never()).setLikeCount(anyLong(), anyInt());
    }

    @Test
    void unlike_DeletedRow_syncsLikeCount() {
        when(likeRelRepository.deleteByUserIdAndNoteId(1L, 100L)).thenReturn(1L);
        when(likeRelRepository.countByNoteId(100L)).thenReturn(0L);

        likeService.unlike(1L, 100L);

        verify(noteRepository).setLikeCount(100L, 0);
    }
}
