# 随记 NoteShare 架构设计方案 v1

> 项目代号：NoteShare（Android） / noteshare-server（后端）
> 包名：com.example.noteshare
> 文档版本：v1.2 | 最后更新：2026-06-01
> 适用范围：随记 NoteShare 全栈开发的基线参考
> 定位：课设项目，以能跑通演示为最高优先级，不过度设计。

---

## 1. 项目概述

### 1.1 一句话简介

随记 NoteShare：一个图文笔记分享 App。用户可以注册登录、浏览首页笔记流、发布图文笔记、点赞评论、关注他人，并通过关键词搜索笔记。

### 1.2 MVP 功能范围

| 模块 | 功能 | Sprint |
|------|------|--------|
| 账号体系 | 注册、登录（JWT）、查看/编辑个人资料（昵称、头像、简介） | Sprint 1 |
| 笔记流 | 首页"最新笔记"分页列表（图文卡片）、笔记详情页 | Sprint 1 |
| 发布笔记 | 标题 + 正文 + 图片上传（最多 3 张） | Sprint 2 |
| 社交互动 | 点赞/取消点赞、单层评论、关注/取消关注、粉丝数 | Sprint 2 |
| 搜索 | 按关键词模糊匹配笔记标题/正文 | Sprint 2 |

### 1.3 明确不做（非目标）

消息/通知/私信、收藏/转发/@提及/标签、推荐算法、楼中楼评论、云存储/CDN/OSS、管理员后台/审核/举报、多语言/深浅色主题、手机号/验证码/第三方登录、refresh token、搜索分词/高亮/历史/热搜、笔记编辑（发布后只能删除）。

---

## 2. 技术栈

### 2.1 Android 客户端

| 类别 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| 语言 | Kotlin | - | 编译目标 JVM 11 |
| UI 框架 | Jetpack Compose + Material Design 3 | Compose BOM 2024.12.01 | 声明式 UI |
| 架构模式 | MVVM + ViewModel + StateFlow | - | UDF 单向数据流 |
| 依赖注入 | Hilt | 2.51.1 | @HiltAndroidApp + @HiltViewModel |
| 网络 | Retrofit + OkHttp | Retrofit 2.11.0 / OkHttp 4.12.0 | Kotlinx Serialization 替代 Gson |
| 序列化 | Kotlinx Serialization | 1.7.3 | 配合 retrofit2-converter-kotlinx-serialization |
| 图片加载 | Coil Compose | 2.7.0 | - |
| 导航 | Navigation Compose | 2.8.5 | 路由 sealed class + NavHost |
| 本地存储 | DataStore Preferences | 1.1.1 | 存 JWT token + 用户基础信息 |
| 生命周期 | lifecycle-viewmodel-compose / lifecycle-runtime-compose | 2.8.7 | collectAsStateWithLifecycle |
| 构建 | Gradle Kotlin DSL | 9.4.1 | AGP 9.2.1 |

### 2.2 后端服务

