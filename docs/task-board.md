# 随记 NoteShare 任务板

> 最后更新：ARCHITECT | 阶段 5 编码协作 Sprint 2 全面质量检测与修复完成

---

## 状态说明
- `TODO` 待开始
- `IN_PROGRESS` 进行中
- `BLOCKED` 被阻塞
- `REVIEWING` 审查中
- `DONE` 已完成

---

## 初始化任务

### INIT-001: 各 Agent 初始化回执
- **owner**: ALL (ARCHITECT / FRONTEND / BACKEND / REVIEWER)
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 确认各 Agent 职责、所需输入、可交付内容、潜在限制
- **验收标准**: 四方全部回执确认
- **完成情况**:
  - ARCHITECT: 已确认职责、建立总控面板与任务板、广播启动消息
  - FRONTEND: UI 状态设计评估完成（Loading/Empty/Error）、PhotoPicker 方案确认
  - BACKEND: 后端架构设计完成、3 个裁决点已获 ARCHITECT 回复
  - REVIEWER: 课设优先级策略确认、范围收敛建议已采纳
- **ARCHITECT 裁决**: ① requestId camelCase ② B-001可先行编码 ③ /api/upload需认证

---

## 架构任务 (A-)

### A-001: Android 项目骨架搭建
- **owner**: FRONTEND
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 维护 com.example.noteshare Android 项目骨架，包含 Kotlin + Compose + Hilt + Navigation + Retrofit 网络层 + DataStore
- **输出**: 13 个 Kotlin 文件，可编译运行的 Android 项目骨架
- **验收标准**: 项目可编译，底部导航可切换，Hilt 注入生效，Retrofit 可发起网络请求
- **完成情况**: 包名迁移完成，AGP 8.5.2 + Kotlin 2.0.20，Navigation 8 路由，NetworkModule 含 TokenInterceptor

### A-002: 后端项目骨架搭建
- **owner**: BACKEND
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 创建 Spring Boot 项目，搭建 Security + JPA + 统一响应/异常处理
- **输出**: 14 个 Java 文件，可启动的 Spring Boot 应用
- **验收标准**: 应用启动无报错，统一响应格式生效，全局异常捕获生效
- **完成情况**: 双 SecurityFilterChain，全部 10 项验收通过，已含 User Entity + UserRepository

### A-003: API v1 决策版确认
- **owner**: ARCHITECT（BACKEND 产出草案，FRONTEND/REVIEWER 审查）
- **reviewer**: ALL
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 基于 0.7 基线清单，产出 17 个接口的完整定义
- **输出**: API v1 决策版文档（architecture-design-v1.md §5）
- **完成情况**: 17 个 API 全部定义完成，前后端 DTO 对应关系已确认

---

## 数据库任务 (DB-)

### DB-001: 数据库表设计与 JPA Entity 实现
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE（合并至 B-003）
- **目标**: 6 张表的 JPA Entity 实现（User, Note, NoteImage, LikeRel, Comment, Follow）
- **完成情况**: 6 Entity + 6 Repository 全部完成，随 B-003 一起交付

---

## 后端任务 (B-)

### B-001: 后端骨架搭建
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: Spring Boot + Security + JPA + 统一响应/异常 + CORS + 静态资源映射
- **验收标准**: 应用启动无报错，统一响应格式生效，全局异常捕获生效
- **完成情况**: 17 个文件，双 SecurityFilterChain，全部 10 项验收通过

### B-002: 注册接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: POST /api/auth/register + POST /api/auth/login
- **验收标准**: 用户名唯一校验、密码 BCrypt 加密、返回统一响应、JWT token 生成
- **完成情况**: 8 个文件（Entity/Repository/DTO/Service/Controller），注册+登录全部验收通过

### B-003: 登录接口（JWT）+ 全部 API 实现
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: POST /api/auth/login + 全部 17 个 API 实现
- **完成情况**: 17 个 API 全部实现完成（Auth/Note/User/Follow/Like/Comment/Search/Upload），47 个 Java 文件

