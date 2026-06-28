# koim — 极简即时通讯系统

基于 **Kotlin / Spring Boot 3** 后端和 **Vue 3 / Vite** 前端的极简 IM 系统。
支持注册/登录、单聊、群聊、WebSocket 实时推送、离线消息同步、未读消息提醒、
Redis 多实例消息扇出，以及完整的可观测性体系（Prometheus + Grafana + Alertmanager）。

> English version: [INTRODUCTION.md](INTRODUCTION.md)

---

## 界面预览

### 登录

![登录页面](docs/screenshots/login.png)

简洁的登录表单 — 输入用户名和密码登录，或点击 "Create account" 注册新账号。

### 注册

![注册页面](docs/screenshots/register.png)

注册表单包含用户名、昵称和密码。注册成功后自动登录。

### 聊天界面

登录后进入聊天页面。左侧栏显示群组与会话列表，右侧为主聊天区域。

![空白聊天](docs/screenshots/chat-empty.png)

新用户会看到空白的会话列表。你可以：
- **创建群组** — 输入群名称，点击 "Create"
- **加入群组** — 输入群 ID，点击 "Join"
- **发起单聊** — 输入对方 UID，点击 "Open"

### 单聊

![单聊](docs/screenshots/chat-conversation.png)

通过 WebSocket 实时发送消息。侧边栏的绿色圆点表示 WebSocket 连接在线。
支持通过表情按钮输入 emoji。

### 群聊

![群聊](docs/screenshots/chat-group.png)

创建群组、邀请成员，一起聊天。消息通过 WebSocket 推送或离线队列投递给所有群成员。

### 可观测性

后端在 `/actuator/prometheus` 暴露 Prometheus 格式的指标：

![Prometheus 指标](docs/screenshots/actuator-prometheus.png)

健康检查端点 `/actuator/health`：

![健康检查](docs/screenshots/actuator-health.png)

通过监控栈（`ops/docker-compose.monitoring.yml`），可获得预配置了 IM 专属指标的
Grafana 仪表盘。

---

## 技术栈

### 后端

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.x · Java 17 |
| 框架 | Spring Boot **3.3.5** |
| Web | Spring Web、Spring WebSocket |
| 数据 | Spring Data JPA · Hibernate 6.5 · PostgreSQL 14+ |
| 安全 | Spring Security 6 · JJWT 0.12.6 · BCrypt |
| 缓存/发布订阅 | Spring Data Redis |
| 监控 | Spring Boot Actuator · Micrometer · Prometheus registry |
| 测试 | JUnit 5 · Spring Boot Test · H2（PostgreSQL 兼容模式） |

### 前端

| 层级 | 技术 |
|------|------|
| 框架 | Vue **3.5** + Vite **5.4** |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| HTTP | axios |
| Emoji | `emoji-picker-element`（Web Component） |
| 工具库 | @vueuse/core |

### 可观测性

| 组件 | 版本 | 用途 |
|------|------|------|
| Prometheus | v2.55.0 | 指标采集与告警 |
| Grafana | 11.2.0 | 仪表盘与可视化 |
| Alertmanager | v0.27.0 | 告警路由与通知 |

---

## 架构总览

```
┌──────────────┐       WebSocket        ┌──────────────────────┐
│  Vue 3 SPA   │◄──────────────────────►│   Spring Boot 3 App  │
│  (Vite 5173) │    ws://host:8080/ws    │      (Tomcat 8080)   │
│              │       REST + JWT        │                      │
└──────────────┘────────────────────────►│  ┌─ Controller ──┐   │
                                        │  │  User / Msg    │   │
                                        │  │  Conv / Group   │   │
                                        │  └──────┬─────────┘   │
                                        │  ┌──────▼─────────┐   │
                                        │  │    Service      │   │
                                        │  └──────┬─────────┘   │
                                        │  ┌──────▼─────────┐   │
                                        │  │  Repository     │   │
                                        │  └──────┬─────────┘   │
                                        │         │             │
                              ┌─────────┼─────────┼─────────┐   │
                              ▼         ▼         ▼         ▼   │
                          PostgreSQL  Redis    Micrometer      │
                           (数据)    (发布订阅)  (指标)          │
                              │         │             │         │
                              │         │             ▼         │
                              │         │      /actuator/       │
                              │         │      prometheus       │
                              │         │             │         │
                              │    ┌────▼─────────────▼─────┐   │
                              │    │   监控栈               │   │
                              │    │  Prometheus + Grafana │   │
                              │    │  + Alertmanager       │   │
                              │    └───────────────────────┘   │
                              └────────────────────────────────┘
```

---

## 目录结构