| 类别 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| 语言 | Java | 17 | - |
| 框架 | Spring Boot | 3.3.0 | 单模块 |
| 安全 | Spring Security + JWT (jjwt) | jjwt 0.12.6 | BCrypt 密码加密 |
| ORM | Spring Data JPA (Hibernate) | - | ddl-auto: update（开发期） |
| 数据库 | MySQL 8 | - | 开发期可用 H2 内存库快速联调 |
| 分页 | Spring Data Pageable | - | - |
| 文件存储 | 本地磁盘 + 静态资源映射 | - | /uploads/** |
| 校验 | Spring Boot Starter Validation | - | @Valid + @NotBlank/@Size/@Pattern |
| 构建 | Maven | - | pom.xml |

### 2.3 通信协议

- RESTful API + JSON
- 统一前缀 `/api`
- 认证方式：`Authorization: Bearer <JWT>`
- JSON 字段命名：camelCase（前后端统一）
- Android 模拟器 BASE_URL：`http://10.0.2.2:8080/`

---

## 3. 目录结构

### 3.1 Android 端目录结构

基础路径：`app/src/main/java/com/example/noteshare/`

```
com.example.noteshare/
|
|-- NoteShareApp.kt                        # Application 类，@HiltAndroidApp 注解
|-- MainActivity.kt                        # 唯一 Activity，承载 Compose NavHost
|-- MainViewModel.kt                       # 全局 ViewModel（登录态管理）
|
|-- core/
|   |-- network/
|   |   |-- TokenInterceptor.kt            # OkHttp Interceptor，自动注入 Authorization header
|   |   |-- NetworkModule.kt               # Hilt @Module，提供 OkHttpClient、Retrofit 实例
|   |   |-- ApiResponse.kt                 # 统一响应包装 data class（code/message/data/requestId）
|   |   +-- UploadApi.kt                   # 文件上传 Retrofit 接口
|   |
|   |-- datastore/
|   |   +-- TokenManager.kt                # DataStore 读写封装，token + 用户基础信息
|   |
|   |-- di/
|   |   +-- DataStoreModule.kt             # Hilt @Module，提供 DataStore 实例
|   |
|   +-- common/
|       |-- Result.kt                      # 封装 Success / Error / Loading 的泛型结果类
|       +-- ErrorCode.kt                   # 错误码常量定义
|
|-- feature/
|   |-- auth/
|   |   |-- data/
|   |   |   |-- AuthApi.kt                 # Retrofit 登录/注册接口
|   |   |   +-- AuthRepository.kt          # 登录/注册数据访问
|   |   |-- domain/model/
|   |   |   +-- AuthModels.kt              # Login/Register/User 响应模型
|   |   +-- presentation/
|   |       |-- LoginScreen.kt
|   |       |-- LoginViewModel.kt
|   |       |-- RegisterScreen.kt
|   |       +-- RegisterViewModel.kt
|   |
|   |-- feed/
|   |   |-- data/
|   |   |   |-- NoteApi.kt                 # Retrofit 笔记/互动接口
|   |   |   |-- FeedRepository.kt          # 笔记列表数据访问
|   |   |   +-- NoteDetailRepository.kt    # 笔记详情/点赞/评论数据访问
|   |   |-- domain/model/
|   |   |   +-- NoteModels.kt              # 笔记/评论/分页模型
|   |   +-- presentation/
|   |       |-- FeedListScreen.kt
|   |       |-- FeedListViewModel.kt
|   |       |-- NoteDetailScreen.kt
|   |       |-- NoteDetailViewModel.kt
|   |       |-- SearchScreen.kt
|   |       +-- SearchViewModel.kt
|   |
|   |-- publish/
|   |   |-- data/
|   |   |   +-- PublishRepository.kt       # 发布笔记数据访问
|   |   +-- presentation/
|   |       |-- PublishScreen.kt
|   |       +-- PublishViewModel.kt
|   |
|   +-- profile/
|       |-- data/
|       |   |-- UserApi.kt                 # Retrofit 用户资料接口
|       |   +-- ProfileRepository.kt       # 用户资料/关注数据访问
|       |-- domain/model/
|       |   +-- ProfileModels.kt           # 用户资料模型
|       +-- presentation/
|           |-- ProfileScreen.kt
|           |-- ProfileViewModel.kt
|           |-- EditProfileScreen.kt
|           +-- EditProfileViewModel.kt
|
+-- shared/
    +-- ui/
        |-- NoteCard.kt                    # 笔记卡片组件（列表/搜索共用）
        +-- BottomNavBar.kt                # 底部导航栏
```

### 3.2 后端目录结构

```
noteshare-server/
|-- pom.xml
|-- uploads/                              # 本地图片存储（.gitignore）
+-- src/
    +-- main/
        |-- java/com/example/noteshare/
        |   |-- NoteShareApplication.java         # 启动类
        |   |
        |   |-- config/
        |   |   |-- SecurityConfig.java           # Spring Security 过滤链
        |   |   |-- CorsConfig.java               # CORS 跨域配置
        |   |   +-- WebMvcConfig.java             # 静态资源映射 /uploads/**
        |   |
        |   |-- security/
        |   |   |-- JwtUtil.java                  # JWT 生成/解析/验证
        |   |   |-- UserDetailsImpl.java          # 实现 UserDetails
        |   |   +-- JwtAuthFilter.java            # OncePerRequestFilter
        |   |
        |   |-- entity/
        |   |   |-- User.java
        |   |   |-- Note.java
        |   |   |-- NoteImage.java
        |   |   |-- LikeRel.java
        |   |   |-- Comment.java
        |   |   +-- Follow.java
        |   |
        |   |-- repository/
        |   |   |-- UserRepository.java
        |   |   |-- NoteRepository.java
        |   |   |-- NoteImageRepository.java
        |   |   |-- LikeRelRepository.java
        |   |   |-- CommentRepository.java
        |   |   +-- FollowRepository.java
        |   |
        |   |-- service/
        |   |   |-- AuthService.java              # 注册/登录
        |   |   |-- UserService.java              # 用户资料/关注
        |   |   |-- NoteService.java              # 笔记 CRUD + 搜索
        |   |   |-- CommentService.java           # 评论
        |   |   |-- FollowService.java            # 关注/取关/计数
        |   |   |-- LikeService.java              # 点赞/取消赞
        |   |   +-- FileService.java              # 图片上传
        |   |
        |   |-- controller/
        |   |   |-- AuthController.java           # /api/auth/*
        |   |   |-- UserController.java           # /api/users/*
        |   |   |-- NoteController.java           # /api/notes/*
        |   |   +-- UploadController.java         # /api/upload
        |   |
        |   |-- dto/
        |   |   |-- request/
        |   |   |   |-- RegisterRequest.java
        |   |   |   |-- LoginRequest.java
        |   |   |   |-- UpdateProfileRequest.java
        |   |   |   |-- CreateNoteRequest.java
        |   |   |   +-- CreateCommentRequest.java
        |   |   +-- response/
        |   |       |-- UserResponse.java
        |   |       |-- NoteResponse.java
        |   |       |-- NoteDetailResponse.java
        |   |       |-- CommentResponse.java
        |   |       +-- PageResponse.java
        |   |
        |   +-- common/
        |       |-- ApiResponse.java              # 统一响应包装
        |       |-- ErrorCode.java                # 错误码枚举
        |       +-- GlobalExceptionHandler.java   # 全局异常处理
        |
        +-- resources/
            |-- application.yml                   # 主配置（MySQL）
            +-- application-dev.yml               # 开发配置（H2）
```

---

## 4. 数据库设计

### 4.1 ER 概览

```
User (1) --< Note (N)           via Note.authorId = User.id
Note (1) --< NoteImage (N)      via NoteImage.noteId = Note.id     [max 3]
User (N) >--< Note (N)          via LikeRel (userId, note_id)       [unique]
Note (1) --< Comment (N)        via Comment.noteId = Note.id
User (1) --< Comment (N)        via Comment.userId = User.id
User (N) >--< User (N)          via Follow (followerId, followeeId) [unique]
```

### 4.2 表结构

| 表名 | 字段 | 说明 |
|------|------|------|
| users | id (PK, AUTO_INCREMENT), username (UNIQUE, VARCHAR 50), password_hash (VARCHAR 100), nickname (VARCHAR 50), avatar_url (VARCHAR 500), bio (VARCHAR 500), created_at (DATETIME) | 用户表 |
| notes | id (PK), author_id (FK->users.id), title (VARCHAR 100), content (TEXT), like_count (INT, default 0), comment_count (INT, default 0), created_at (DATETIME) | 笔记表 |
| note_images | id (PK), note_id (FK->notes.id), url (VARCHAR 500), sort (INT) | 笔记图片表，sort 控制顺序 |
| like_rel | id (PK), user_id (FK->users.id), note_id (FK->notes.id), created_at (DATETIME), UNIQUE(user_id, note_id) | 点赞关系表 |
| comments | id (PK), note_id (FK->notes.id), user_id (FK->users.id), content (TEXT), created_at (DATETIME) | 评论表 |
| follows | id (PK), follower_id (FK->users.id), followee_id (FK->users.id), created_at (DATETIME), UNIQUE(follower_id, followee_id) | 关注关系表 |

### 4.3 设计决策

- 所有外键存 Long 值，不使用 JPA `@ManyToOne` / `@OneToMany` 关联映射，避免循环序列化和 N+1 问题。
- `like_count` / `comment_count` 为冗余计数字段，写操作时 Service 层同步 +1 / -1。
- 图片拆到 `note_images` 表，1:N 关系，最多 3 条。
- `createdAt` 用 `@PrePersist` 自动填充，`updatable = false`。
- 字段命名统一 camelCase（Java/Kotlin），数据库列名通过 `@Column(name=...)` 映射下划线风格。

### 4.4 关键查询

| 场景 | 查询方式 |
|------|---------|
| 首页笔记列表 | NoteRepository.findAll(Pageable) + 逐条查 Author |
| 笔记详情 | NoteRepository.findById + NoteImageRepository.findByNoteIdOrderBySortAsc + 查 Author + 查 liked 状态 |
| 搜索笔记 | @Query("WHERE title LIKE %:keyword% OR content LIKE %:keyword% ORDER BY createdAt DESC") |
| 点赞计数 | Note.likeCount 冗余字段，@Modifying @Query("SET likeCount = likeCount + 1") |
| 评论计数 | Note.commentCount 冗余字段，同上 |
| 关注数 | FollowRepository.countByFollowerId(userId) |
| 粉丝数 | FollowRepository.countByFolloweeId(userId) |
| 是否已赞 | LikeRelRepository.existsByUserIdAndNoteId(userId, noteId) |
| 是否已关注 | FollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId) |

---

## 5. API v1 决策版

### 5.1 统一响应格式

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "a1b2c3d4e5f6..."
}
```

**分页响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "hasMore": true
  },
  "requestId": "..."
}
```

**错误响应：**

```json
{
  "code": 40010,
  "message": "用户名已存在",
  "data": null,
  "requestId": "..."
}
```

**后端 Java 包装类：** `ApiResponse<T>` — 字段 `code`, `message`, `data`, `requestId`
**Android Kotlin 包装类：** `ApiResponse<T>` — 字段 `code`, `message`, `data`, `requestId`（Kotlinx Serializable）

### 5.2 接口总览（17 个端点）

| # | 方法 | 路径 | 认证 | 功能 | 请求体/参数 | 响应 data |
|---|------|------|------|------|------------|----------|
| 1 | POST | /api/auth/register | 公开 | 注册 | `{username, password}` | `UserResponse` |
| 2 | POST | /api/auth/login | 公开 | 登录 | `{username, password}` | `{token, user}` |
| 3 | GET | /api/users/me | 认证 | 当前用户资料 | - | `UserResponse` |
| 4 | PUT | /api/users/me | 认证 | 编辑资料 | `{nickname, avatarUrl, bio}` | `UserResponse` |
| 5 | GET | /api/users/{id} | 公开 | 查看他人资料 | - | `UserResponse`（含 followerCount, followingCount, followed） |
| 6 | POST | /api/users/{id}/follow | 认证 | 关注 | - | `null` |
| 7 | DELETE | /api/users/{id}/follow | 认证 | 取消关注 | - | `null` |
| 8 | GET | /api/notes | 公开 | 笔记列表 | `?page=1&size=20` | `PageResponse<NoteResponse>` |
| 9 | GET | /api/notes/search | 公开 | 搜索 | `?keyword=&page=1&size=20` | `PageResponse<NoteResponse>` |
| 10 | GET | /api/notes/{id} | 公开 | 笔记详情 | - | `NoteDetailResponse` |
| 11 | POST | /api/notes | 认证 | 发布笔记 | `{title, content, imageUrls[]}` | `NoteResponse` |
| 12 | DELETE | /api/notes/{id} | 认证 | 删除笔记 | - | `null` |
| 13 | POST | /api/notes/{id}/like | 认证 | 点赞 | - | `null` |
| 14 | DELETE | /api/notes/{id}/like | 认证 | 取消赞 | - | `null` |
| 15 | GET | /api/notes/{id}/comments | 公开 | 评论列表 | `?page=1&size=20` | `PageResponse<CommentResponse>` |
| 16 | POST | /api/notes/{id}/comments | 认证 | 发表评论 | `{content}` | `CommentResponse` |
| 17 | POST | /api/upload | 认证 | 上传图片 | `multipart: file` | `String`（URL） |

### 5.3 公开 vs 认证路径

| 路径 | 方法 | 是否需要认证 |
|------|------|-------------|
| /api/auth/register | POST | 公开 |
| /api/auth/login | POST | 公开 |
| /api/notes | GET | 公开 |
| /api/notes/search | GET | 公开 |
| /api/notes/{id} | GET | 公开 |
| /api/notes/{id}/comments | GET | 公开 |
| /api/users/{id} | GET | 公开 |
| /uploads/** | GET | 公开 |
| /api/upload | POST | 认证（需 `Authorization: Bearer <token>`） |
| /api/users/me | GET/PUT | 认证 |
| /api/notes | POST | 认证 |
| /api/notes/{id} | DELETE | 认证 |
| /api/notes/{id}/like | POST/DELETE | 认证 |
| /api/notes/{id}/comments | POST | 认证 |
| /api/users/{id}/follow | POST/DELETE | 认证 |

### 5.4 错误码体系

| 范围 | 模块 | 错误码 | 说明 |
|------|------|--------|------|
| 通用 | - | 0 | 成功 |
| 通用 | - | 40000 | 参数校验失败 |
| 通用 | - | 50000 | 服务器内部错误 |
| 认证 | Auth | 40100 | 未提供认证令牌 |
| 认证 | Auth | 40101 | 令牌无效或已过期 |
| 认证 | Auth | 40102 | 令牌已过期 |
| 认证 | Auth | 40103 | 未登录 |
| 认证 | Auth | 40104 | 无权操作 |
| 注册登录 | Auth | 40010 | 用户名已存在 |
| 注册登录 | Auth | 40011 | 用户名格式不正确（3-50 字符，字母数字下划线） |
| 注册登录 | Auth | 40012 | 密码长度需 6-50 字符 |
| 注册登录 | Auth | 40020 | 用户名或密码错误 |
| 用户 | User | 40200 | 用户不存在 |
| 用户 | User | 40210 | 不能关注自己 |
| 用户 | User | 40211 | 已关注该用户 |
| 用户 | User | 40212 | 未关注该用户 |
| 笔记 | Note | 40300 | 笔记不存在 |
| 笔记 | Note | 40301 | 无权操作该笔记 |
| 笔记 | Note | 40310 | 标题不能为空 |
| 笔记 | Note | 40311 | 标题不能超过 100 字符 |
| 笔记 | Note | 40312 | 正文不能为空 |
| 点赞 | Like | 40400 | 已经点赞过了 |
| 点赞 | Like | 40401 | 未点赞 |
| 评论 | Comment | 40500 | 评论内容不能为空 |
| 上传 | Upload | 40600 | 文件不能为空 |
| 上传 | Upload | 40601 | 仅支持 jpg/png/gif/webp 格式 |
| 上传 | Upload | 40602 | 单张图片不能超过 5MB |
| 上传 | Upload | 40603 | 文件上传失败 |

### 5.5 前后端 DTO 对应关系

以下确保 Android 端 data class 与后端 Java DTO 字段完全一致（camelCase）：

**Request DTO：**

| 后端 Java 类 | Android Kotlin 类 | 字段 |
|-------------|------------------|------|
| RegisterRequest | RegisterRequest | username: String, password: String |
| LoginRequest | LoginRequest | username: String, password: String |
| UpdateProfileRequest | UpdateProfileRequest | nickname: String?, avatarUrl: String?, bio: String? |
| CreateNoteRequest | CreateNoteRequest | title: String, content: String, imageUrls: List<String>? |
| CreateCommentRequest | CreateCommentRequest | content: String |

**Response DTO：**

| 后端 Java 类 | Android Kotlin 类 | 字段 |
|-------------|------------------|------|
| UserResponse | UserResponse | id, username, nickname, avatarUrl, bio, followerCount?, followingCount?, followed? |
| NoteResponse | NoteResponse | id, title, content, imageUrls, likeCount, commentCount, createdAt, author(AuthorBrief) |
| NoteDetailResponse | NoteDetailResponse | id, title, content, imageUrls, likeCount, commentCount, liked?, createdAt, author(AuthorBrief) |
| CommentResponse | CommentResponse | id, content, createdAt, author(AuthorBrief) |
| AuthorBrief | AuthorBrief | id, nickname, avatarUrl |
| PageResponse<T> | PageData<T> | items, page, pageSize, total, hasMore |
| LoginResponse（data） | LoginResponse | token: String, user: UserResponse |

**注意：** 后端 PageResponse 字段 `page` 从 1 开始（Controller 层做 page-1 转换给 Spring Data JPA），Android 端 PageData.page 也从 1 开始。

---

## 6. Android 端架构设计

### 6.1 整体架构：UDF 单向数据流

```
用户操作（点击/输入）
    |
    v
Composable（UI 层）
    |  调用 ViewModel 方法
    v
ViewModel
    |  调用 Repository 方法
    v
Repository（data 层具体类）
    |  调用对应 Retrofit Api
    v
Retrofit API（OkHttp 发起 HTTP 请求）
    |  后端返回 ApiResponse<T>
    v
Repository 解包（检查 code == 0）
    |  返回 Result<T>
    v
ViewModel 将结果转为 UI State
    |  更新 StateFlow
    v
StateFlow 驱动 Composable 重组
    |
    v
UI 刷新显示
```

### 6.2 各层职责边界

| 层 | 职责 | 不做什么 |
|----|------|---------|
| Composable（UI） | 读取 StateFlow，渲染 UI，转发用户事件给 ViewModel | 不直接调用网络、不做业务逻辑判断 |
| ViewModel | 持有 UI State（StateFlow），调用 Repository，处理业务逻辑，错误映射 | 不引用 Android Context（Application 除外）、不直接操作 Retrofit |
| Repository（data） | 调用 Retrofit API，将响应转为 Result，统一错误处理 | 不包含 UI 逻辑 |
| Retrofit Api | 按功能声明 HTTP 方法和路径 | 不做业务逻辑 |
| TokenManager（core/datastore） | 读写 token 和用户信息 | 不做网络请求 |

### 6.3 Retrofit 接口定义

接口按功能拆分为 `AuthApi.kt`、`NoteApi.kt`、`UserApi.kt`、`UploadApi.kt`：

```kotlin
interface AuthApi {

    // ===== 鉴权 =====
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // ===== 用户 =====
    @GET("api/users/me")
    suspend fun getCurrentUser(): ApiResponse<UserResponse>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserResponse>

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: Long): ApiResponse<UserResponse>

    @POST("api/users/{id}/follow")
    suspend fun followUser(@Path("id") userId: Long): ApiResponse<Unit>

    @DELETE("api/users/{id}/follow")
    suspend fun unfollowUser(@Path("id") userId: Long): ApiResponse<Unit>

    // ===== 笔记 =====
    @GET("api/notes")
    suspend fun getNotes(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NoteResponse>>

    @GET("api/notes/search")
    suspend fun searchNotes(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NoteResponse>>

    @GET("api/notes/{id}")
    suspend fun getNoteDetail(@Path("id") noteId: Long): ApiResponse<NoteDetailResponse>

    @POST("api/notes")
    suspend fun createNote(@Body request: CreateNoteRequest): ApiResponse<NoteResponse>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") noteId: Long): ApiResponse<Unit>

    // ===== 互动 =====
    @POST("api/notes/{id}/like")
    suspend fun likeNote(@Path("id") noteId: Long): ApiResponse<Unit>

    @DELETE("api/notes/{id}/like")
    suspend fun unlikeNote(@Path("id") noteId: Long): ApiResponse<Unit>

    @GET("api/notes/{id}/comments")
    suspend fun getComments(
        @Path("id") noteId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<CommentResponse>>

    @POST("api/notes/{id}/comments")
    suspend fun createComment(
        @Path("id") noteId: Long,
        @Body request: CreateCommentRequest
    ): ApiResponse<CommentResponse>

    // ===== 文件上传 =====
    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ApiResponse<String>
}
```

### 6.4 统一 ApiResponse 解包

```kotlin
// core/network/ApiResponse.kt
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
    val requestId: String? = null
)

// core/common/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: Int, val message: String) : Result<Nothing>()
}
```

**Repository 内统一解包模式：**

```kotlin
suspend fun getNotes(page: Int, size: Int = 20): Result<PageData<NoteResponse>> {
    return try {
        val response = noteApi.getNotes(page, size)
        if (response.code == ErrorCode.SUCCESS && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(response.code, response.message)
        }
    } catch (e: Exception) {
        Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
    }
}
```

### 6.5 Token 注入拦截器

```kotlin
class TokenInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val token = runBlocking { tokenManager.getToken() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
```

### 6.6 状态管理设计

每个页面定义一个 data class 作为 UI State，ViewModel 通过 `MutableStateFlow + asStateFlow()` 暴露给 UI。

**关键页面 UI State：**

| 页面 | 核心字段 |
|------|---------|
| LoginUiState | username, password, isLoading, error, loginSuccess |
| RegisterUiState | username, password, confirmPassword, isLoading, error, registerSuccess |
| FeedListUiState | notes, isLoading, isLoadingMore, hasMore, currentPage, error, loadMoreError |
| NoteDetailUiState | note, comments, isLoading, isLiking, isCommenting, commentText, hasMoreComments, error |
| SearchUiState | keyword, results, isLoading, isLoadingMore, hasMore, currentPage, error, hasSearched |
| PublishUiState | title, content, selectedImages, uploadedImageUrls, isUploading, isPublishing, error, publishSuccess |
| ProfileUiState | user, notes, isLoading, isLoadingMore, hasMore, notesPage, isFollowing, isFollowLoading, error, isCurrentUser |
| EditProfileUiState | nickname, bio, avatarUrl, newAvatarUri, isLoading, isSaving, error, saveSuccess |

**状态展示规则：**

| 状态 | 展示方式 |
|------|---------|
| `isLoading = true` | 全屏居中 CircularProgressIndicator |
| `error != null` | 页面内错误文本或 Snackbar/Toast |
| `items.isEmpty() && !isLoading` | 页面内空状态文本（如"暂无笔记"） |
| `items.isNotEmpty()` | 正常列表 |
| `isLoadingMore = true` | 列表底部显示小 Loading |
| `loadMoreError != null` | 列表底部显示"加载失败，点击重试" |
| `hasMore = false && items.isNotEmpty()` | 列表底部显示"没有更多了" |

### 6.7 导航设计

```kotlin
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object Publish : Screen("publish")
    data object Profile : Screen("profile")
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    data object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: Long) = "user_profile/$userId"
    }
    data object EditProfile : Screen("edit_profile")
    data object Search : Screen("search")
}
```

底部导航栏三个 Tab：首页（Feed）、发布（Publish）、我的（Profile）。

参数传递：NoteDetail 和 UserProfile 使用路径参数 `{noteId}` / `{userId}` + `NavType.LongType`。

### 6.8 依赖注入设计

**Hilt Module 划分：**

| Module | 提供内容 | Scope |
|--------|---------|-------|
| NetworkModule | OkHttpClient, Retrofit, 各 Retrofit Api | @Singleton |
| DataStoreModule | DataStore<Preferences> | @Singleton |
| Repository 具体类 | 通过 `@Inject constructor` 直接注入 | 由 Hilt 构造 |

Repository 直接注入到 ViewModel；当前实现未保留单独的 Repository 绑定模块。

### 6.9 DataStore 存储设计

| Key | 类型 | 说明 |
|-----|------|------|
| token | String | JWT Token |
| user_id | Long | 当前用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar_url | String | 头像 URL |

写入时机：登录/注册成功后调用 `saveLoginInfo()`。清除时机：用户退出登录或 token 过期。

---

## 7. 后端架构设计

### 7.1 分层架构

```
Controller --> Service --> Repository --> JPA Entity
```

### 7.2 安全设计

**JWT 流程：**
- 依赖：jjwt-api 0.12.6 + jjwt-impl + jjwt-jackson
- Token 有效期：7 天（不实现 refresh token）
- Payload：subject = userId, claim = username
- 签名算法：HMAC-SHA256

**BCrypt 密码加密：**
- 注册时 `passwordEncoder.encode(rawPassword)` 存入 `password_hash`
- 登录时 `passwordEncoder.matches(rawPassword, user.getPasswordHash())`

**SecurityFilterChain 配置要点：**
- CSRF 禁用（无状态 JWT）
- Session 策略：STATELESS
- 公开接口见 5.3 节公开路径表
- 其余接口走 `JwtAuthFilter` 认证

**JwtAuthFilter 逻辑：**
1. 从 `Authorization` header 提取 Bearer token
2. 调用 `jwtUtil.validateToken(token)` 验证
3. 验证通过则从 token 提取 userId，构建 `UsernamePasswordAuthenticationToken` 放入 `SecurityContextHolder`
4. 验证失败则不设置认证，后续 `@PreAuthorize` 或配置链会拦截

**Controller 获取当前用户：**
```java
// 工具方法
public static Long currentUserId() {
    UserDetailsImpl details = (UserDetailsImpl)
        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return details.getUserId();
}

// 公开接口中安全获取（未登录返回 null）
public static Long currentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
        return null;
    }
    return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
}
```

### 7.3 全局异常处理

`GlobalExceptionHandler` (`@RestControllerAdvice`) 统一捕获：

| 异常类型 | 处理方式 |
|---------|---------|
| BusinessException | 返回对应 ErrorCode 的 ApiResponse |
| MethodArgumentNotValidException | 拼接字段错误信息，返回 PARAM_INVALID |
| MethodArgumentTypeMismatchException | 返回 PARAM_INVALID |
| HttpMessageNotReadableException | 返回 PARAM_INVALID |
| AccessDeniedException | 返回 AUTH_FORBIDDEN |
| Exception（兜底） | 返回 INTERNAL_ERROR |

### 7.4 文件上传设计

- 存储路径：`./uploads/` + 按日期分目录（`20260601/`）
- 文件名：UUID + MIME 类型映射扩展名
- 校验：类型（jpg/png/gif/webp）、大小（<=5MB）
- 返回 URL 格式：`/uploads/20260601/uuid.jpg`
- 静态资源映射：`WebMvcConfig` 将 `/uploads/**` 映射到本地磁盘
- 上传流程（Android 端视角）：先逐张调用 POST /api/upload 获得 URL 列表，再调用 POST /api/notes 提交
- Android 显示流程：后端返回相对路径 `/uploads/**`，客户端通过 `resolveMediaUrl` 补全为 `BuildConfig.BASE_URL + path` 后交给 Coil 加载

### 7.5 关键实现提醒

1. `@Modifying` + `@Transactional`：自定义更新查询需要加 `@Transactional`。
2. `deleteByXxx` 也需要 `@Transactional`。
3. 分页转换：Spring Data JPA 从 0 开始，Controller 接收前端 page 从 1 开始，需做 `page - 1`。
4. JSON 字段命名：统一 camelCase。
5. 删除笔记时需手动删除关联的 NoteImage、LikeRel、Comment。
6. `upload` 接口需要认证；静态资源 `/uploads/**` 保持公开读取。

---

## 8. Sprint 1 标注与实现顺序

### 8.1 Sprint 1 范围

**Sprint 1 目标：账号体系 + 首页笔记流（列表 + 详情），端到端跑通。**

包含的功能点：

| 序号 | 功能 | 后端接口 | Android 页面 |
|------|------|---------|-------------|
| 1 | 用户注册 | POST /api/auth/register | RegisterScreen |
| 2 | 用户登录 | POST /api/auth/login | LoginScreen |
| 3 | JWT 鉴权基础设施 | SecurityConfig + JwtUtil + JwtAuthFilter | TokenInterceptor + TokenManager |
| 4 | 首页笔记列表 | GET /api/notes | FeedListScreen |
| 5 | 笔记详情 | GET /api/notes/{id} | NoteDetailScreen |
| 6 | 点赞/取消赞 | POST/DELETE /api/notes/{id}/like | NoteDetailScreen 内 |
| 7 | 评论列表 | GET /api/notes/{id}/comments | NoteDetailScreen 内 |
| 8 | 发表评论 | POST /api/notes/{id}/comments | NoteDetailScreen 内 |
| 9 | 查看当前用户资料 | GET /api/users/me | ProfileScreen |
| 10 | 统一响应/异常/错误码 | ApiResponse + ErrorCode + GlobalExceptionHandler | Repository 内 try/catch + ErrorCode |

**Sprint 2 排期（Sprint 1 跑通后）：**

| 功能 | 后端接口 | Android 页面 |
|------|---------|-------------|
| 发布笔记 | POST /api/notes + POST /api/upload | PublishScreen |
| 删除笔记 | DELETE /api/notes/{id} | NoteDetailScreen 长按菜单 |
| 编辑资料 | PUT /api/users/me | EditProfileScreen |
| 查看他人资料 | GET /api/users/{id} | ProfileScreen(userId) |
| 关注/取消关注 | POST/DELETE /api/users/{id}/follow | ProfileScreen 内 |
| 搜索笔记 | GET /api/notes/search | SearchScreen |

### 8.2 实现顺序建议

以下是推荐的实现顺序，前后端可交替或并行推进：

```
阶段 A：基础设施搭建
=============================================
1. [BACKEND]  创建 Maven 项目 + pom.xml 全部依赖
2. [FRONTEND] Android 项目迁移（包名、Kotlin、Compose、依赖、目录结构）
3. [BACKEND]  application.yml 配置（MySQL/H2、JWT、文件存储）
4. [FRONTEND] build.gradle.kts 配置（全部插件和依赖）

阶段 B：核心基础设施层
=============================================
5. [BACKEND]  JPA Entity 层（6 个实体类）+ Repository 层（6 个接口）
6. [BACKEND]  统一响应 ApiResponse + ErrorCode + BusinessException + GlobalExceptionHandler
7. [BACKEND]  SecurityConfig + JwtUtil + JwtAuthFilter + CorsConfig + PasswordEncoder
8. [FRONTEND] core/network（ApiResponse.kt, TokenInterceptor.kt, NetworkModule.kt, AuthApi/NoteApi/UserApi/UploadApi）
9. [FRONTEND] core/datastore（TokenManager.kt, DataStoreModule.kt）
10. [FRONTEND] core/common（Result.kt, ErrorCode.kt）
11. [FRONTEND] core/di（NetworkModule.kt, DataStoreModule.kt）

阶段 C：鉴权模块（Sprint 1 核心）
=============================================
12. [BACKEND]  RegisterRequest/LoginRequest/UserResponse DTO
13. [BACKEND]  AuthService + AuthController（注册 + 登录）
14. [FRONTEND] feature/auth/data（AuthApi, AuthRepository）
15. [FRONTEND] feature/auth/domain/model（LoginRequest, RegisterRequest, LoginResponse）
16. [FRONTEND] feature/auth/presentation/login（LoginScreen, LoginViewModel, LoginUiState）
17. [FRONTEND] feature/auth/presentation/register（RegisterScreen, RegisterViewModel, RegisterUiState）
18. [FRONTEND] NoteShareApp.kt + MainActivity.kt（Hilt 入口 + Compose）
>>> 联调检查点：注册 -> 登录 -> Token 保存 -> 跳转首页

阶段 D：笔记流模块（Sprint 1 核心）
=============================================
19. [BACKEND]  NoteResponse/NoteDetailResponse/CommentResponse/AuthorBrief/PageResponse DTO
20. [BACKEND]  NoteService + NoteController（笔记列表、详情、搜索）
21. [BACKEND]  LikeService（点赞/取消赞）
22. [BACKEND]  CommentService + NoteController 评论接口（评论列表、发表评论）
23. [FRONTEND] shared/ui 公共组件（NoteCard, BottomNavBar）
24. [FRONTEND] Navigation 路由 + NavHost + BottomNavBar
25. [FRONTEND] feature/feed/data（NoteApi, FeedRepository, NoteDetailRepository）
26. [FRONTEND] feature/feed/domain/model（数据模型）
27. [FRONTEND] feature/feed/presentation/feedlist（FeedListScreen, ViewModel, 下拉刷新 + 上拉分页）
28. [FRONTEND] feature/feed/presentation/notedetail（NoteDetailScreen, 图片轮播 + 点赞 + 评论）
>>> 联调检查点：首页列表加载 -> 点击详情 -> 点赞 -> 发表评论

阶段 E：收尾
=============================================
29. [BACKEND]  UserService + UserController（GET /api/users/me）
30. [FRONTEND] feature/profile 基础（ProfileScreen 展示当前用户资料）
31. [ALL]      端到端联调：注册 -> 登录 -> 首页列表 -> 详情 -> 点赞/评论 -> 个人资料
32. [REVIEWER] Sprint 1 验收测试
```

### 8.3 前后端并行策略

在阶段 B 完成后，前后端可以并行推进：

- **后端优先路径：** Entity -> Repository -> 统一响应 -> 安全层 -> AuthService -> NoteService -> LikeService -> CommentService
- **前端优先路径：** Retrofit Api 定义 -> TokenManager -> Repository 错误处理 -> LoginScreen -> NavHost -> FeedListScreen -> NoteDetailScreen

两端在"阶段 C 联调检查点"和"阶段 D 联调检查点"汇合验证。

### 8.4 Sprint 1 任务卡概览

| task_id | 任务 | owner | 优先级 |
|---------|------|-------|--------|
| A-001 | Android 项目骨架搭建 | FRONTEND | P0 |
| A-002 | 后端项目骨架搭建 | BACKEND | P0 |
| B-001 | Entity + Repository 层 | BACKEND | P0 |
| B-002 | 统一响应 + 异常处理 | BACKEND | P0 |
| B-003 | 安全层（Security + JWT） | BACKEND | P0 |
| B-004 | DTO 层 | BACKEND | P0 |
| B-005 | AuthService + AuthController | BACKEND | P0 |
| B-006 | NoteService + NoteController | BACKEND | P0 |
| B-007 | LikeService + CommentService | BACKEND | P0 |
| B-008 | UserService + UserController | BACKEND | P1 |
| B-009 | application.yml 配置 | BACKEND | P0 |
| F-001 | core/ 基础设施层 | FRONTEND | P0 |
| F-002 | NoteShareApp + MainActivity | FRONTEND | P0 |
| F-003 | Navigation + BottomNavBar | FRONTEND | P0 |
| F-004 | shared/ui 公共组件 | FRONTEND | P0 |
| F-005 | auth feature（登录 + 注册） | FRONTEND | P0 |
| F-006 | feed feature（列表 + 详情） | FRONTEND | P0 |
| F-007 | profile feature（基础版） | FRONTEND | P1 |
| R-001 | Sprint 1 验收标准 | REVIEWER | P0 |
| R-002 | 后端接口测试 | REVIEWER | P1 |
| R-003 | 前端联调测试 | REVIEWER | P1 |

---

## 9. 配置参考

### 9.1 后端 application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: noteshare-server
  datasource:
    url: jdbc:mysql://localhost:3306/noteshare?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

jwt:
  secret: noteshare-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256
  expiration: 604800000

file:
  upload-dir: ./uploads
  allowed-types:
    - image/jpeg
    - image/png
    - image/gif
    - image/webp
  max-size: 5242880
```

### 9.2 Android BASE_URL 配置

| 环境 | BASE_URL | 说明 |
|------|----------|------|
| 模拟器开发 | `http://10.0.2.2:8080/` | 模拟器通过 10.0.2.2 访问宿主机 localhost |
| 真机开发 | `http://<电脑IP>:8080/` | 手机和电脑在同一局域网 |
| 发布版 | `https://<服务器域名>/` | 上线时修改 |

建议通过 `BuildConfig` 配置，避免硬编码：
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
    }
}
```

---

## 10. 安全与权限规则

1. Android 端敏感信息不得硬编码；token 存 DataStore。
2. 后端不得把密码、Token、密钥、内部错误栈返回给客户端。
3. 密码必须 BCrypt 加密存储。
4. 图片上传需校验类型（jpg/png/gif/webp）与大小（<=5MB）。
5. 笔记只能由作者自己删除（Service 层校验 authorId == userId）。
6. 不能关注自己，重复关注/点赞返回错误码。
7. CORS 配置课设阶段允许所有来源，为 Web 测试做准备。
8. JWT 有效期 7 天，过期后 Android 端跳转登录页。

---

## 11. 已知风险

| 等级 | 风险 | 缓解方案 |
|------|------|----------|
| P1 | Gradle 9.4.1 + AGP 9.2.1 较新，Compose BOM 兼容性需验证 | 优先验证编译通过；若不兼容降级版本 |
| P1 | compileSdk 新语法 `release(36) { minorApiLevel = 1 }` 兼容性 | 验证不通过则改用 `compileSdk = 36` |
| P2 | Android 模拟器通过 10.0.2.2 访问宿主机，真机需同一局域网 | 开发期优先用模拟器 |
| P3 | 图片上传本地存储路径与 Android 相对 URL 显示 | 已修复：后端保存时转绝对路径，Android 用 `resolveMediaUrl` 补全 `/uploads/**` |
| P3 | Spring Data JPA ddl-auto: update 不删列 | 开发期用 H2 create-drop 或手动 DROP TABLE |
| P3 | LIKE %keyword% 模糊搜索性能 | 课设数据量小，无需全文索引 |

---

*文档版本：v1.2 | 最后更新：2026-06-01（同步图片上传认证、异常处理与 Android 媒体 URL 解析约定）*
