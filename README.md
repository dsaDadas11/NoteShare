# 随记 (NoteShare)

这是一个基于 Android (Jetpack Compose) 和 Spring Boot 的笔记分享平台，作为课程设计项目开发。该项目实现了用户注册登录、笔记发布、图文展示、互动社交（点赞、评论、关注）以及内容搜索等核心功能。

## 项目结构

本项目包含两个主要部分：

1. **app (Android 客户端)**
   - 采用 Kotlin 语言开发。
   - UI 框架：Jetpack Compose + Material Design 3。
   - 架构模式：MVVM 架构，使用 StateFlow 实现单向数据流。
   - 网络请求：Retrofit + OkHttp。
   - 依赖注入：Hilt。
   - 图片加载：Coil。
   - 局部数据存储：DataStore (用于存储 JWT Token 等登录状态)。

2. **noteshare-server (后端服务端)**
   - 采用 Java 17 和 Spring Boot 3.x 框架。
   - 数据库：MySQL 8 (使用 Spring Data JPA 进行数据访问操作)。
   - 安全框架：Spring Security + JWT (无状态鉴权)。
   - 图片存储：本地磁盘目录，通过静态资源映射供前端访问。
   - 采用标准的三层架构 (Controller, Service, Repository)。

## 核心功能

- **账号体系**：支持用户注册、登录、修改个人资料（昵称、头像、简介）。
- **个人资料**：展示关注数、粉丝数和笔记数，支持刷新后同步最新统计与个人笔记列表。
- **笔记流**：首页分页展示最新笔记，支持拉取刷新和下拉加载。
- **发布笔记**：支持标题、正文及最多上传 3 张图片的笔记发布。
- **互动社交**：
  - 对笔记点赞和取消点赞。
  - 查看和发表笔记评论（单层评论）。
  - 用户之间的关注和取消关注机制。
- **搜索功能**：通过关键词模糊匹配笔记的标题或正文。

## 运行与部署指南

### 后端 (noteshare-server)

1. 环境要求：JDK 17、MySQL 8、Maven。
2. 数据库配置：
   - 创建名为 `noteshare` 的数据库。
   - 在 `src/main/resources/application.yml` 中修改数据库连接参数（用户名、密码）。
3. 运行项目：
   - 根目录下执行 `./mvnw spring-boot:run`。
   - 或者使用 IDEA 直接运行 `NoteShareApplication`。
   - 服务默认运行在 `http://localhost:8080`。

### 客户端 (app)

1. 环境要求：Android Studio (最新版本)、Android SDK。
2. 配置网络：
   - 确保手机/模拟器与后端服务处于同一局域网内。
   - 在 `app/src/main/java/com/example/noteshare/core/di/NetworkModule.kt` 或相关的配置文件中，将 `BASE_URL` 修改为你的电脑的局域网 IP 地址（例如 `http://192.168.x.x:8080/`）。注意：不要使用 `localhost` 或 `127.0.0.1`，模拟器可以使用 `10.0.2.2`。
3. 运行应用：在 Android Studio 中点击 Run，部署到设备或模拟器上。

## 注意事项

- 项目中上传的图片保存在后端的本地文件系统中（如 `uploads` 文件夹），如果后端重启，需要确保该文件夹不会被意外清理。
- 已精简冗余的代码和文档，项目结构清晰，适合作为本专科课程设计的参考。