### B-004: 用户资料接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE（合并至 B-003）
- **目标**: GET/PUT /api/users/me, GET /api/users/{id}
- **完成情况**: 随 B-003 一起实现，含关注/粉丝数、是否已关注字段

### B-005: 笔记 CRUD 接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE (合并至 B-003)
- **目标**: GET /api/notes, GET /api/notes/{id}, POST /api/notes, DELETE /api/notes/{id}
- **完成情况**: B-003 统一完成，17 个 API 全部实现

### B-006: 关注/取关接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE (合并至 B-003)
- **目标**: POST/DELETE /api/users/{id}/follow
- **完成情况**: B-003 统一完成，followerCount/followingCount 同步

### B-007: 点赞/取消点赞接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE (合并至 B-003)
- **目标**: POST/DELETE /api/notes/{id}/like
- **完成情况**: B-003 统一完成，likeCount 同步更新

### B-008: 评论接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE (合并至 B-003)
- **目标**: GET/POST /api/notes/{id}/comments
- **完成情况**: B-003 统一完成，commentCount 同步更新

### B-009: 搜索接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P2
- **status**: ✅ DONE (合并至 B-003)
- **目标**: GET /api/notes/search?keyword=&page=&size=
- **完成情况**: B-003 统一完成，LIKE 模糊匹配 title + content

### B-010: 文件上传接口
- **owner**: BACKEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE (合并至 B-003)
- **目标**: POST /api/upload
- **完成情况**: B-003 统一完成，类型校验+大小限制+UUID 文件名

---

## 前端任务 (F-)

### F-001: Android 项目骨架搭建
- **owner**: FRONTEND
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: Compose + Hilt + Navigation + Retrofit 网络层 + DataStore + 底部导航框架
- **完成情况**: 40 个 Kotlin 文件，MVVM 架构，feature-based 目录结构

### F-002: 登录/注册页面
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 登录页 + 注册页 + 表单验证 + 调用 API + Token 存储
- **完成情况**: 8 个新文件（AuthApi/AuthRepository/AuthModels/LoginScreen/RegisterScreen/LoginVM/RegisterVM/MainViewModel），路由保护机制实现

### F-003: 底部导航 + 主框架
- **owner**: FRONTEND
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 底部导航（首页/发布/我的）+ Navigation 路由
- **完成情况**: 8 路由 NavHost + BottomNavBar，路由保护机制（未登录跳转登录页）

### F-004: 首页笔记流列表
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 图文卡片列表 + 下拉刷新 + 上拉分页 + loading/empty/error 状态
- **完成情况**: FeedListScreen + FeedListViewModel，Coil 图片加载，分页加载

### F-005: 笔记详情页
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 图片轮播 + 标题正文 + 作者信息 + 点赞 + 评论列表 + 评论输入
- **完成情况**: NoteDetailScreen + NoteDetailViewModel，点赞/评论功能完整

### F-006: 发布笔记页
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE
- **目标**: 标题、正文、选图（最多 3 张）、上传、提交
- **验收标准**: 发布后跳转首页，新笔记出现在列表顶部
- **完成情况**: PublishScreen + PublishViewModel + PublishRepository 已实现；图片上传前校验 5MB；上传返回 `/uploads/**` 后提交笔记；帖子卡片/详情页通过 `resolveMediaUrl` 正常加载图片

### F-007: 搜索页
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P2
- **status**: ✅ DONE
- **目标**: 搜索框 + 结果列表
- **验收标准**: 输入关键词可搜索，结果正常展示
- **完成情况**: SearchScreen + SearchViewModel 已实现，含分页加载、错误重试、空状态提示
- **质量修复**: 分页索引对齐(0→1)、空列表 loadMore 守卫

### F-008: 我的/他人资料页
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE
- **目标**: 头像昵称简介、关注/粉丝数、关注按钮、Ta 的笔记列表
- **验收标准**: 资料正常展示，关注操作生效
- **完成情况**: ProfileScreen + ProfileViewModel 已实现，含笔记列表分页、关注/取关、退出登录
- **质量修复**: 分页索引对齐(0→1)、个人页请求数优化(2→1)、后端 noteCount 字段补齐

