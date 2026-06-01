# 随记 NoteShare 项目总控面板

> 最后更新：ARCHITECT | 阶段 5 编码协作 Sprint 2 全面质量检测与修复完成

---

## 当前开发阶段
**阶段 0 ✅** → **阶段 1 ✅** → **阶段 2 ✅** → **阶段 3 ✅** → **阶段 4 ✅** → **阶段 5** 🔄（编码协作）

## 当前版本目标
**Sprint 2**：发布笔记、搜索、个人资料编辑、关注/粉丝（Sprint 1 已完成）

### 设计产出文档清单
| 文档 | 状态 | 说明 |
|------|------|------|
| `docs/architecture-design-v1.md` | ✅ | 综合架构设计方案 v1（Android + 后端 + API，11 章节） |
| `docs/task-board.md` | ✅ | 任务板（含 Sprint 1 执行顺序图） |

---

## 已确认需求（MVP 范围）

| 模块 | 功能 | Sprint | 状态 |
|------|------|--------|------|
| 账号体系 | 注册、登录（JWT）、查看/编辑个人资料（昵称、头像、简介） | Sprint 1 | ✅ 后端完成，前端完成（登录/注册） |
| 笔记流 | 首页"最新笔记"分页列表（图文卡片）、笔记详情页 | Sprint 1 | ✅ 后端完成，前端完成 |
| 发布笔记 | 标题 + 正文 + 图片上传（最多 3 张） | Sprint 2 | ✅ 后端完成，✅ 前端完成（图片上传/显示已联调） |
| 社交互动 | 点赞/取消点赞、单层评论、关注/取消关注、粉丝数 | Sprint 1-2 | ✅ 后端完成，✅ 前端完成 |
| 搜索 | 按关键词模糊匹配笔记标题/正文 | Sprint 2 | ✅ 后端完成，✅ 前端完成 |

## 暂缓需求（非目标，明确不做）

- 消息/系统通知、私信 IM
- 收藏、转发、@提及、话题标签体系
- 个人主页聚合 feed、个性化推荐算法
- 楼中楼评论、表情、富文本编辑器
- 云对象存储、CDN、图片压缩流水线
- 管理员后台、内容审核、举报
- 多语言、深浅色主题切换

---

## 技术栈决策

### Android 客户端
- **语言**：Kotlin
- **UI**：Jetpack Compose + Material Design 3
- **架构**：MVVM + ViewModel + StateFlow（UDF 单向数据流）
- **网络**：Retrofit + OkHttp + Kotlinx Serialization
- **图片**：Coil
- **DI**：Hilt
- **导航**：Navigation Compose
- **存储**：DataStore（JWT + 登录态）
- **构建**：Gradle Kotlin DSL

### 后端服务
- **语言**：Java 17
- **框架**：Spring Boot 3.x
- **安全**：Spring Security + JWT（jjwt）
- **数据**：Spring Data JPA
- **数据库**：MySQL 8（开发期可用 H2 内存库）
- **文件**：本地磁盘目录 + 静态资源映射（`/uploads/**`）
- **分页**：Spring Data Pageable

---

## 架构决策

### Android 端
- 单模块，feature-based 目录结构
- UDF 单向数据流：ViewModel → Repository → Retrofit API → UI State
- 每个 feature 含 data/domain/presentation 三层
- 统一 `ApiResponse<T>` 解包，错误码映射到用户可读消息
- Token 注入通过 OkHttp Interceptor

### 后端
- 单模块，分层架构（Controller → Service → Repository）
- JPA Entity 6 张表：User, Note, NoteImage, LikeRel, Comment, Follow
- 统一响应包装：`{code, message, data, request_id}`
- 全局异常处理：@RestControllerAdvice + ErrorCode 枚举
- BCrypt 密码加密，JWT 无状态鉴权

---

## 数据库决策

