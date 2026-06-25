package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.LikeRel;
import com.example.noteshare.repository.LikeRelRepository;
import com.example.noteshare.repository.NoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

        LikeRel like = new LikeRel();
        like.setUserId(userId);
        like.setNoteId(noteId);
        try {
            likeRelRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.LIKE_ALREADY);
        }

        noteRepository.incrementLikeCount(noteId);

        // 触发通知（仅保存到数据库）
        notificationService.createLikeNotification(userId, noteId);
    }

    @Transactional
    public void unlike(Long userId, Long noteId) {
        int deleted = likeRelRepository.deleteByUserIdAndNoteId(userId, noteId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        noteRepository.decrementLikeCount(noteId);
    }
}
