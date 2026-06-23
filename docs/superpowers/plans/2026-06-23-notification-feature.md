# 通知功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 NoteShare 添加类似小红书的通知功能，当其他用户点赞或评论笔记时，实时推送通知并显示小红点提示。

**Architecture:** 后端新增 Notification 实体和 WebSocket 推送服务，点赞/评论时触发通知。Android 端通过 OkHttp WebSocket 接收实时推送，MainViewModel 管理未读数状态，底部导航"我的"Tab 显示红点。

**Tech Stack:** Spring WebSocket, OkHttp WebSocket, Jetpack Compose Badge, Hilt, Retrofit

---

## 文件清单

### 后端新增文件
| 文件 | 职责 |
|------|------|
| `noteshare-server/.../entity/Notification.java` | 通知实体 |
| `noteshare-server/.../repository/NotificationRepository.java` | 通知数据访问 |
| `noteshare-server/.../dto/response/NotificationResponse.java` | 通知响应 DTO |
| `noteshare-server/.../dto/response/UnreadCountResponse.java` | 未读数响应 DTO |
| `noteshare-server/.../service/NotificationService.java` | 通知业务逻辑 |
| `noteshare-server/.../controller/NotificationController.java` | 通知 REST API |
| `noteshare-server/.../config/WebSocketConfig.java` | WebSocket 配置 |
| `noteshare-server/.../websocket/NotificationWebSocketHandler.java` | WebSocket 处理器 |

### 后端修改文件
| 文件 | 修改内容 |
|------|----------|
| `noteshare-server/pom.xml` | 添加 websocket 依赖 |
| `noteshare-server/.../service/LikeService.java` | 点赞后触发通知 |
| `noteshare-server/.../service/CommentService.java` | 评论后触发通知 |
| `noteshare-server/.../config/SecurityConfig.java` | WebSocket 端点放行 |

### Android 新增文件
| 文件 | 职责 |
|------|------|
| `app/.../feature/notification/data/NotificationApi.kt` | Retrofit 接口 |
| `app/.../feature/notification/data/NotificationRepository.kt` | 数据仓库 |
| `app/.../feature/notification/domain/model/NotificationModels.kt` | 数据模型 |
| `app/.../feature/notification/presentation/NotificationScreen.kt` | 通知页面 |
| `app/.../feature/notification/presentation/NotificationViewModel.kt` | 状态管理 |
| `app/.../core/network/NotificationWebSocketClient.kt` | WebSocket 客户端 |

### Android 修改文件
| 文件 | 修改内容 |
|------|----------|
| `app/.../core/di/NetworkModule.kt` | 注入 NotificationApi |
| `app/.../MainViewModel.kt` | 管理未读数 + WebSocket 生命周期 |
| `app/.../shared/ui/BottomNavBar.kt` | "我的"Tab 显示红点 |
| `app/.../feature/profile/presentation/ProfileScreen.kt` | 增加通知入口 |
| `app/.../MainActivity.kt` | 注册通知路由 |

---

## Task 1: 后端 — pom.xml 添加 WebSocket 依赖

**Files:**
- Modify: `noteshare-server/pom.xml:24-88`

- [ ] **Step 1: 添加 spring-boot-starter-websocket 依赖**

在 `<dependencies>` 中 `spring-boot-starter-validation` 之后添加：

