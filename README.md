# 随记 (NoteShare)

这是一个基于 Android 和 Spring Boot 的笔记分享平台，主要作为**普通课程设计**项目进行开发。
本项目在满足现代开发标准的同时，力求代码基础、逻辑清晰、通俗易懂。该项目实现了用户注册登录、笔记发布、图文展示、互动社交（点赞、评论、关注）以及内容搜索等核心功能。

## 项目结构

本项目包含两个主要部分：

1. **app (Android 客户端)**
   - 开发语言：Kotlin
   - UI 框架：基于 Jetpack Compose 搭建的现代 UI 界面。
   - 架构模式：基础的 MVVM 分层架构。
   - 网络请求：Retrofit + OkHttp。
   - 图片加载：Coil。
   - 数据存储：DataStore（用于保存本地登录状态）。

2. **noteshare-server (后端服务端)**
   - 开发框架：Java 17 + Spring Boot 3.x。
   - 数据库：MySQL 8（使用 Spring Data JPA 进行数据交互）。
   - 安全机制：基础的 Spring Security 单链拦截与 JWT 鉴权。
   - 图片存储：本地文件系统上传与读取。
   - 架构模式：传统的 Controller-Service-Dao 三层架构。

## 核心功能

- **账号体系**：支持用户注册、登录、修改个人资料（昵称、头像、简介）。
- **个人资料**：展示关注数、粉丝数和笔记数，支持刷新后同步最新统计与个人笔记列表。
- **笔记流**：首页分页展示最新笔记，支持拉取刷新和下拉加载。
- **发布笔记**：支持标题、正文及最多上传 3 张图片的笔记发布。
- **互动社交**：
  - 对笔记点赞和取消点赞。
  - 查看和发表笔记评论（支持楼中楼二级回复）。
  - 用户之间的关注和取消关注机制。
- **搜索功能**：通过关键词模糊匹配笔记的标题或正文。
- **通知系统**：点赞、评论等互动行为生成通知，每 5 秒轮询未读数刷新红点，支持通知列表查看与一键已读。

## 运行与部署指南

### 后端 (noteshare-server)

1. 环境要求：JDK 17、MySQL 8、Maven。
2. 数据库配置：
   - 创建名为 `noteshare` 的数据库。
   - 设置环境变量 `DB_PASSWORD` 为你的 MySQL 密码（或在 `application-local.yml` 中配置）。
   - 设置环境变量 `JWT_SECRET` 为一个安全的密钥（至少 64 字节）。
3. 运行项目：
   - 根目录下执行 `./mvnw spring-boot:run`。
   - 或者使用 IDEA / Eclipse 直接运行主类 `NoteShareApplication`。
   - 服务默认运行在 `http://localhost:8200`。

### 客户端 (app)

1. 环境要求：Android Studio、Android SDK。
2. 配置网络：
   - 默认 `BASE_URL` 为 `http://127.0.0.1:8200/`（在 `app/build.gradle.kts` 中定义）。
   - 如需修改，在项目根目录 `gradle.properties` 中添加：`noteshareBaseUrl=http://你的IP:8200/`
   - 确保手机/模拟器与后端服务处于同一局域网内。注意：不要使用 `localhost` 或 `127.0.0.1`，模拟器可以使用 `10.0.2.2`。
3. 运行应用：在 Android Studio 中点击 Run，部署到手机或模拟器上。

## 项目改进记录

### 安全优化
- HTTP 请求日志仅在 Debug 构建中输出，Release 构建不再记录敏感数据。
- 移除 Android 端明文流量许可，强制使用 HTTPS。
- 服务端 JWT 密钥和数据库密码不再硬编码默认值，必须通过环境变量配置。

### 代码质量
- 统一使用自定义 `Result` 类型，移除无用的 `Loading` 状态。
- 提取 `safeApiCall` / `safeApiCallUnit` 工具函数，消除 Repository 层重复的 try/catch 样板代码。
- 提取 `uploadFile` / `uploadVideo` 公共工具，消除文件上传逻辑重复。
- 修复 `CommentService.listReplies` 的 N+1 查询问题，改用批量加载。
- 修复 `MainViewModel` 轮询协程的生命周期管理（登出取消、防止重复创建）。
- 补充客户端缺失的 `ErrorCode` 常量，`RegisterViewModel` 根据错误码显示友好提示。
- 修复 JPQL 搜索的通配符注入问题。

## 注意事项

- 项目中上传的图片保存在后端的本地 `uploads` 文件夹中，如果清理了该文件夹，对应的图片资源将无法访问。
- 本项目为我的课程设计项目。
