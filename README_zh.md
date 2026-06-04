# koim — 极简即时通讯系统（Spring Boot + Vue 3）

一个基于 Kotlin / Spring Boot 后端（仅依赖 PostgreSQL）和 Vue 3 / Vite 前端的极简 IM 系统。
支持注册 / 登录、单聊、群聊、WebSocket 实时推送、离线消息同步、未读消息提醒。

> English version: [README.md](README.md)

---

## 1. 技术栈

### 后端
- Kotlin 2.x · Spring Boot **3.3.5** · Java 17
- Spring Web、Spring WebSocket、Spring Data JPA、Spring Security 6
- PostgreSQL 14+（运行时）、H2 PostgreSQL 兼容模式（测试）
- JJWT 0.12.6 — 无状态 JWT 鉴权
- Hibernate 6.5（`ddl-auto: update` 自动建表）
- BCrypt 密码哈希

### 前端
- Vue **3.5** + Vite **5.4**（不使用 TypeScript）
- Pinia（状态管理）· Vue Router · axios · @vueuse/core
- `emoji-picker-element`（Web Component，支持 emoji 输入）

---

## 2. 目录结构

```
koim/
├── build.gradle.kts                # Gradle KTS 构建脚本
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt
│   │   │   ├── config/             # WebSocketConfig
│   │   │   ├── controller/         # 用户 / 消息 / 会话 / 群组 / 全局异常处理
│   │   │   ├── dto/Dtos.kt         # 请求 / 响应 DTO + ApiResponse 包装
│   │   │   ├── entity/             # User、ChatGroup、GroupMember、Message、
│   │   │   │                       # OfflineMessage、Conversation
│   │   │   ├── repository/         # 6 个 JpaRepository
│   │   │   ├── security/           # JwtService、JwtAuthenticationFilter、
│   │   │   │                       # SecurityConfig、CurrentUser
│   │   │   ├── service/            # UserService、MessageService、
│   │   │   │                       # ConversationService、GroupService
│   │   │   └── ws/                 # OnlineUserManager、ChatWebSocketHandler
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── kotlin/ImEndToEndTest.kt
│       └── resources/application-test.yml   # H2 PostgreSQL 兼容模式
└── front_end/
    ├── package.json · vite.config.js · index.html
    └── src/
        ├── api/         # client.js（JWT 拦截 + ApiResponse 解包）
        │                # user / message / conversation / group
        ├── stores/      # auth · messages · conversation · websocket
        ├── router/index.js
        ├── views/       # Login.vue · Register.vue · Chat.vue
        ├── components/  # ConversationList · ChatBox · MessageBubble · GroupPanel
        ├── App.vue · main.js · style.css
        └── assets/
```

---

## 3. 数据模型

| 表名               | 用途                                                                    |
| ------------------ | ----------------------------------------------------------------------- |
| `users`            | uid（主键）、username（唯一）、password（bcrypt）、nickname            |
| `chat_groups`      | group_id（主键）、name、owner_uid（避开 SQL 关键字 `groups`）          |
| `group_members`    | 联合主键 (group_id, uid)、join_time                                    |
| `messages`         | seq（IDENTITY 主键）、msg_id（唯一幂等键）、from_uid、to_uid、group_id、type、content、send_time |
| `offline_messages` | id（主键）、to_uid、msg_id、send_time —— 离线投递队列                  |
| `conversations`    | 联合主键 (uid, peer_type, peer_id)、last_msg_*、unread_count           |

设计要点：
- `seq` 使用 IDENTITY 列，PostgreSQL 与 H2 均可工作；用于消息全局排序与基于 `lastSeq` 的离线同步。
- `msg_id` 由客户端生成（建议 UUID），数据库唯一约束保证 **幂等发送**。
- `peer_type` 取值 `'u'`（单聊）或 `'g'`（群聊）。

---

## 4. REST 接口

所有响应统一格式：`{ ok: bool, data, error }`。
除 `/api/user/register`、`/api/user/login`、`/ws/**` 外都需要 `Authorization: Bearer <jwt>`。

### 用户
- `POST /api/user/register` → `{ uid, token }`
- `POST /api/user/login` → `{ uid, token }`

### 消息
- `POST /api/message/send` `{ msgId, toUid?, groupId?, type, content }` → `{ seq, sendTime }`
  幂等：相同 `msgId` 重复请求返回首次记录。
- `POST /api/message/sync` `{ lastSeq }` → `{ messages: MessageDto[] }`
  拉取离线消息（seq > lastSeq）。
- `POST /api/message/confirm` `{ msgIds }` → 从 `offline_messages` 删除已确认消息。

### 会话
- `GET  /api/conversation/list` → `ConversationDto[]`
- `POST /api/conversation/read` `{ peerType, peerId }` → 清除未读数

### 群组
- `POST /api/group/create` `{ name, memberUids: [] }` → `{ groupId }`
- `POST /api/group/join` `{ groupId }` → `{ name, memberCount }`
- `GET  /api/group/{id}` → 群信息

### WebSocket
- `ws://<host>:8080/ws/chat?token=<jwt>`
  服务端推送：`{ type: 'msg', seq, msgId, fromUid, toUid?, groupId?, content, sendTime }`

---

## 5. 消息流程