### F-009: 编辑资料页
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE
- **目标**: 改昵称/头像/简介
- **验收标准**: 编辑后资料页刷新显示新数据
- **完成情况**: EditProfileScreen + EditProfileViewModel 已实现，含头像 PhotoPicker、保存成功回调
- **质量修复**: bio 输入框 maxLines 限制、后端 updateProfile 补齐 followerCount/followingCount/noteCount 返回

### F-010: 关注/粉丝（简化）
- **owner**: FRONTEND
- **reviewer**: REVIEWER
- **priority**: P2
- **status**: TODO
- **目标**: 简化版关注/粉丝列表展示
- **依赖**: F-008
- **验收标准**: 可查看关注/粉丝用户列表

---

## 测试任务 (R-)

### R-001: 验收标准与高风险清单
- **owner**: REVIEWER
- **reviewer**: ARCHITECT
- **priority**: P0
- **status**: ✅ DONE
- **目标**: 制定全项目验收标准、识别高风险点
- **输出**: 验收标准文档 + 高风险清单 + 质量门禁建议
- **完成情况**: P0/P1/P2 验收条件、安全/数据/边界风险清单、API 格式冻结建议

### R-002: 后端接口测试
- **owner**: REVIEWER
- **reviewer**: ARCHITECT
- **priority**: P1
- **status**: ✅ DONE（ARCHITECT 代执行）
- **目标**: 17 个 API 接口的功能测试、鉴权测试、异常测试
- **依赖**: B-001~B-010
- **完成情况**: 使用 H2 dev profile 启动后端，curl 测试全部 16 个端点（不含 GET /api/users/{id}/notes），全部返回预期结果

### R-003: 前端 UI 测试
- **owner**: REVIEWER
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: TODO
- **目标**: Compose UI 测试、ViewModel 单元测试、关键流程集成测试
- **依赖**: F-001~F-010

---

## 缺陷任务 (BUG-)

### BUG-001: 小图上传返回文件上传失败
- **owner**: ARCHITECT
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE
- **现象**: 小于 5MB 图片上传后返回 `40603 文件上传失败`
- **原因**: 8080 运行的是旧后端进程；旧 `MultipartFile.transferTo(File)` 保存路径在当前环境不稳定
- **修复**: `FileService` 改为绝对上传目录 + `Files.copy`；上传扩展名由 MIME 类型映射；重启后端后 curl 验证上传成功

### BUG-002: 图片上传成功但帖子中不显示图片
- **owner**: ARCHITECT
- **reviewer**: REVIEWER
- **priority**: P1
- **status**: ✅ DONE
- **现象**: 上传返回 `/uploads/20260601/uuid.jpg`，发布后列表/详情无图
- **原因**: Android 端直接把相对路径传给 Coil，未拼接后端 `BASE_URL`
- **修复**: 新增 `core/network/MediaUrl.kt` 的 `resolveMediaUrl`，统一将 `/uploads/**` 补全为完整 URL；帖子图、头像、评论头像均接入

### BUG-003: 超过 5MB 图片触发 HTTP 500
- **owner**: ARCHITECT
- **reviewer**: REVIEWER
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 大图 multipart 请求在进入 Controller 前被 Spring 拦截并落入兜底 500
- **修复**: `GlobalExceptionHandler` 增加 `MaxUploadSizeExceededException`、`MultipartException`、`MissingServletRequestPartException` 处理；Android 上传前先检查 5MB

### BUG-004: 前端分页索引不一致导致首页数据重复
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P1
- **status**: ✅ DONE
- **现象**: 前端分页从 page=0 开始，后端从 page=1 开始，page=0 被 clamp 到 1 导致重复加载
- **修复**: 所有前端 ViewModel（FeedList/NoteDetail/Profile/Search）和 API 接口（NoteApi/UserApi）分页默认值从 0 改为 1，涉及 6 个文件