```
koim/
├── build.gradle.kts                     # Gradle KTS 构建脚本
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt                  # @SpringBootApplication 入口
│   │   │   ├── controller/
│   │   │   │   ├── UserController.kt
│   │   │   │   ├── MessageController.kt
│   │   │   │   ├── ConversationController.kt
│   │   │   │   ├── GroupController.kt
│   │   │   │   └── GlobalExceptionHandler.kt
│   │   │   ├── dto/
│   │   │   │   └── Dtos.kt             # 请求/响应 DTO + ApiResponse 包装
│   │   │   ├── entity/
│   │   │   │   ├── User.kt
│   │   │   │   ├── ChatGroup.kt
│   │   │   │   ├── GroupMember.kt
│   │   │   │   ├── Message.kt
│   │   │   │   ├── OfflineMessage.kt
│   │   │   │   └── Conversation.kt
│   │   │   ├── repository/              # 6 个 JpaRepository
│   │   │   ├── security/
│   │   │   │   ├── JwtService.kt
│   │   │   │   ├── JwtAuthenticationFilter.kt
│   │   │   │   ├── SecurityConfig.kt
│   │   │   │   └── CurrentUser.kt
│   │   │   ├── service/
│   │   │   │   ├── UserService.kt
│   │   │   │   ├── MessageService.kt
│   │   │   │   ├── ConversationService.kt
│   │   │   │   └── GroupService.kt
│   │   │   ├── websocket/
│   │   │   │   ├── OnlineUserManager.kt          # 接口定义
│   │   │   │   ├── InMemoryOnlineUserManager.kt   # 单实例实现
│   │   │   │   ├── RedisOnlineUserManager.kt       # 多实例实现（发布订阅）
│   │   │   │   ├── ChatWebSocketHandler.kt
│   │   │   │   └── WebSocketConfig.kt
│   │   │   └── metrics/
│   │   │       ├── ImMetrics.kt                   # 领域指标定义
│   │   │       └── OfflineQueueMetrics.kt          # 定时 Gauge 轮询
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── kotlin/ImEndToEndTest.kt
│       └── resources/application-test.yml          # H2 PostgreSQL 兼容模式
├── front_end/
│   ├── package.json · vite.config.js · index.html
│   └── src/
│       ├── api/               # client.js（JWT 拦截 + ApiResponse 解包）
│       │                      # user.js · message.js · conversation.js · group.js
│       ├── stores/            # auth · messages · conversation · websocket
│       ├── router/index.js
│       ├── views/             # Login.vue · Register.vue · Chat.vue
│       ├── components/        # ConversationList · ChatBox · MessageBubble · GroupPanel
│       ├── App.vue · main.js · style.css
│       └── assets/
├── docs/                       # 文档资源
│   └── screenshots/           # 实际运行截图
└── ops/                       # 可观测性栈
    ├── docker-compose.monitoring.yml
    ├── prometheus.yml
    ├── alerts.yml
    ├── alertmanager.yml
    └── grafana/provisioning/  # 数据源 + 仪表盘自动预配置
```

---

## 数据模型

| 表名 | 用途 |
|------|------|
| `users` | uid（主键）、username（唯一）、password（bcrypt）、nickname |
| `chat_groups` | group_id（主键）、name、owner_uid（避开 SQL 关键字 `groups`） |
| `group_members` | 联合主键 (group_id, uid)、join_time |
| `messages` | seq（IDENTITY 主键）、msg_id（唯一幂等键）、from_uid、to_uid、group_id、type、content、send_time |
| `offline_messages` | id（主键）、to_uid、msg_id、send_time —— 离线投递队列 |
| `conversations` | 联合主键 (uid, peer_type, peer_id)、last_msg_*、unread_count |

设计要点：

- **`seq`** — IDENTITY 列，PostgreSQL 与 H2 均可工作；用于消息全局排序与基于 `lastSeq` 的离线同步。
- **`msg_id`** — 客户端生成（建议 UUID），数据库唯一约束保证**幂等发送**。
- **`peer_type`** — 取值 `'u'`（单聊）或 `'g'`（群聊）。

---

## REST 接口

所有响应统一格式：`{ ok: bool, data, error }`。
除 `/api/user/register`、`/api/user/login`、`/ws/**` 外都需要 `Authorization: Bearer <jwt>`。

### 用户

| 方法 | 端点 | 请求体 | 响应 |
|------|------|--------|------|
| POST | `/api/user/register` | `{ username, password, nickname }` | `{ uid, token }` |
| POST | `/api/user/login` | `{ username, password }` | `{ uid, token }` |

### 消息

| 方法 | 端点 | 请求体 | 响应 |
|------|------|--------|------|
| POST | `/api/message/send` | `{ msgId, toUid?, groupId?, type, content }` | `{ seq, sendTime }` |
| POST | `/api/message/sync` | `{ lastSeq }` | `{ messages: MessageDto[] }` |
| POST | `/api/message/confirm` | `{ msgIds }` | — |