```
客户端 A ── POST /message/send ──▶ MessageService
                                   │
                                   ├─ 单聊
                                   │   ├─ B 在线？  WebSocket 推送给 B
                                   │   └─ B 离线？  插入 offline_messages(B)
                                   │
                                   └─ 群聊
                                       └─ 对每个 ≠ 发送者 的成员：
                                           在线？WS 推送   离线？插入 offline_messages
```

客户端在登录 / 重连后：
1. 调用 `/message/sync` 携带本地保存的 `lastSeq`，拉取离线消息；
2. 调用 `/message/confirm` 把已收到的 `msgIds` 从离线队列中删除。

---

## 6. 本地启动

### 6.1 PostgreSQL
```bash
psql -d postgres -c "CREATE ROLE koim WITH LOGIN PASSWORD 'koim';"
psql -d postgres -c "CREATE DATABASE koim OWNER koim;"
```

### 6.2 后端
```bash
./gradlew bootRun        # 启动 Tomcat 监听 8080，Hibernate 自动建表
./gradlew test           # 运行 ImEndToEndTest（5 个用例全部通过）
```

### 6.3 前端
```bash
cd front_end
npm install --legacy-peer-deps     # peer 依赖差异需要 legacy 解析
npm run dev                        # http://localhost:5173（同时开放 LAN 访问）
npm run build
```

前端会自动从 `window.location.hostname` 推导后端地址，因此手机在同 Wi-Fi
下访问 `http://<Mac 的局域网 IP>:5173` 无需任何额外配置。

---

## 7. 前端架构要点

### Pinia 状态管理
- `auth` — token + uid，持久化到 localStorage。
- `messages` — `Map<peerKey, MessageDto[]>`，提供 `append` 与 `updateByMsgId`。
  > 注意：直接修改原始对象引用 **不会触发响应式更新**（因为 store 中保存的是 Proxy 包装版本）。必须通过 `updateByMsgId` 这类 action 走 Proxy 才能触发渲染。
- `conversation` — list / active / `upsertFromMessage` / `markActiveRead` /
  `openLocal`（在尚未产生消息时也能创建本地会话条目，立刻打开输入框）。
- `websocket` — connect / disconnect / handler 派发，断线后每 3 秒自动重连。

### 乐观发送
1. 构造 `localMsg`（`pending: true`），追加到消息 store。
2. POST `/message/send`。
3. 成功 → `updateByMsgId` 把 `pending` 置 `false`，回填 `seq` / `sendTime`。
4. 失败 → 标记 `failed: true`。

### 动态后端地址
`api/client.js` 与 `stores/websocket.js` 都从 `window.location.hostname` 推导
base URL，因此同一个产物可以同时支持 `localhost` 与任意局域网 IP，无需环境变量。

### Emoji 输入
`ChatBox.vue` 集成 `emoji-picker-element`：
- `vite.config.js` 中将 `<emoji-picker>` 注册为 Web Component（`isCustomElement`）。
- 通过原生 `addEventListener('emoji-click', ...)` 监听选择事件。
- 在 textarea 的当前光标位置插入 emoji，并把光标移动到 emoji 之后。

---

## 8. 验收测试

`src/test/kotlin/ImEndToEndTest.kt` 覆盖 5 个用例（全部通过）：

1. 单聊 — 离线入库 / 上线同步 / 确认删除。
2. 幂等发送 — 相同 `msgId` 重复请求返回首次记录。
3. 会话列表 / 未读数 / 标记已读。
4. 群聊 — 消息派发到除发送者外的所有成员。
5. 未携带 token 的请求返回 401。

执行：`./gradlew test`。

---

## 9. 配置项

`src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/koim
    username: koim
    password: koim
koim:
  jwt:
    secret: <长度 ≥ 32 字节的密钥>
    expire-minutes: 1440
  ws:
    path: /ws/chat
```

CORS 默认开放所有来源（见 [SecurityConfig](src/main/kotlin/security/SecurityConfig.kt)），便于本地开发。

---

## 10. 已知限制 / 后续扩展

- **单实例部署**：`OnlineUserManager` 为内存 Map；多节点部署需引入 Redis 发布订阅或会话注册表。
- **无历史消息漫游**：`/message/sync` 只返回离线队列；如需向上滚动加载需新增 `/message/history?peerType&peerId&beforeSeq` 接口。
- **不支持文件 / 图片消息**：后端 `content` 字段仅存文本。
- **未读统计粒度有限**：仅会话级未读数，无逐条已读回执。

---

## 11. 常见问题

**Q: 启动报 `FATAL: role "koim" does not exist`？**
A: 还没创建 PostgreSQL 角色 / 数据库，参见 6.1 节的两条 SQL。

**Q: 手机访问注册时报"网络错误"？**
A: 确保用电脑的局域网 IP 而不是 `localhost` 访问；macOS 防火墙需放行 5173 / 8080；
路由器若开启了 AP isolation 也会导致同 Wi-Fi 设备无法互通。

**Q: 发送消息后 "sending…" 不变成时间戳？**
A: 这是 Vue 3 响应式陷阱，已通过 `messages.js` 的 `updateByMsgId` 修复。
若仍出现，请硬刷新浏览器（Cmd+Shift+R）清除 Pinia HMR 缓存。

**Q: `npm install` 报 ERESOLVE 冲突？**
A: 加上 `--legacy-peer-deps`；如果还报 EACCES，通过 `--cache /tmp/npm-cache-koim`
绕开 `~/.npm` 的权限问题。
