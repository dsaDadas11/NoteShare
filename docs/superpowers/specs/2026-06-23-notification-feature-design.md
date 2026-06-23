# 通知功能设计文档

> 日期：2026-06-23 | 状态：已确认

## 1. 需求概述

为 NoteShare 添加类似小红书的通知功能：当其他用户点赞或评论你的笔记时，实时推送通知并显示小红点提示。

### 1.1 设计决策

| 维度 | 决策 | 理由 |
|------|------|------|
| 通知类型 | 仅点赞 + 评论 | 范围精简，符合课设要求 |
| 推送方式 | WebSocket 实时推送 | 零延迟，技术有亮点 |
| 列表样式 | 时间线列表（独立条目） | 类似小红书，直观清晰 |
| 红点位置 | 底部导航 "我的" Tab | 不改导航结构，交互习惯一致 |

## 2. 后端设计

### 2.1 数据库表：notifications

```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,           -- 'LIKE' 或 'COMMENT'
    sender_id BIGINT NOT NULL,           -- 触发者（谁点赞/评论）
    receiver_id BIGINT NOT NULL,         -- 接收者（笔记作者）
    note_id BIGINT NOT NULL,             -- 关联的笔记
    comment_content VARCHAR(500),        -- 评论内容（仅 COMMENT 类型有值）
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_receiver_read (receiver_id, is_read),
    INDEX idx_receiver_created (receiver_id, created_at DESC)
);
```

### 2.2 Entity

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
    // getters/setters...
}
```

### 2.3 Repository

```java
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false")
    void markAllAsRead(@Param("receiverId") Long receiverId);
}
```

### 2.4 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/notifications` | 获取通知列表（分页） | ✅ |
| GET | `/api/notifications/unread-count` | 获取未读数量 | ✅ |
| PUT | `/api/notifications/read-all` | 全部标记已读 | ✅ |
| WS | `/ws/notification` | WebSocket 实时推送 | ✅（JWT） |

#### 响应格式

```json
// GET /api/notifications
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "type": "LIKE",
        "senderId": 2,
        "senderNickname": "小明",
        "senderAvatar": "/uploads/avatar/2.jpg",
        "noteId": 5,
        "noteTitle": "周末游记",
        "commentContent": null,
        "isRead": false,
        "createdAt": "2026-06-23T10:30:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "number": 0
  }
}

// GET /api/notifications/unread-count
{
  "code": 200,
  "data": { "count": 3 }
}
```

#### WebSocket 推送格式

```json
{
  "type": "NOTIFICATION",
  "data": {
    "notificationId": 1,
    "type": "LIKE",
    "senderId": 2,
    "senderNickname": "小明",
    "senderAvatar": "/uploads/avatar/2.jpg",
    "noteId": 5,
    "noteTitle": "周末游记",
    "createdAt": "2026-06-23T10:30:00"
  }
}
```

### 2.5 WebSocket 配置

- 使用 Spring WebSocket（`spring-boot-starter-websocket`）
- 端点：`/ws/notification`
- 连接时通过 URL 参数传递 JWT token 进行认证
- 维护 `userId → WebSocketSession` 映射
- 点赞/评论时，通过映射查找目标用户的 session 并推送

### 2.6 触发时机

修改现有 Service，在以下操作后触发通知：

1. **LikeService.like()** — 点赞成功后：
   - 查找笔记作者 ID
   - 如果点赞者 ≠ 笔记作者，创建 LIKE 通知并通过 WebSocket 推送

2. **CommentService.createComment()** — 评论成功后：
   - 如果评论者 ≠ 笔记作者，创建 COMMENT 通知并通过 WebSocket 推送

### 2.7 防重复

- 同一用户对同一笔记的多次点赞只产生一条通知（取消赞再点赞也只有一条）
- 实现方式：查询是否已存在 `type=LIKE, sender_id=X, note_id=Y` 的记录，存在则更新 `created_at` 和 `is_read`

## 3. Android 端设计

### 3.1 新增模块结构