```xml
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖下载**

```bash
cd noteshare-server && ./mvnw dependency:resolve -DincludeArtifactIds=spring-boot-starter-websocket
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add noteshare-server/pom.xml
git commit -m "deps: add spring-boot-starter-websocket dependency"
```

---

## Task 2: 后端 — Notification Entity + Repository

**Files:**
- Create: `noteshare-server/src/main/java/com/example/noteshare/entity/Notification.java`
- Create: `noteshare-server/src/main/java/com/example/noteshare/repository/NotificationRepository.java`

- [ ] **Step 1: 创建 Notification 实体**

```java
package com.example.noteshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String type;  // LIKE / COMMENT

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "comment_content", length = 500)
    private String commentContent;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public String getCommentContent() { return commentContent; }
    public void setCommentContent(String commentContent) { this.commentContent = commentContent; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: 创建 NotificationRepository**

```java
package com.example.noteshare.repository;

import com.example.noteshare.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false")
    void markAllAsRead(@Param("receiverId") Long receiverId);

    Optional<Notification> findByTypeAndSenderIdAndNoteId(String type, Long senderId, Long noteId);
}
```

- [ ] **Step 3: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/entity/Notification.java \
        noteshare-server/src/main/java/com/example/noteshare/repository/NotificationRepository.java
git commit -m "feat(notification): add Notification entity and repository"
```

---

## Task 3: 后端 — DTO 响应类

**Files:**
- Create: `noteshare-server/src/main/java/com/example/noteshare/dto/response/NotificationResponse.java`
- Create: `noteshare-server/src/main/java/com/example/noteshare/dto/response/UnreadCountResponse.java`

- [ ] **Step 1: 创建 NotificationResponse**

```java
package com.example.noteshare.dto.response;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private String type;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long noteId;
    private String noteTitle;
    private String commentContent;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public String getNoteTitle() { return noteTitle; }
    public void setNoteTitle(String noteTitle) { this.noteTitle = noteTitle; }
    public String getCommentContent() { return commentContent; }
    public void setCommentContent(String commentContent) { this.commentContent = commentContent; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: 创建 UnreadCountResponse**

```java
package com.example.noteshare.dto.response;

public class UnreadCountResponse {

    private long count;

    public UnreadCountResponse() {}

    public UnreadCountResponse(long count) {
        this.count = count;
    }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
```

- [ ] **Step 3: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/dto/response/NotificationResponse.java \
        noteshare-server/src/main/java/com/example/noteshare/dto/response/UnreadCountResponse.java
git commit -m "feat(notification): add notification response DTOs"
```

---

## Task 4: 后端 — NotificationService

**Files:**
- Create: `noteshare-server/src/main/java/com/example/noteshare/service/NotificationService.java`

- [ ] **Step 1: 创建 NotificationService**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/service/NotificationService.java
git commit -m "feat(notification): add NotificationService with like/comment triggers"
```

---

## Task 5: 后端 — WebSocket 配置 + Handler

**Files:**
- Create: `noteshare-server/src/main/java/com/example/noteshare/config/WebSocketConfig.java`
- Create: `noteshare-server/src/main/java/com/example/noteshare/websocket/NotificationWebSocketHandler.java`

- [ ] **Step 1: 创建 WebSocketConfig**

```java
package com.example.noteshare.config;

import com.example.noteshare.websocket.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notification")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: 创建 NotificationWebSocketHandler**

```java
package com.example.noteshare.websocket;

import com.example.noteshare.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    // userId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Map<String, String> params = UriComponentsBuilder
                .fromUriString("?" + query).build()
                .getQueryParams().toSingleValueMap();

        String token = params.get("token");
        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        sessions.put(userId, session);
        log.info("WebSocket connected: userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("WebSocket disconnected: status={}", status);
    }

    /**
     * 向指定用户推送通知
     */
    public void sendNotification(Long userId, Object notificationData) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "NOTIFICATION",
                        "data", notificationData
                );
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                log.info("Pushed notification to userId={}", userId);
            } catch (IOException e) {
                log.error("Failed to send notification to userId={}", userId, e);
            }
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/config/WebSocketConfig.java \
        noteshare-server/src/main/java/com/example/noteshare/websocket/NotificationWebSocketHandler.java
git commit -m "feat(notification): add WebSocket config and handler"
```

---

## Task 6: 后端 — NotificationController

**Files:**
- Create: `noteshare-server/src/main/java/com/example/noteshare/controller/NotificationController.java`

- [ ] **Step 1: 创建 NotificationController**

```java
package com.example.noteshare.controller;