| 表名 | 核心字段 | 关系 | 特殊说明 |
|------|----------|------|----------|
| User | id, username[uniq], password_hash, nickname, avatar_url, bio, created_at | - | username 唯一索引 |
| Note | id, author_id→User, title, content, like_count, comment_count, created_at | N:1 User | like_count/comment_count 冗余计数 |
| NoteImage | id, note_id→Note, url, sort | N:1 Note | 每篇最多 3 张 |
| LikeRel | id, user_id→User, note_id→Note, created_at | M:N | uniq(user_id, note_id) |
| Comment | id, note_id→Note, user_id→User, content, created_at | N:1 Note, N:1 User | 单层评论 |
| Follow | id, follower_id→User, followee_id→User, created_at | M:N User | uniq(follower_id, followee_id) |

---

## API 决策

统一前缀 `/api`，需登录接口走 `Authorization: Bearer <token>`。

### 鉴权与用户
| 方法 | 路径 | 说明 | Auth |
|------|------|------|------|
| POST | /api/auth/register | 注册 | ❌ |
| POST | /api/auth/login | 登录，返回 token + user | ❌ |
| GET | /api/users/me | 当前用户资料 | ✅ |
| PUT | /api/users/me | 编辑昵称/头像/简介 | ✅ |
| GET | /api/users/{id} | 查看他人资料 | ✅ |
| POST | /api/users/{id}/follow | 关注 | ✅ |
| DELETE | /api/users/{id}/follow | 取消关注 | ✅ |

### 笔记
| 方法 | 路径 | 说明 | Auth |
|------|------|------|------|
| GET | /api/notes?page=&size= | 首页最新笔记（分页） | ❌ |
| GET | /api/notes/search?keyword=&page=&size= | 关键词搜索 | ❌ |
| GET | /api/notes/{id} | 笔记详情 | ✅ |
| POST | /api/notes | 发布笔记 | ✅ |
| DELETE | /api/notes/{id} | 删除自己的笔记 | ✅ |

### 互动
| 方法 | 路径 | 说明 | Auth |
|------|------|------|------|
| POST | /api/notes/{id}/like | 点赞 | ✅ |
| DELETE | /api/notes/{id}/like | 取消点赞 | ✅ |
| GET | /api/notes/{id}/comments?page=&size= | 评论列表 | ❌ |
| POST | /api/notes/{id}/comments | 发表评论 | ✅ |

### 文件
| 方法 | 路径 | 说明 | Auth |
|------|------|------|------|
| POST | /api/upload | 上传单张图片 | ✅ |

---

## 前端任务状态

| task_id | 任务 | owner | status |
|---------|------|-------|--------|
| F-001 | Android 项目骨架搭建 | FRONTEND | ✅ DONE |
| F-002 | 登录/注册页面实现 | FRONTEND | ✅ DONE |
| F-003 | 底部导航 + 主框架 | FRONTEND | ✅ DONE |
| F-004 | 首页笔记流列表 | FRONTEND | ✅ DONE |
| F-005 | 笔记详情页 | FRONTEND | ✅ DONE |
| F-006 | 发布笔记页 | FRONTEND | ✅ DONE |
| F-007 | 搜索页 | FRONTEND | ✅ DONE（质量修复完成） |
| F-008 | 我的/他人资料页 | FRONTEND | ✅ DONE（质量修复完成） |
| F-009 | 编辑资料页 | FRONTEND | ✅ DONE（质量修复完成） |
| F-010 | 关注/粉丝（简化） | FRONTEND | TODO |

## 后端任务状态

| task_id | 任务 | owner | status |
|---------|------|-------|--------|
| B-001 | 后端骨架搭建 | BACKEND | ✅ DONE |
| B-002 | 注册+登录接口 | BACKEND | ✅ DONE |
| B-003 | 全部 API 实现（17 个） | BACKEND | ✅ DONE |
| B-004 | 用户资料接口 | BACKEND | ✅ DONE |

## 测试任务状态

| task_id | 任务 | owner | status |
|---------|------|-------|--------|
| R-001 | 验收标准与高风险清单 | REVIEWER | ✅ DONE |
| R-002 | 后端接口测试 | REVIEWER | ✅ DONE（ARCHITECT 代执行，16/16 API 通过） |
| R-003 | 前端 UI 测试 | REVIEWER | TODO |

## 架构任务状态