```
feature/notification/
├── data/
│   ├── NotificationApi.kt        // Retrofit 接口
│   └── NotificationRepository.kt // 数据仓库
├── domain/
│   └── model/
│       └── NotificationModels.kt // 数据模型
└── presentation/
    ├── NotificationScreen.kt     // 通知列表页面
    └── NotificationViewModel.kt  // 状态管理
```

### 3.2 NotificationApi.kt

```kotlin
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

### 3.3 数据模型

```kotlin
@Serializable
data class NotificationResponse(
    val id: Long,
    val type: String,           // "LIKE" / "COMMENT"
    val senderId: Long,
    val senderNickname: String,
    val senderAvatar: String?,
    val noteId: Long,
    val noteTitle: String,
    val commentContent: String?, // 仅 COMMENT 类型有值
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class UnreadCountResponse(val count: Int)
```

### 3.4 通知页面 UI

时间线列表样式：
- 每条通知是一个卡片
- 左侧彩色竖条：红色 = 点赞，蓝色 = 评论
- 头像 + 昵称 + 操作描述 + 笔记标题 + 时间
- 右侧显示笔记缩略图（如有）
- 未读通知背景略高亮
- 点击通知跳转到对应笔记详情

### 3.5 WebSocket 客户端

使用 OkHttp 的 WebSocket 支持：

```kotlin
@Singleton
class NotificationWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private var webSocket: WebSocket? = null
    private val _notifications = MutableSharedFlow<NotificationPush>(extraBufferCapacity = 1)
    val notifications = _notifications.asSharedFlow()

    fun connect() {
        val token = tokenManager.getTokenSync()
        val request = Request.Builder()
            .url("${BASE_URL.replace("http","ws")}ws/notification?token=$token")
            .build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(ws: WebSocket, text: String) {
                val push = Json.decodeFromString<NotificationPush>(text)
                _notifications.tryEmit(push)
            }
        })
    }

    fun disconnect() { webSocket?.close(1000, "bye") }
}
```

### 3.6 红点逻辑

```
MainViewModel
├── unreadNotificationCount: StateFlow<Int>
├── WebSocketClient.notifications 收集 → count++
└── 进入通知页 → markAllAsRead() → count = 0

BottomNavBar
└── "我的" Tab 图标上显示 Badge（当 count > 0）
```

### 3.7 导航变更

- "我的" 页面（ProfileScreen）顶部增加铃铛图标按钮
- 点击跳转到 `notification` 路由
- 新增 `composable("notification") { NotificationScreen(...) }`

### 3.8 生命周期

- App 启动且已登录 → 连接 WebSocket
- 收到推送 → 更新未读数 → 触发红点
- 进入通知页 → 拉取列表 + 标记已读 → 红点消失
- App 退出或登出 → 断开 WebSocket

## 4. 工作量估算

### 后端（~7 个文件）

| 文件 | 类型 | 说明 |
|------|------|------|
| Notification.java | 新增 | Entity |
| NotificationRepository.java | 新增 | Repository |
| NotificationService.java | 新增 | 通知业务逻辑 |
| NotificationController.java | 新增 | REST API |
| NotificationWebSocketHandler.java | 新增 | WebSocket 处理 |
| WebSocketConfig.java | 新增 | WebSocket 配置 |
| LikeService.java | 修改 | 点赞后触发通知 |
| CommentService.java | 修改 | 评论后触发通知 |

### Android（~7 个文件）

| 文件 | 类型 | 说明 |
|------|------|------|
| NotificationApi.kt | 新增 | Retrofit 接口 |
| NotificationRepository.kt | 新增 | 数据仓库 |
| NotificationModels.kt | 新增 | 数据模型 |
| NotificationScreen.kt | 新增 | 通知页面 |
| NotificationViewModel.kt | 新增 | 状态管理 |
| NotificationWebSocketClient.kt | 新增 | WebSocket 客户端 |
| NetworkModule.kt | 修改 | 注入 NotificationApi |
| MainViewModel.kt | 修改 | 管理未读数 |
| BottomNavBar.kt | 修改 | 显示红点 |
| ProfileScreen.kt | 修改 | 增加通知入口 |
| MainActivity.kt | 修改 | 注册路由 |
