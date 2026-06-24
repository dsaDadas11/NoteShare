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
        // 校验笔记存在
        if (!noteRepository.existsById(noteId)) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        // 检查是否已赞
        if (likeRelRepository.existsByUserIdAndNoteId(userId, noteId)) {
            throw new BusinessException(ErrorCode.LIKE_ALREADY);
        }
        // 保存点赞关系
        LikeRel like = new LikeRel();
        like.setUserId(userId);
        like.setNoteId(noteId);
        likeRelRepository.save(like);
        // 更新计数
        noteRepository.incrementLikeCount(noteId);

        // 触发通知（仅保存到数据库）
        notificationService.createLikeNotification(userId, noteId);
    }

    @Transactional
    public void unlike(Long userId, Long noteId) {
        if (!likeRelRepository.existsByUserIdAndNoteId(userId, noteId)) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        likeRelRepository.deleteByUserIdAndNoteId(userId, noteId);
        noteRepository.decrementLikeCount(noteId);
    }
}