| task_id | 任务 | owner | status |
|---------|------|-------|--------|
| A-001 | Android 项目骨架设计 | ARCHITECT→FRONTEND | ✅ DONE |
| A-002 | 后端项目骨架设计 | ARCHITECT→BACKEND | ✅ DONE |
| A-003 | API v1 决策版确认 | ARCHITECT | ✅ DONE |
| INIT-001 | 各 Agent 初始化回执 | ALL | ✅ DONE |

---

## 当前 Blocker

1. 暂无构建阻塞项

## 当前风险

| 等级 | 风险 | 缓解方案 |
|------|------|----------|
| P3 | 图片上传/静态资源联调回归 | 已修复：后端绝对路径保存 + Android 端 `resolveMediaUrl` 补全 `/uploads/**` |

## 已修复缺陷（Sprint 2 质量检测）

| 编号 | 等级 | 问题 | 修复方案 |
|------|------|------|----------|
| BUG-004 | Critical | 前端分页索引 0-based vs 后端 1-based，导致首页数据重复 | 全部前端 ViewModel/API 分页默认值改为 1 |
| BUG-005 | Critical | 登录错误码映射错误，友好提示不显示 | `INVALID_PARAMETER` → `LOGIN_FAILED` |
| BUG-006 | Critical | 后端 UserResponse 缺少 noteCount，个人主页笔记数永远为 0 | 后端添加 noteCount 字段及查询逻辑 |
| BUG-007 | High | 评论发送失败时输入文字丢失 | 改为仅成功后清空输入框 |
| BUG-008 | High | createdAt 显示原始 ISO 时间戳 | 新增 DateTimeUtil 工具类，显示"刚刚/X分钟前" |
| BUG-009 | High | TokenInterceptor runBlocking 潜在 ANR | 添加 @Volatile 内存缓存，登录/登出时刷新 |
| BUG-010 | High | NoteDetailViewModel 对 ProfileRepository 耦合依赖 | 改为直接注入 UserApi |
| BUG-011 | Medium | RegisterRequest 无 nickname 字段 | 添加可选 nickname，注册时优先使用 |
| BUG-012 | Medium | uploadImage 返回类型混用 | 重构为顺序执行，消除类型混乱 |
| BUG-013 | Medium | 个人页查看他人资料多发一次 getMyProfile 请求 | 优化为条件请求 |
| BUG-014 | Medium | 搜索页空列表时触发无效 loadMore | 添加 totalItemsNumber > 0 守卫 |
| BUG-015 | Low | Divider 已废弃、未用 import、bio 无 maxLines、JWT 注释误导 | 逐一修复 |

## 最近一次 MCP 同步记录

| 时间 | 事件 | 详情 |
|------|------|------|
| 阶段 0 启动 | INIT-001 广播 | ARCHITECT 向 FRONTEND/BACKEND/REVIEWER 发送项目启动通知 |
| 阶段 0 完成 | INIT-001 回执 | 三方全部回执确认 |
| 阶段 1 裁决 | ARCHITECT 决策 | ①requestId camelCase ②B-001可先行编码 ③/api/upload需认证 |
| 阶段 2 完成 | 设计文档体系 | 产出综合架构设计文档（architecture-design-v1.md） |
| 阶段 3 完成 | API v1 决策版 | 17 个 API 全部定义完成 |
| 阶段 4 完成 | 任务拆解 | Sprint 1 + Sprint 2 任务卡拆解完成 |
| 阶段 5 Sprint 1 | 编码协作 | 后端 17 个 API 全部实现，前端登录/注册/首页/详情页完成 |
| 阶段 5 Sprint 2 | 编码进行中 | 发布笔记、搜索、资料页开发中 |
| 阶段 5 Sprint 2 联调 | 图片上传修复 | 修复上传 500/40603、超 5MB 异常映射、`/uploads/**` 相对 URL 在 Android 端无法显示的问题 |
| 阶段 5 Sprint 2 质量检测 | 全面代码审查 | ARCHITECT 代执行：后端 16/16 API 测试通过 + 前端代码审查发现 16 项 Bug（3C+4H+4M+5L），全部修复完成 |

## 下一步计划

1. 📋 R-003 前端 UI 测试（可选）
2. 🚀 课设演示与报告撰写