import com.example.noteshare.common.ApiResponse;
import com.example.noteshare.common.PageResponse;
import com.example.noteshare.dto.response.NotificationResponse;
import com.example.noteshare.dto.response.UnreadCountResponse;
import com.example.noteshare.security.SecurityUtil;
import com.example.noteshare.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 获取通知列表（分页） */
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.currentUserId();
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        return ApiResponse.ok(notificationService.listNotifications(userId, page, size));
    }

    /** 获取未读通知数量 */
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = SecurityUtil.currentUserId();
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = SecurityUtil.currentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/controller/NotificationController.java
git commit -m "feat(notification): add NotificationController with REST APIs"
```

---

## Task 7: 后端 — 修改 LikeService 触发通知

**Files:**
- Modify: `noteshare-server/src/main/java/com/example/noteshare/service/LikeService.java`

- [ ] **Step 1: 注入 NotificationService 和 NotificationWebSocketHandler**

修改构造函数，添加两个新依赖：

```java
package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.LikeRel;
import com.example.noteshare.entity.Notification;
import com.example.noteshare.repository.LikeRelRepository;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.websocket.NotificationWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRelRepository likeRelRepository;
    private final NoteRepository noteRepository;
    private final NotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;

    public LikeService(LikeRelRepository likeRelRepository,
                       NoteRepository noteRepository,
                       NotificationService notificationService,
                       NotificationWebSocketHandler webSocketHandler) {
        this.likeRelRepository = likeRelRepository;
        this.noteRepository = noteRepository;
        this.notificationService = notificationService;
        this.webSocketHandler = webSocketHandler;
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
        likeRelRepository.save(like);
        noteRepository.incrementLikeCount(noteId);

        // 触发通知
        Notification notification = notificationService.createLikeNotification(userId, noteId);
        if (notification != null) {
            webSocketHandler.sendNotification(notification.getReceiverId(), buildNotificationData(notification));
        }
    }

    @Transactional
    public void unlike(Long userId, Long noteId) {
        if (!likeRelRepository.existsByUserIdAndNoteId(userId, noteId)) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        likeRelRepository.deleteByUserIdAndNoteId(userId, noteId);
        noteRepository.decrementLikeCount(noteId);
    }

    private Object buildNotificationData(Notification notification) {
        return new Object() {
            public final Long getNotificationId() { return notification.getId(); }
            public final String getType() { return notification.getType(); }
            public final Long getSenderId() { return notification.getSenderId(); }
            public final Long getNoteId() { return notification.getNoteId(); }
            public final String getCreatedAt() { return notification.getCreatedAt().toString(); }
        };
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/service/LikeService.java
git commit -m "feat(notification): trigger notification on like"
```

---

## Task 8: 后端 — 修改 CommentService 触发通知

**Files:**
- Modify: `noteshare-server/src/main/java/com/example/noteshare/service/CommentService.java`

- [ ] **Step 1: 注入 NotificationService 和 NotificationWebSocketHandler**

修改构造函数，添加依赖，并在 `createComment` 方法末尾触发通知：

```java
// 在构造函数中添加：
private final NotificationService notificationService;
private final NotificationWebSocketHandler webSocketHandler;

public CommentService(CommentRepository commentRepository,
                      NoteRepository noteRepository,
                      UserRepository userRepository,
                      CommentLikeRelRepository commentLikeRelRepository,
                      NotificationService notificationService,
                      NotificationWebSocketHandler webSocketHandler) {
    this.commentRepository = commentRepository;
    this.noteRepository = noteRepository;
    this.userRepository = userRepository;
    this.commentLikeRelRepository = commentLikeRelRepository;
    this.notificationService = notificationService;
    this.webSocketHandler = webSocketHandler;
}
```

- [ ] **Step 2: 在 createComment 方法末尾添加通知触发**

在 `return buildCommentResponse(comment, userId);` 之前添加：

```java
        // 触发评论通知
        Notification notification = notificationService.createCommentNotification(
                userId, noteId, req.getContent());
        if (notification != null) {
            webSocketHandler.sendNotification(notification.getReceiverId(), buildNotificationPush(notification));
        }
```

添加辅助方法：

```java
    private Object buildNotificationPush(Notification notification) {
        return new Object() {
            public final Long getNotificationId() { return notification.getId(); }
            public final String getType() { return notification.getType(); }
            public final Long getSenderId() { return notification.getSenderId(); }
            public final Long getNoteId() { return notification.getNoteId(); }
            public final String getCommentContent() { return notification.getCommentContent(); }
            public final String getCreatedAt() { return notification.getCreatedAt().toString(); }
        };
    }
```

- [ ] **Step 3: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/service/CommentService.java
git commit -m "feat(notification): trigger notification on comment"
```

---

## Task 9: 后端 — SecurityConfig 放行 WebSocket

**Files:**
- Modify: `noteshare-server/src/main/java/com/example/noteshare/config/SecurityConfig.java:36-48`

- [ ] **Step 1: 添加 WebSocket 端点放行**

在 `authorizeHttpRequests` 中添加 WebSocket 端点放行：

```java
.authorizeHttpRequests(auth -> auth
    // WebSocket 端点放行（认证在 Handler 中通过 token 参数完成）
    .requestMatchers("/ws/**").permitAll()
    // 公开接口放行
    .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/notes/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
    // ... 其余不变
)
```

- [ ] **Step 2: Commit**

```bash
git add noteshare-server/src/main/java/com/example/noteshare/config/SecurityConfig.java
git commit -m "feat(notification): allow WebSocket endpoints in security config"
```

---

## Task 10: Android — 数据模型 + API 接口

**Files:**
- Create: `app/src/main/java/com/example/noteshare/feature/notification/domain/model/NotificationModels.kt`
- Create: `app/src/main/java/com/example/noteshare/feature/notification/data/NotificationApi.kt`

- [ ] **Step 1: 创建 NotificationModels.kt**

```kotlin
package com.example.noteshare.feature.notification.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: Long,
    val type: String,
    val senderId: Long,
    val senderNickname: String? = null,
    val senderAvatar: String? = null,
    val noteId: Long,
    val noteTitle: String? = null,
    val commentContent: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

@Serializable
data class UnreadCountResponse(val count: Int = 0)

@Serializable
data class NotificationPush(
    val notificationId: Long? = null,
    val type: String,
    val senderId: Long,
    val senderNickname: String? = null,
    val senderAvatar: String? = null,
    val noteId: Long,
    val noteTitle: String? = null,
    val commentContent: String? = null,
    val createdAt: String
)
```

- [ ] **Step 2: 创建 NotificationApi.kt**

```kotlin
package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.feature.notification.domain.model.UnreadCountResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface NotificationApi {

    @GET("/api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NotificationResponse>>

    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<UnreadCountResponse>

    @PUT("/api/notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<Unit>
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/noteshare/feature/notification/domain/model/NotificationModels.kt \
        app/src/main/java/com/example/noteshare/feature/notification/data/NotificationApi.kt
git commit -m "feat(notification): add Android notification models and API interface"
```

---

## Task 11: Android — NotificationRepository + NetworkModule 注入

**Files:**
- Create: `app/src/main/java/com/example/noteshare/feature/notification/data/NotificationRepository.kt`
- Modify: `app/src/main/java/com/example/noteshare/core/di/NetworkModule.kt`

- [ ] **Step 1: 创建 NotificationRepository.kt**

```kotlin
package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.feature.notification.domain.model.UnreadCountResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi
) {
    suspend fun getNotifications(page: Int = 1, size: Int = 20): Result<PageData<NotificationResponse>> {
        return try {
            val response = notificationApi.getNotifications(page, size)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取通知失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(): Result<Long> {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.success(response.data.count)
            } else {
                Result.failure(Exception(response.message ?: "获取未读数失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.code == ErrorCode.SUCCESS) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "标记已读失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: 在 NetworkModule 中注入 NotificationApi**

在 `NetworkModule.kt` 的 `provideUserApi` 方法之后添加：

```kotlin
    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): com.example.noteshare.feature.notification.data.NotificationApi {
        return retrofit.create(com.example.noteshare.feature.notification.data.NotificationApi::class.java)
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/noteshare/feature/notification/data/NotificationRepository.kt \
        app/src/main/java/com/example/noteshare/core/di/NetworkModule.kt
git commit -m "feat(notification): add NotificationRepository and inject NotificationApi"
```

---

## Task 12: Android — WebSocket 客户端

**Files:**
- Create: `app/src/main/java/com/example/noteshare/core/network/NotificationWebSocketClient.kt`

- [ ] **Step 1: 创建 NotificationWebSocketClient.kt**

```kotlin
package com.example.noteshare.core.network

import com.example.noteshare.BuildConfig
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.feature.notification.domain.model.NotificationPush
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableSharedFlow<NotificationPush>(extraBufferCapacity = 10)
    val notifications: SharedFlow<NotificationPush> = _notifications.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    fun connect() {
        scope.launch {
            val token = tokenManager.tokenFlow.firstOrNull()
            if (token.isNullOrEmpty()) return@launch

            val wsUrl = BuildConfig.BASE_URL
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/') + "ws/notification?token=$token"

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    _connectionState.tryEmit(true)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val wrapper = json.decodeFromString<WebSocketMessage>(text)
                        if (wrapper.type == "NOTIFICATION") {
                            val push = json.decodeFromString<NotificationPush>(wrapper.data.toString())
                            _notifications.tryEmit(push)
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _connectionState.tryEmit(false)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.tryEmit(false)
                }
            })
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closing")
        webSocket = null
    }
}