> `/message/send` 幂等：相同 `msgId` 重复请求返回首次记录。
> `/message/sync` 拉取离线消息（seq > lastSeq）。
> `/message/confirm` 把已确认的 `msgIds` 从离线队列中删除。

### 会话

| 方法 | 端点 | 请求体 | 响应 |
|------|------|--------|------|
| GET | `/api/conversation/list` | — | `ConversationDto[]` |
| POST | `/api/conversation/read` | `{ peerType, peerId }` | 清除未读数 |

### 群组

| 方法 | 端点 | 请求体 | 响应 |
|------|------|--------|------|
| POST | `/api/group/create` | `{ name, memberUids: [] }` | `{ groupId }` |
| POST | `/api/group/join` | `{ groupId }` | `{ name, memberCount }` |
| GET | `/api/group/{id}` | — | 群信息 |

### WebSocket

连接：`ws://<host>:8080/ws/chat?token=<jwt>`

服务端推送：
```json
{ "type": "msg", "seq", "msgId", "fromUid", "toUid?", "groupId?", "content", "sendTime" }
```

---

## 消息流程

```
客户端 A ── POST /message/send ──▶ MessageService
                                    │
                                    ├─ 单聊
                                    │   ├─ B 在线？ ──▶ WebSocket 推送给 B
                                    │   └─ B 离线？──▶ INSERT offline_messages(B)
                                    │
                                    └─ 群聊
                                        └─ 对每个 ≠ 发送者 的成员：
                                            在线？──▶ WS 推送
                                            离线？──▶ INSERT offline_messages
```

客户端登录/重连后：
1. 调用 `/message/sync` 携带本地保存的 `lastSeq`，拉取离线消息。
2. 调用 `/message/confirm` 把已收到的 `msgIds` 从离线队列中删除。

---

## 多实例支持

`OnlineUserManager` 接口提供两种实现，通过 `application.yml` 中的
`koim.online-manager` 切换：

| 模式 | 实现类 | 机制 | 适用场景 |
|------|--------|------|----------|
| `memory` | `InMemoryOnlineUserManager` | `ConcurrentHashMap` | 单实例部署 |
| `redis` | `RedisOnlineUserManager` | Redis SET (`koim:online`) + 发布订阅 (`koim:fanout`) | 多实例部署 |

`redis` 模式工作方式：
- **在线状态**通过 Redis SET 共享，所有节点可见。
- **跨节点投递**使用 Redis 发布订阅：若目标用户未连接到本节点，
  消息会发布到 `koim:fanout` 频道；所有节点收到后，持有目标用户
  WebSocket 连接的节点负责本地投递。

---

## 可观测性

### 领域指标

所有 IM 领域指标定义在 [ImMetrics](src/main/kotlin/metrics/ImMetrics.kt)，
通过 `/actuator/prometheus` 暴露：

![Prometheus 指标](docs/screenshots/actuator-prometheus.png)

| 指标 | 类型 | 描述 |
|------|------|------|
| `koim_ws_sessions_active` | Gauge | 当前进程持有的 WebSocket 会话数 |
| `koim_offline_queue_size` | Gauge | `offline_messages` 表行数（每 30 秒轮询） |
| `koim_ws_connect_total` | Counter | WebSocket 连接接受总数 |
| `koim_ws_disconnect_total` | Counter | WebSocket 连接关闭总数 |
| `koim_message_sent_total` | Counter（标签：`peerType`、`result`） | MessageService 处理的消息数 |
| `koim_message_delivery` | Timer（p50、p95、p99） | MessageService.send 端到端延迟 |
| `koim_redis_fanout_published_total` | Counter | 发布到 Redis 发布订阅的跨节点消息数 |
| `koim_redis_fanout_received_total` | Counter | 从 Redis 发布订阅收到的跨节点消息数 |

### 健康检查

![健康检查](docs/screenshots/actuator-health.png)

### 监控栈

位于 `ops/`，通过 Docker Compose 拉起 Prometheus + Grafana + Alertmanager：

```bash
cd ops
docker compose -f docker-compose.monitoring.yml up -d
```

| 服务 | URL | 凭据 |
|------|-----|------|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Alertmanager | http://localhost:9093 | — |

Grafana 中的 **koim → IM Overview** 仪表盘会自动预配置。
详见 [ops/README.md](ops/README.md)。

---

## 前端架构

### Pinia 状态管理

| Store | 职责 |
|-------|------|
| `auth` | token + uid，持久化到 localStorage |
| `messages` | `Map<peerKey, MessageDto[]>`，提供 `append` 与 `updateByMsgId`（必须通过 action 修改才能触发 Vue 3 响应式渲染） |
| `conversation` | 列表、当前活跃会话、`upsertFromMessage`、`markActiveRead`、`openLocal`（在消息产生前即可创建本地会话条目） |
| `websocket` | 连接/断开/消息处理派发，断线后每 3 秒自动重连 |

