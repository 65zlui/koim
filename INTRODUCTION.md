# koim — Minimal Instant Messaging System

A minimal IM system built with **Kotlin / Spring Boot 3** backend and **Vue 3 / Vite** frontend.
Supports user registration/login, single chat, group chat, real-time WebSocket push,
offline message sync, unread badges, Redis-based multi-instance fanout, and a full
observability stack (Prometheus + Grafana + Alertmanager).

> 中文版本：[INTRODUCTION_zh.md](INTRODUCTION_zh.md)

---

## What Does It Look Like?

### Login

![Login Page](docs/screenshots/login.png)

Clean and minimal login form — enter username and password, or click "Create account" to register.

### Register

![Register Page](docs/screenshots/register.png)

Registration form with username, nickname, and password. After registering, you're automatically logged in.

### Chat Interface

After logging in, you land on the chat page. The sidebar shows your groups and conversations,
and the main pane shows the active chat.

![Empty Chat](docs/screenshots/chat-empty.png)

New users see an empty conversation list. You can:
- **Create a group** — enter a name and click "Create"
- **Join a group** — enter a group ID and click "Join"
- **Start a single chat** — enter a peer UID and click "Open"

### Single Chat

![Single Chat](docs/screenshots/chat-conversation.png)

Send messages in real time via WebSocket. The green dot in the sidebar header indicates
a live WS connection. Emoji input is supported via the emoji picker button.

### Group Chat

![Group Chat](docs/screenshots/chat-group.png)

Create groups, invite members, and chat together. Messages are dispatched to all group
members via WebSocket push or offline queue.

### Observability

The backend exposes Prometheus-format metrics at `/actuator/prometheus`:

![Prometheus Metrics](docs/screenshots/actuator-prometheus.png)

Health checks are available at `/actuator/health`:

![Health Check](docs/screenshots/actuator-health.png)

With the monitoring stack (`ops/docker-compose.monitoring.yml`), you get a full
Grafana dashboard auto-provisioned with IM-specific metrics.

---

## Tech Stack

### Backend

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.x · Java 17 |
| Framework | Spring Boot **3.3.5** |
| Web | Spring Web, Spring WebSocket |
| Data | Spring Data JPA · Hibernate 6.5 · PostgreSQL 14+ |
| Security | Spring Security 6 · JJWT 0.12.6 · BCrypt |
| Cache / Pub-Sub | Spring Data Redis |
| Monitoring | Spring Boot Actuator · Micrometer · Prometheus registry |
| Test | JUnit 5 · Spring Boot Test · H2 (PG-compat mode) |

### Frontend

| Layer | Technology |
|-------|-----------|
| Framework | Vue **3.5** + Vite **5.4** |
| State | Pinia |
| Routing | Vue Router 4 |
| HTTP | axios |
| Emoji | `emoji-picker-element` (Web Component) |
| Utilities | @vueuse/core |

### Observability

| Component | Version | Purpose |
|-----------|---------|---------|
| Prometheus | v2.55.0 | Metrics scraping & alerting |
| Grafana | 11.2.0 | Dashboards & visualization |
| Alertmanager | v0.27.0 | Alert routing & notification |

---

## Architecture Overview

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
                           (data)    (pub/sub)  (metrics)       │
                              │         │             │         │
                              │         │             ▼         │
                              │         │      /actuator/       │
                              │         │      prometheus       │
                              │         │             │         │
                              │    ┌────▼─────────────▼─────┐   │
                              │    │  Monitoring Stack      │   │
                              │    │  Prometheus + Grafana  │   │
                              │    │  + Alertmanager        │   │
                              │    └───────────────────────┘   │
                              └────────────────────────────────┘
