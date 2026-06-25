package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.Follow;
import com.example.noteshare.repository.FollowRepository;
import com.example.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void follow_ConcurrentDuplicate_throwsFollowAlready() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(followRepository.saveAndFlush(any(Follow.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate follow"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> followService.follow(1L, 2L));

        assertEquals(ErrorCode.FOLLOW_ALREADY, exception.getErrorCode());
    }

    @Test
    void unfollow_NoDeletedRow_throwsFollowNotFound() {
        when(followRepository.deleteByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> followService.unfollow(1L, 2L));

        assertEquals(ErrorCode.FOLLOW_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void unfollow_DeletedRow_succeeds() {
        when(followRepository.deleteByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(1);

        followService.unfollow(1L, 2L);

        verify(followRepository).deleteByFollowerIdAndFolloweeId(1L, 2L);
    }
}
