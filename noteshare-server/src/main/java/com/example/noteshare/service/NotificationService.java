package com.example.noteshare.service;

import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.response.NotificationResponse;
import com.example.noteshare.dto.response.UnreadCountResponse;
import com.example.noteshare.entity.Notification;
import com.example.noteshare.entity.Note;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.repository.NotificationRepository;
import com.example.noteshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NoteRepository noteRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
    }

    /**
     * 创建点赞通知（防重复：同一用户对同一笔记只保留一条）
     */
    @Transactional
    public Notification createLikeNotification(Long senderId, Long noteId) {
        Note note = noteRepository.findById(noteId).orElse(null);
        if (note == null) return null;
        Long receiverId = note.getAuthorId();
        if (senderId.equals(receiverId)) return null; // 不给自己发

        // 防重复
        Notification existing = notificationRepository
                .findByTypeAndSenderIdAndNoteId("LIKE", senderId, noteId)
                .orElse(null);
        if (existing != null) {
            existing.setIsRead(false);
            return notificationRepository.save(existing);
        }

        Notification notification = new Notification();
        notification.setType("LIKE");
        notification.setSenderId(senderId);
        notification.setReceiverId(receiverId);
        notification.setNoteId(noteId);
        return notificationRepository.save(notification);
    }

    /**
     * 创建评论通知
     */
    @Transactional
    public Notification createCommentNotification(Long senderId, Long noteId, String commentContent) {
        Note note = noteRepository.findById(noteId).orElse(null);
        if (note == null) return null;
        Long receiverId = note.getAuthorId();
        if (senderId.equals(receiverId)) return null; // 不给自己发

        Notification notification = new Notification();
        notification.setType("COMMENT");
        notification.setSenderId(senderId);
        notification.setReceiverId(receiverId);
        notification.setNoteId(noteId);
        notification.setCommentContent(commentContent);
        return notificationRepository.save(notification);
    }

    /**
     * 获取通知列表（分页）
     */
    public PageResponse<NotificationResponse> listNotifications(Long userId, int page, int size) {
        Page<Notification> notificationPage = notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));

        List<Notification> notifications = notificationPage.getContent();
        if (notifications.isEmpty()) {
            PageResponse<NotificationResponse> resp = new PageResponse<>();
            resp.setItems(List.of());
            resp.setPage(page);
            resp.setPageSize(size);
            resp.setTotal(0);
            resp.setHasMore(false);
            return resp;
        }

        // 批量加载发送者信息
        List<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId).distinct().toList();
        Map<Long, User> senderMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 批量加载笔记标题
        List<Long> noteIds = notifications.stream()
                .map(Notification::getNoteId).distinct().toList();
        Map<Long, Note> noteMap = noteRepository.findAllById(noteIds).stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));

        List<NotificationResponse> items = notifications.stream().map(n -> {
            NotificationResponse resp = new NotificationResponse();
            resp.setId(n.getId());
            resp.setType(n.getType());
            resp.setSenderId(n.getSenderId());
            resp.setNoteId(n.getNoteId());
            resp.setCommentContent(n.getCommentContent());
            resp.setIsRead(n.getIsRead());
            resp.setCreatedAt(n.getCreatedAt());

            User sender = senderMap.get(n.getSenderId());
            if (sender != null) {
                resp.setSenderNickname(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                resp.setSenderAvatar(sender.getAvatarUrl());
            }

            Note note = noteMap.get(n.getNoteId());
            if (note != null) {
                resp.setNoteTitle(note.getTitle());
            }

            return resp;
        }).toList();

        PageResponse<NotificationResponse> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setPage(page);
        resp.setPageSize(size);
        resp.setTotal(notificationPage.getTotalElements());
        resp.setHasMore(notificationPage.hasNext());
        return resp;
    }

    /**
     * 获取未读数量
     */
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByReceiverIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