### BUG-005: 登录错误码映射错误
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P1
- **status**: ✅ DONE
- **现象**: 登录失败时用户看到原始后端错误信息而非"用户名或密码错误"
- **原因**: LoginViewModel 用 `ErrorCode.INVALID_PARAMETER`(-2) 比对，后端返回 `LOGIN_FAILED`(40020)
- **修复**: LoginViewModel 改为 `ErrorCode.LOGIN_FAILED`

### BUG-006: UserResponse 缺少 noteCount 字段
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P1
- **status**: ✅ DONE
- **现象**: 个人主页"笔记数"永远显示 0
- **原因**: 后端 UserResponse 无 noteCount 字段
- **修复**: 后端 UserResponse 添加 noteCount 字段，UserService 三个方法均查询笔记数并填充

### BUG-007: 评论发送失败时输入文字丢失
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 点击发送后立即清空输入框，若网络失败文字无法恢复
- **修复**: NoteDetailViewModel 新增 commentSendSuccess 事件流，NoteDetailScreen 仅在成功后清空

### BUG-008: createdAt 显示原始 ISO 时间戳
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 用户看到 "2024-01-15T10:30:00" 而非友好格式
- **修复**: 新增 `core/common/DateTimeUtil.kt`，显示"刚刚/X分钟前/X小时前/X天前/yyyy-MM-dd HH:mm"

### BUG-009: TokenInterceptor runBlocking 潜在 ANR
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 每次 HTTP 请求都通过 runBlocking 读取 DataStore，高并发下可能 ANR
- **修复**: 添加 @Volatile 内存缓存，首次读取后缓存；登录时 updateCachedToken、登出时 invalidateCache

### BUG-010: NoteDetailViewModel 对 ProfileRepository 耦合依赖
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 笔记详情页注入了包含文件上传功能的完整 ProfileRepository，仅用来做关注操作
- **修复**: 改为直接注入 UserApi，内联关注/取关逻辑

### BUG-011: RegisterRequest 无 nickname 字段
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 注册时客户端无法指定昵称，始终默认为用户名
- **修复**: RegisterRequest 添加可选 nickname 字段，AuthService 优先使用

### BUG-012: uploadImage 返回类型混用
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: withContext 块返回 Result.Error / MultipartBody.Part / null 三种类型，逻辑脆弱
- **修复**: PublishRepository 和 ProfileRepository 重构为顺序执行，每步提前返回错误

### BUG-013: 个人页查看他人资料多发 getMyProfile 请求
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 查看任何用户资料都需要两个网络请求来判断 isMyProfile
- **修复**: ProfileViewModel 条件化请求——我的主页只发 getMyProfile，他人主页只发 getUserProfile

### BUG-014: 搜索页空列表触发无效 loadMore
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P2
- **status**: ✅ DONE
- **现象**: 搜索结果为空时 snapshotFlow 仍触发 loadMore
- **修复**: SearchScreen 添加 totalItemsNumber > 0 守卫

### BUG-015: 代码风格与废弃 API
- **owner**: ARCHITECT
- **reviewer**: ARCHITECT
- **priority**: P3
- **status**: ✅ DONE
- **修复项**: Divider→HorizontalDivider、删除 NoteCard 未用 painterResource import、EditProfileScreen bio 添加 maxLines、JWT secret 注释修正

---

## Sprint 1 ✅ 已完成

Sprint 1 全部任务已完成：
- 后端：骨架(A-002/B-001) + 注册登录(B-002) + 全部 17 个 API(B-003) + 验收标准(R-001)
- 前端：骨架(F-001) + 登录注册(F-002) + 底部导航(F-003) + 首页列表(F-004) + 详情页(F-005)

## Sprint 2 执行顺序

```
F-006 (发布笔记)    ✅ DONE
F-008 (资料页)      ✅ DONE
F-007 (搜索页)      ✅ DONE
F-009 (编辑资料)    ✅ DONE
    ↓
R-002 (后端测试)    ✅ DONE（16/16 API 通过）
    ↓
Sprint 2 质量检测   ✅ DONE（16 项 Bug 修复）
    ↓
R-003 (前端测试)    TODO（可选）
    ↓
课设演示与报告撰写
```
