package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.LikeRel;
import com.example.noteshare.repository.LikeRelRepository;
import com.example.noteshare.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRelRepository likeRelRepository;
    private final NoteRepository noteRepository;
    private final NotificationService notificationService;

    public LikeService(LikeRelRepository likeRelRepository,
                       NoteRepository noteRepository,
                       NotificationService notificationService) {
        this.likeRelRepository = likeRelRepository;
        this.noteRepository = noteRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void like(Long userId, Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        if (likeRelRepository.existsByUserIdAndNoteId(userId, noteId)) {
            throw new BusinessException(ErrorCode.LIKE_ALREADY);
        }

        LikeRel like = new LikeRel();
        like.setUserId(userId);
        like.setNoteId(noteId);
        likeRelRepository.saveAndFlush(like);
        syncLikeCount(noteId);

        notificationService.createLikeNotification(userId, noteId);
    }

    @Transactional
    public void unlike(Long userId, Long noteId) {
        long deleted = likeRelRepository.deleteByUserIdAndNoteId(userId, noteId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        syncLikeCount(noteId);
    }

    private void syncLikeCount(Long noteId) {
        int count = (int) likeRelRepository.countByNoteId(noteId);
        noteRepository.setLikeCount(noteId, count);
    }
}
