# Sprint 2 任务规划

> 最后更新：ARCHITECT | Sprint 2 全面质量检测与修复完成

---

## Sprint 2 范围

| 任务 | Agent | 优先级 | 状态 | 说明 |
|------|-------|--------|------|------|
| F-006 | FRONTEND | P1 | ✅ DONE | 发布笔记页（标题+正文+选图+上传+提交） |
| F-007 | FRONTEND | P2 | ✅ DONE | 搜索页（搜索框+结果列表+分页） |
| F-008 | FRONTEND | P1 | ✅ DONE | 我的/他人资料页（头像+昵称+简介+关注+笔记列表） |
| F-009 | FRONTEND | P1 | ✅ DONE | 编辑资料页（改昵称/头像/简介） |
| B-004 | BACKEND | P2 | ✅ DONE | 联调支持 + Bug 修复（noteCount 字段、RegisterRequest nickname、updateProfile 返回值） |
| R-002 | REVIEWER | P1 | ✅ DONE | 后端接口测试（ARCHITECT 代执行，16/16 通过） |
| R-004 | REVIEWER | P1 | ✅ DONE | Sprint 2 功能验收测试（ARCHITECT 代执行，全面代码审查 + 16 项 Bug 修复） |

## 后端 API 已就绪清单

所有 Sprint 2 需要的后端 API 均已在 B-003 中实现：

- POST /api/upload — 文件上传 ✅
- POST /api/notes — 发布笔记 ✅
- GET /api/notes/search — 搜索 ✅
- GET /api/users/{id} — 他人资料 ✅
- PUT /api/users/me — 编辑资料 ✅
- POST/DELETE /api/users/{id}/follow — 关注/取消 ✅

## 已完成联调记录

- 发布笔记图片上传：`POST /api/upload` 需携带 JWT，返回 `/uploads/yyyyMMdd/uuid.ext`
- 静态图片访问：`GET /uploads/**` 公开访问，已验证返回 `HTTP 200 image/jpeg`
- Android 图片显示：新增 `resolveMediaUrl`，将后端相对路径 `/uploads/**` 补全为 `BuildConfig.BASE_URL + path`
- 异常处理：超过 5MB 返回 `40602`，缺少/错误 multipart 返回参数错误，不再落入 HTTP 500

## Sprint 2 质量检测（ARCHITECT 代执行）

### 后端测试
- 编译：✅ 通过
- 单元测试：✅ 4/4 通过
- API 运行测试：✅ 16/16 通过（H2 dev profile）

### 前端代码审查
- 审查范围：全部 42 个 Kotlin 文件
- 发现问题：3 Critical + 4 High + 5 Medium + 5 Low
- 修复状态：✅ 全部修复完成（共 20 个文件修改，1 个新文件）

### 主要修复内容
1. 分页索引统一（0-based → 1-based）
2. 登录错误码映射修正
3. 后端 noteCount 字段补齐
4. 评论发送可靠性改进
5. 日期友好格式化
6. Token 内存缓存优化
7. ViewModel 依赖解耦
8. 图片上传返回类型重构
9. 个人页请求优化
10. 搜索页空列表守卫

## 结论

**Sprint 2 全部 MVP 功能已完成并通过质量检测，可进入课设演示阶段。**