```

---

## Repository Layout

```
koim/
├── build.gradle.kts                     # Gradle KTS build script
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt                  # @SpringBootApplication entry point
│   │   │   ├── controller/
│   │   │   │   ├── UserController.kt
│   │   │   │   ├── MessageController.kt
│   │   │   │   ├── ConversationController.kt
│   │   │   │   ├── GroupController.kt
│   │   │   │   └── GlobalExceptionHandler.kt
│   │   │   ├── dto/
│   │   │   │   └── Dtos.kt             # Request & response DTOs + ApiResponse wrapper
│   │   │   ├── entity/
│   │   │   │   ├── User.kt
│   │   │   │   ├── ChatGroup.kt
│   │   │   │   ├── GroupMember.kt
│   │   │   │   ├── Message.kt
│   │   │   │   ├── OfflineMessage.kt
│   │   │   │   └── Conversation.kt
│   │   │   ├── repository/              # 6 JpaRepositories
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
│   │   │   │   ├── OnlineUserManager.kt          # Interface
│   │   │   │   ├── InMemoryOnlineUserManager.kt   # Single-instance impl
│   │   │   │   ├── RedisOnlineUserManager.kt       # Multi-instance impl (pub/sub)
│   │   │   │   ├── ChatWebSocketHandler.kt
│   │   │   │   └── WebSocketConfig.kt
│   │   │   └── metrics/
│   │   │       ├── ImMetrics.kt                   # Domain metric definitions
│   │   │       └── OfflineQueueMetrics.kt          # Scheduled gauge poller
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── kotlin/ImEndToEndTest.kt
│       └── resources/application-test.yml          # H2 in PG-compat mode
├── front_end/
│   ├── package.json · vite.config.js · index.html
│   └── src/
│       ├── api/               # client.js (JWT + ApiResponse unwrap)
│       │                      # user.js · message.js · conversation.js · group.js
│       ├── stores/            # auth · messages · conversation · websocket
│       ├── router/index.js
│       ├── views/             # Login.vue · Register.vue · Chat.vue
│       ├── components/        # ConversationList · ChatBox · MessageBubble · GroupPanel
│       ├── App.vue · main.js · style.css
│       └── assets/
├── docs/                       # Documentation assets
│   └── screenshots/           # Actual running screenshots
└── ops/                       # Observability stack
    ├── docker-compose.monitoring.yml
    ├── prometheus.yml
    ├── alerts.yml
    ├── alertmanager.yml
    └── grafana/provisioning/  # Datasource + dashboard auto-provisioning