@kotlinx.serialization.Serializable
private data class WebSocketMessage(
    val type: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/noteshare/core/network/NotificationWebSocketClient.kt
git commit -m "feat(notification): add WebSocket client for real-time notifications"
```

---

## Task 13: Android — NotificationViewModel

**Files:**
- Create: `app/src/main/java/com/example/noteshare/feature/notification/presentation/NotificationViewModel.kt`

- [ ] **Step 1: 创建 NotificationViewModel.kt**

```kotlin
package com.example.noteshare.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<NotificationResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        markAllAsRead()
    }

    fun loadNotifications() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            notificationRepository.getNotifications(page = 1)
                .onSuccess { pageData ->
                    _uiState.value = _uiState.value.copy(
                        notifications = pageData.content,
                        isLoading = false,
                        currentPage = 1,
                        hasMore = pageData.content.size >= 20
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        val nextPage = _uiState.value.currentPage + 1
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            notificationRepository.getNotifications(page = nextPage)
                .onSuccess { pageData ->
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications + pageData.content,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMore = pageData.content.size >= 20
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun errorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/noteshare/feature/notification/presentation/NotificationViewModel.kt
git commit -m "feat(notification): add NotificationViewModel"
```

---

## Task 14: Android — NotificationScreen

**Files:**
- Create: `app/src/main/java/com/example/noteshare/feature/notification/presentation/NotificationScreen.kt`

- [ ] **Step 1: 创建 NotificationScreen.kt**

```kotlin
package com.example.noteshare.feature.notification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.noteshare.core.common.DateTimeUtil
import com.example.noteshare.core.network.resolveMediaUrl
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.shared.ui.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    // Load more
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - 3)
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) viewModel.loadMore()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("通知") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无通知", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                onNavigateToDetail(notification.noteId)
                            }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationResponse,
    onClick: () -> Unit
) {
    val isLike = notification.type == "LIKE"
    val accentColor = if (isLike) Color(0xFFFF4757) else Color(0xFF3742FA)
    val bgColor = if (notification.isRead)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧彩色竖条
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // 头像
            AvatarImage(
                model = resolveMediaUrl(notification.senderAvatar)
                    ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${notification.senderId}",
                contentDescription = "头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedText(notification),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateTimeUtil.formatRelative(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun buildAnnotatedText(notification: NotificationResponse): String {
    val sender = notification.senderNickname ?: "用户"
    val noteTitle = notification.noteTitle ?: "笔记"
    return when (notification.type) {
        "LIKE" -> "$sender 赞了你的笔记《$noteTitle》"
        "COMMENT" -> {
            val content = notification.commentContent?.take(30) ?: ""
            "$sender 评论了你的笔记《$noteTitle》：\"$content\""
        }
        else -> "$sender 与你的笔记互动了"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/noteshare/feature/notification/presentation/NotificationScreen.kt
git commit -m "feat(notification): add NotificationScreen with timeline UI"
```

---

## Task 15: Android — MainViewModel 集成未读数 + WebSocket

**Files:**
- Modify: `app/src/main/java/com/example/noteshare/MainViewModel.kt`

- [ ] **Step 1: 注入 WebSocket 客户端和通知仓库，管理未读数**

替换 MainViewModel 全文：

```kotlin
package com.example.noteshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.NotificationWebSocketClient
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.profile.data.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenInterceptor: TokenInterceptor,
    private val userApi: UserApi,
    private val unauthorizedEventBus: UnauthorizedEventBus,
    private val webSocketClient: NotificationWebSocketClient,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Boolean?>(null)
    val loginState: StateFlow<Boolean?> = _loginState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    val unauthorizedEvents = unauthorizedEventBus.events

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.firstOrNull()
            if (token.isNullOrEmpty()) {
                _loginState.value = false
            } else {
                val valid = validateToken()
                _loginState.value = valid
                if (!valid) {
                    tokenManager.clearToken()
                    tokenInterceptor.invalidateCache()
                } else {
                    // 登录成功后连接 WebSocket 并获取未读数
                    connectWebSocket()
                    fetchUnreadCount()
                }
            }
        }
    }

    private suspend fun validateToken(): Boolean {
        return try {
            val response = userApi.getMyProfile()
            response.code == ErrorCode.SUCCESS && response.data != null
        } catch (_: Exception) {
            true
        }
    }

    private fun connectWebSocket() {
        webSocketClient.connect()
        viewModelScope.launch {
            webSocketClient.notifications.collect {
                // 收到推送时未读数 +1
                _unreadCount.value = _unreadCount.value + 1
            }
        }
    }

    private fun fetchUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount()
                .onSuccess { count ->
                    _unreadCount.value = count
                }
        }
    }

    /** 进入通知页后调用，清零未读数 */
    fun clearUnreadCount() {
        _unreadCount.value = 0
    }

    fun logout() {
        viewModelScope.launch {
            webSocketClient.disconnect()
            tokenManager.clearToken()
            tokenInterceptor.invalidateCache()
            _loginState.value = false
            _unreadCount.value = 0
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/noteshare/MainViewModel.kt
git commit -m "feat(notification): integrate WebSocket and unread count in MainViewModel"
```

---

## Task 16: Android — BottomNavBar 显示红点

**Files:**
- Modify: `app/src/main/java/com/example/noteshare/shared/ui/BottomNavBar.kt`

- [ ] **Step 1: 添加 Badge 支持**

替换 BottomNavBar.kt 全文：

```kotlin
package com.example.noteshare.shared.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, unreadCount: Long = 0) {
    val items = listOf(
        BottomNavItem("feed", "首页", Icons.Default.Home),
        BottomNavItem("publish", "发布", Icons.Default.AddCircle),
        BottomNavItem("profile", "我的", Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in listOf("feed", "publish", "profile")

    if (showBottomNav) {
        NavigationBar(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        if (item.route == "profile" && unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(
                                            text = if (unreadCount > 99) "99+" else "$unreadCount",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            ) {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.title)
                        }
                    },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/noteshare/shared/ui/BottomNavBar.kt
git commit -m "feat(notification): add red badge on profile tab in BottomNavBar"
```

---

## Task 17: Android — ProfileScreen 添加通知入口 + MainActivity 路由

**Files:**
- Modify: `app/src/main/java/com/example/noteshare/feature/profile/presentation/ProfileScreen.kt:58-80`
- Modify: `app/src/main/java/com/example/noteshare/MainActivity.kt`

- [ ] **Step 1: ProfileScreen 添加通知图标**

在 ProfileScreen 的 `TopAppBar` 的 `actions` 中，刷新按钮之前添加通知铃铛图标：

```kotlin
actions = {
    if (uiState.isMyProfile) {
        IconButton(onClick = onNavigateToNotification) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "通知"
            )
        }
        IconButton(onClick = { viewModel.refresh() }) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
        }
        TextButton(onClick = onLogout) {
            Text("退出")
        }
    }
}
```

同时修改 ProfileScreen 函数签名，添加 `onNavigateToNotification` 参数：

```kotlin
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNotification: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
```

在文件顶部添加 import：

```kotlin
import androidx.compose.material.icons.filled.Notifications
```

- [ ] **Step 2: MainActivity 注册通知路由并传递 unreadCount**

在 MainActivity 的 `NavHost` 中添加通知路由：

```kotlin
composable("notification") {
    NotificationScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
        onNavigateToProfile = { userId -> navController.navigate("profile/$userId") }
    )
}
```

修改 Scaffold 的 `bottomBar`，传递 `unreadCount`：

```kotlin
Scaffold(
    bottomBar = {
        val unreadCount by mainViewModel.unreadCount.collectAsState()
        BottomNavBar(navController = navController, unreadCount = unreadCount)
    }
) { paddingValues ->
```

修改 `profile` 路由，传递通知入口回调：

```kotlin
composable("profile") {
    ProfileScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToEditProfile = { navController.navigate("edit_profile") },
        onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
        onNavigateToNotification = {
            mainViewModel.clearUnreadCount()
            navController.navigate("notification")
        },
        onLogout = {
            mainViewModel.logout()
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )
}
```

在文件顶部添加 import：

```kotlin
import com.example.noteshare.feature.notification.presentation.NotificationScreen
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/noteshare/feature/profile/presentation/ProfileScreen.kt \
        app/src/main/java/com/example/noteshare/MainActivity.kt
git commit -m "feat(notification): add notification entry in profile and route in MainActivity"
```

---

## Task 18: 后端编译验证

- [ ] **Step 1: 编译后端**

```bash
cd noteshare-server && ./mvnw compile -q
```

Expected: BUILD SUCCESS（无编译错误）

- [ ] **Step 2: 修复编译错误（如有）**

如有 import 缺失或类型不匹配，修复后重新编译。

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "fix(notification): resolve compilation issues"
```

---

## Task 19: Android 编译验证

- [ ] **Step 1: 编译 Android**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 修复编译错误（如有）**

常见问题：
- DateTimeUtil.formatRelative 可能不存在，需检查 `core/common/DateTimeUtil.kt` 是否有此方法
- resolveMediaUrl 的 import 路径
- ApiResponse/PageData 的泛型结构是否匹配

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "fix(notification): resolve Android compilation issues"
```

---

## Task 20: 端到端测试

- [ ] **Step 1: 启动后端**

```bash
cd noteshare-server && ./mvnw spring-boot:run
```

- [ ] **Step 2: 用 curl 测试通知 API**

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8200/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 获取未读数
curl -s http://localhost:8200/api/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN"

# 获取通知列表
curl -s http://localhost:8200/api/notifications \
  -H "Authorization: Bearer $TOKEN"
```

- [ ] **Step 3: 在 Android 模拟器上验证**

1. 运行 App，登录
2. 用另一个账号点赞/评论当前用户的笔记
3. 观察底部导航"我的"Tab 是否出现红点
4. 点击"我的" → 铃铛图标 → 进入通知页
5. 确认通知列表正确显示
6. 返回后红点消失

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete notification feature with WebSocket real-time push"
```