### 乐观发送

1. 构造 `localMsg`（`pending: true`），追加到消息 store。
2. POST `/message/send`。
3. 成功 → `updateByMsgId` 将 `pending` 置 `false`，回填 `seq` / `sendTime`。
4. 失败 → 标记 `failed: true`。

### 动态后端地址

`api/client.js` 与 `stores/websocket.js` 从 `window.location.hostname` 推导
后端地址，因此同一个产物可以同时支持 `localhost` 与任意局域网 IP，无需环境变量。

### Emoji 输入

`ChatBox.vue` 集成 `emoji-picker-element` 作为 Web Component：
- 在 `vite.config.js` 中通过 `isCustomElement` 注册。
- 监听原生 `emoji-click` 事件。
- 在 textarea 光标位置插入 emoji 并移动光标。

---

## 快速开始

### 环境要求

- Java 17+
- PostgreSQL 14+
- Redis（仅在 `redis` 在线管理器模式下需要）
- Node.js 18+（前端）

### 1. PostgreSQL 设置

```bash
psql -d postgres -c "CREATE ROLE koim WITH LOGIN PASSWORD 'koim';"
psql -d postgres -c "CREATE DATABASE koim OWNER koim;"
```

### 2. Redis（可选）

如使用 `koim.online-manager=redis`，需确保 Redis 已启动：

```bash
redis-server    # 默认 localhost:6379
```

### 3. 后端

```bash
./gradlew bootRun        # 启动 Tomcat 监听 8080，Hibernate 自动建表
./gradlew test           # 运行 ImEndToEndTest（5 个测试用例）
```

### 4. 前端

```bash
cd front_end
npm install --legacy-peer-deps     # peer 依赖差异需要 legacy 解析
npm run dev                        # http://localhost:5173（同时开放 LAN 访问）
npm run build                      # 生产构建 → dist/
```

手机在同 Wi-Fi 下访问 `http://<Mac 局域网 IP>:5173` 无需任何额外配置。

### 5. 监控（可选）

```bash
cd ops
docker compose -f docker-compose.monitoring.yml up -d
```

---

## 配置项

`src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/koim
    username: koim
    password: koim
  data:
    redis:
      host: localhost
      port: 6379

koim:
  jwt:
    secret: <长度 ≥ 32 字节的密钥>
    expire-minutes: 1440
  ws:
    path: /ws/chat
  online-manager: redis   # memory（单实例）| redis（多实例）

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
```

CORS 默认开放所有来源便于本地开发（见 [SecurityConfig](src/main/kotlin/security/SecurityConfig.kt)）。

---

## 验收测试

`src/test/kotlin/ImEndToEndTest.kt` 覆盖 5 个场景（全部通过）：

1. **单聊** — 离线入库 / 上线同步 / 确认删除。
2. **幂等发送** — 相同 `msgId` 重复请求返回首次记录。
3. **会话** — 列表 / 未读数 / 标记已读。
4. **群聊** — 消息派发到除发送者外的所有成员。
5. **鉴权** — 未携带 token 的请求返回 401。

执行：`./gradlew test`。

---

## 常见问题

**Q：启动报 `FATAL: role "koim" does not exist`？**
A：需要先创建 PostgreSQL 角色和数据库，参见 PostgreSQL 设置章节。

**Q：手机访问注册时报"网络错误"？**
A：确保用电脑的局域网 IP 而不是 `localhost` 访问；macOS 防火墙需放行 5173/8080；
路由器若开启了 AP 隔离也会导致同 Wi-Fi 设备无法互通。

**Q：发送消息后 "sending…" 不变成时间戳？**
A：这是 Vue 3 响应式陷阱，已通过 `messages.js` 的 `updateByMsgId` 修复。
若仍出现，请硬刷新浏览器（Cmd+Shift+R）清除 Pinia HMR 缓存。

**Q：`npm install` 报 ERESOLVE 冲突？**
A：加上 `--legacy-peer-deps`；如果还报 EACCES，通过 `--cache /tmp/npm-cache-koim`
绕开 `~/.npm` 的权限问题。

---

## 已知限制与后续扩展

- **无历史消息漫游** — `/message/sync` 只返回离线队列；如需向上滚动加载需新增
  `/message/history?peerType&peerId&beforeSeq` 接口。
- **不支持文件/图片消息** — 后端 `content` 字段仅存文本。
- **无逐条已读回执** — 仅会话级未读数。
- **Actuator 安全** — `/actuator/**` 在开发模式下 `permitAll`；生产环境应绑定到独立管理端口并加防火墙。