```

---

## Data Model

| Table | Purpose |
|-------|---------|
| `users` | uid (PK), username (unique), password (bcrypt), nickname |
| `chat_groups` | group_id (PK), name, owner_uid (renamed from `groups` to avoid SQL keyword) |
| `group_members` | composite PK (group_id, uid), join_time |
| `messages` | seq (IDENTITY PK), msg_id (unique idempotency key), from_uid, to_uid, group_id, type, content, send_time |
| `offline_messages` | id (PK), to_uid, msg_id, send_time — pending delivery queue |
| `conversations` | composite PK (uid, peer_type, peer_id), last_msg_*, unread_count |

Key design choices:

- **`seq`** — IDENTITY column (works on both PostgreSQL and H2) used for global message ordering and `lastSeq`-based offline sync.
- **`msg_id`** — Client-supplied UUID with a unique constraint → server-side idempotent send.
- **`peer_type`** — `'u'` (single chat) or `'g'` (group chat).

---

## REST API

All responses use the wrapper `{ ok: bool, data, error }`. All endpoints except
`/api/user/register`, `/api/user/login`, and `/ws/**` require `Authorization: Bearer <jwt>`.

### User

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/api/user/register` | `{ username, password, nickname }` | `{ uid, token }` |
| POST | `/api/user/login` | `{ username, password }` | `{ uid, token }` |

### Message

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/api/message/send` | `{ msgId, toUid?, groupId?, type, content }` | `{ seq, sendTime }` |
| POST | `/api/message/sync` | `{ lastSeq }` | `{ messages: MessageDto[] }` |
| POST | `/api/message/confirm` | `{ msgIds }` | — |

> `/message/send` is idempotent: same `msgId` returns the original record.
> `/message/sync` pulls offline messages with `seq > lastSeq`.
> `/message/confirm` removes confirmed messages from `offline_messages`.

### Conversation

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| GET | `/api/conversation/list` | — | `ConversationDto[]` |
| POST | `/api/conversation/read` | `{ peerType, peerId }` | unreadCount cleared |

### Group

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/api/group/create` | `{ name, memberUids: [] }` | `{ groupId }` |
| POST | `/api/group/join` | `{ groupId }` | `{ name, memberCount }` |
| GET | `/api/group/{id}` | — | group info |

### WebSocket

Connect: `ws://<host>:8080/ws/chat?token=<jwt>`

Server pushes:
```json
{ "type": "msg", "seq", "msgId", "fromUid", "toUid?", "groupId?", "content", "sendTime" }
```

---

## Message Flow

```
Client A ── POST /message/send ──▶ MessageService
                                    │
                                    ├─ Single chat
                                    │   ├─ B online?  ──▶ WS push to B
                                    │   └─ B offline? ──▶ INSERT offline_messages(B)
                                    │
                                    └─ Group chat
                                        └─ For each member ≠ sender:
                                            online? ──▶ WS push
                                            offline?──▶ INSERT offline_messages
```

On client login / reconnect:
1. Call `/message/sync` with locally saved `lastSeq` to pull offline messages.
2. Call `/message/confirm` to drain `offline_messages`.

---

## Multi-Instance Support

`OnlineUserManager` is an abstraction with two implementations, switched via
`koim.online-manager` in `application.yml`:

| Mode | Class | Mechanism | Use Case |
|------|-------|-----------|----------|
| `memory` | `InMemoryOnlineUserManager` | `ConcurrentHashMap` | Single-instance deployment |
| `redis` | `RedisOnlineUserManager` | Redis SET (`koim:online`) + pub/sub (`koim:fanout`) | Multi-instance deployment |

When `redis` mode is active:
- **Online presence** is tracked in a Redis SET, shared across all nodes.
- **Cross-node delivery** uses Redis pub/sub: if the target user is not connected
  to the local node, the payload is published to `koim:fanout`; every node
  receives it and the one owning the target uid delivers it via its local WebSocket.

---

## Observability

### Domain Metrics

All IM-specific metrics are defined in [ImMetrics](src/main/kotlin/metrics/ImMetrics.kt)
and exposed at `/actuator/prometheus`:

![Prometheus Metrics](docs/screenshots/actuator-prometheus.png)

| Metric | Type | Description |
|--------|------|-------------|
| `koim_ws_sessions_active` | Gauge | WebSocket sessions currently held by this process |
| `koim_offline_queue_size` | Gauge | Rows in `offline_messages` (polled every 30s) |
| `koim_ws_connect_total` | Counter | WebSocket connections accepted |
| `koim_ws_disconnect_total` | Counter | WebSocket connections closed |
| `koim_message_sent_total` | Counter (tagged: `peerType`, `result`) | Messages processed by MessageService |
| `koim_message_delivery` | Timer (p50, p95, p99) | MessageService.send end-to-end latency |
| `koim_redis_fanout_published_total` | Counter | Cross-node messages published to Redis pub/sub |
| `koim_redis_fanout_received_total` | Counter | Cross-node messages received from Redis pub/sub |

### Health Check

![Health Check](docs/screenshots/actuator-health.png)

### Monitoring Stack

Located in `ops/`. Brings up Prometheus + Grafana + Alertmanager via Docker Compose:

```bash
cd ops
docker compose -f docker-compose.monitoring.yml up -d
```

| Service | URL | Credentials |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Alertmanager | http://localhost:9093 | — |

The **koim → IM Overview** dashboard is auto-provisioned in Grafana.
See [ops/README.md](ops/README.md) for details.

---

## Frontend Architecture

### State Stores (Pinia)

| Store | Responsibility |
|-------|---------------|
| `auth` | Token + uid, persisted in localStorage |
| `messages` | `Map<peerKey, MessageDto[]>` with `append` and `updateByMsgId` (required for Vue 3 reactivity through Proxy) |
| `conversation` | List, active conversation, `upsertFromMessage`, `markActiveRead`, `openLocal` (create local entry before first message) |
| `websocket` | Connect / disconnect / handler dispatch, auto-reconnect every 3s |

### Optimistic Send

1. Build `localMsg` with `pending: true` and append to messages store.
2. POST `/message/send`.
3. On success → `updateByMsgId` clears `pending` and fills `seq` / `sendTime`.
4. On failure → mark `failed: true`.

### Dynamic API Base URL

`api/client.js` and `stores/websocket.js` derive the backend host from
`window.location.hostname`. The same build works for `localhost` and any
LAN IP without environment variables.

### Emoji Input

`ChatBox.vue` integrates `emoji-picker-element` as a Web Component:
- Registered via `isCustomElement` in `vite.config.js`.
- Listens to native `emoji-click` events.
- Inserts emoji at cursor position in the textarea.

---

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Redis (only needed for `redis` online-manager mode)
- Node.js 18+ (for frontend)

### 1. PostgreSQL Setup

```bash
psql -d postgres -c "CREATE ROLE koim WITH LOGIN PASSWORD 'koim';"
psql -d postgres -c "CREATE DATABASE koim OWNER koim;"
```

### 2. Redis (Optional)

If using `koim.online-manager=redis`, ensure Redis is running:

```bash
redis-server    # defaults to localhost:6379
```

### 3. Backend

```bash
./gradlew bootRun        # Starts on :8080, Hibernate auto-creates tables
./gradlew test           # Runs ImEndToEndTest (5 test cases)
```

### 4. Frontend

```bash
cd front_end
npm install --legacy-peer-deps     # Legacy peer dep resolution required
npm run dev                        # http://localhost:5173 (LAN-accessible)
npm run build                      # Production build → dist/
```

Opening `http://<mac-LAN-ip>:5173` from a phone on the same Wi-Fi works
without any additional configuration.

### 5. Monitoring (Optional)

```bash
cd ops
docker compose -f docker-compose.monitoring.yml up -d
```

---

## Configuration

`src/main/resources/application.yml`:

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
    secret: <32+ byte secret>
    expire-minutes: 1440
  ws:
    path: /ws/chat
  online-manager: redis   # memory (single-instance) | redis (multi-instance)

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

CORS is open to all origins for development convenience (see [SecurityConfig](src/main/kotlin/security/SecurityConfig.kt)).

---

## Acceptance Tests

`src/test/kotlin/ImEndToEndTest.kt` covers 5 scenarios (all passing):

1. **Single chat** — offline insertion, sync after connect, confirm drains queue.
2. **Idempotent send** — same `msgId` returns the original record.
3. **Conversation** — list / unread count / mark-as-read.
4. **Group chat** — message dispatched to every member except sender.
5. **Auth** — 401 on unauthenticated requests.

Run with `./gradlew test`.

---

## FAQ

**Q: Startup fails with `FATAL: role "koim" does not exist`?**
A: Create the PostgreSQL role and database first — see the PostgreSQL Setup section.

**Q: Mobile browser shows "Network Error" on registration?**
A: Use the computer's LAN IP instead of `localhost`. Make sure macOS firewall allows
ports 5173/8080, and that AP isolation is disabled on your router.

**Q: Sent message stays as "sending…" without a timestamp?**
A: This is a Vue 3 reactivity gotcha, fixed via `updateByMsgId` in `messages.js`.
If it persists, hard-refresh the browser (Cmd+Shift+R) to clear Pinia HMR cache.

**Q: `npm install` fails with ERESOLVE?**
A: Add `--legacy-peer-deps`. If you also get EACCES, use
`--cache /tmp/npm-cache-koim` to bypass `~/.npm` permission issues.

---

## Limitations & Future Work

- **No history pagination** — `/message/sync` only returns the offline queue. A
  `/message/history?peerType&peerId&beforeSeq` endpoint would support scroll-up
  message roaming.
- **No file / image messages** — `content` is text only.
- **No per-message read receipts** — only per-conversation `unreadCount`.
- **Actuator security** — `/actuator/**` is `permitAll` in dev; bind to a
  separate management port and firewall it in production.
