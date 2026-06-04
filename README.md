# koim — Minimal IM (Spring Boot + Vue 3)

A minimal instant-messaging system with a Kotlin / Spring Boot backend (PostgreSQL only)
and a Vue 3 / Vite frontend. Supports user register / login, single chat, group chat,
real-time WebSocket push, offline message sync, and unread badges.

> 中文版本：[README_zh.md](README_zh.md)

---

## 1. Tech Stack

### Backend
- Kotlin 2.x · Spring Boot **3.3.5** · Java 17
- Spring Web, Spring WebSocket, Spring Data JPA, Spring Security 6
- PostgreSQL 14+ (runtime), H2 in PG-compat mode (tests)
- JJWT 0.12.6 for stateless JWT auth
- Hibernate 6.5 with `ddl-auto: update`
- BCrypt password hashing

### Frontend
- Vue **3.5** + Vite **5.4** (no TypeScript)
- Pinia (state) · Vue Router · axios · @vueuse/core
- `emoji-picker-element` (Web Component) for emoji input

---

## 2. Repository Layout

```
koim/
├── build.gradle.kts                # Gradle KTS build (Spring Boot, JPA, JWT, JJWT, etc.)
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt
│   │   │   ├── config/             # WebSocketConfig
│   │   │   ├── controller/         # User / Message / Conversation / Group / GlobalExceptionHandler
│   │   │   ├── dto/Dtos.kt         # Request & response DTOs + ApiResponse wrapper
│   │   │   ├── entity/             # User, ChatGroup, GroupMember, Message,
│   │   │   │                       # OfflineMessage, Conversation
│   │   │   ├── repository/         # JpaRepositories (6)
│   │   │   ├── security/           # JwtService, JwtAuthenticationFilter,
│   │   │   │                       # SecurityConfig, CurrentUser
│   │   │   ├── service/            # UserService, MessageService,
│   │   │   │                       # ConversationService, GroupService
│   │   │   └── ws/                 # OnlineUserManager, ChatWebSocketHandler
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── kotlin/ImEndToEndTest.kt
│       └── resources/application-test.yml   # H2 in PG mode
└── front_end/
    ├── package.json · vite.config.js · index.html
    └── src/
        ├── api/         # client.js (JWT + ApiResponse unwrap), user / message
        │                # / conversation / group
        ├── stores/      # auth · messages · conversation · websocket
        ├── router/index.js
        ├── views/       # Login.vue · Register.vue · Chat.vue
        ├── components/  # ConversationList · ChatBox · MessageBubble · GroupPanel
        ├── App.vue · main.js · style.css
        └── assets/
```

---

## 3. Data Model

| Table             | Purpose                                                              |
| ----------------- | -------------------------------------------------------------------- |
| `users`           | uid (PK), username (unique), password (bcrypt), nickname             |
| `chat_groups`     | group_id (PK), name, owner_uid (renamed from `groups` to avoid SQL keyword) |
| `group_members`   | composite PK (group_id, uid), join_time                              |
| `messages`        | seq (IDENTITY PK), msg_id (unique idempotency key), from_uid, to_uid, group_id, type, content, send_time |
| `offline_messages`| id (PK), to_uid, msg_id, send_time — pending delivery queue          |
| `conversations`   | composite PK (uid, peer_type, peer_id), last_msg_*, unread_count     |

Key choices:
- `seq` is a global IDENTITY column (works on both PostgreSQL and H2) used for ordering and for `lastSeq`-based offline sync.
- `msg_id` is a client-supplied UUID with a unique constraint → server-side idempotent send.
- `peer_type` is `'u'` (single chat) or `'g'` (group).

---

## 4. REST API

All responses use the wrapper `{ ok: bool, data, error }`. All endpoints
except `/api/user/register`, `/api/user/login`, and `/ws/**` require
`Authorization: Bearer <jwt>`.

### User
- `POST /api/user/register` → `{ uid, token }`
- `POST /api/user/login` → `{ uid, token }`

### Message
- `POST /api/message/send` `{ msgId, toUid?, groupId?, type, content }` → `{ seq, sendTime }`
  Idempotent: same `msgId` returns the original record.
- `POST /api/message/sync` `{ lastSeq }` → `{ messages: MessageDto[] }`
  Pulls offline messages with seq > lastSeq.
- `POST /api/message/confirm` `{ msgIds }` → deletes from `offline_messages`.

### Conversation
- `GET  /api/conversation/list` → `ConversationDto[]`
- `POST /api/conversation/read` `{ peerType, peerId }` → unreadCount cleared

### Group
- `POST /api/group/create` `{ name, memberUids: [] }` → `{ groupId }`
- `POST /api/group/join` `{ groupId }` → `{ name, memberCount }`
- `GET  /api/group/{id}` → group info

### WebSocket
- `ws://<host>:8080/ws/chat?token=<jwt>`
  Server pushes `{ type: 'msg', seq, msgId, fromUid, toUid?, groupId?, content, sendTime }`.

---

## 5. Message Flow

```
client A ── POST /message/send ──▶ MessageService
                                   │
                                   ├─ (single chat)
                                   │   ├─ B online?  WS push to B
                                   │   └─ B offline? insert offline_messages(B)
                                   │
                                   └─ (group chat)
                                       └─ for each member ≠ sender:
                                           online?  WS push  : insert offline_messages
```

On client login / reconnect, the frontend calls `/message/sync` with the
last seen seq, then `/message/confirm` to drain `offline_messages`.

---

## 6. Run It Locally

### 6.1 PostgreSQL
```bash
psql -d postgres -c "CREATE ROLE koim WITH LOGIN PASSWORD 'koim';"
psql -d postgres -c "CREATE DATABASE koim OWNER koim;"
```

### 6.2 Backend
```bash
./gradlew bootRun        # starts Tomcat on :8080, Hibernate auto-creates tables
./gradlew test           # runs ImEndToEndTest (5 cases, all pass)
```

### 6.3 Frontend
```bash
cd front_end
npm install --legacy-peer-deps     # peer deps differ; legacy resolution required
npm run dev                        # http://localhost:5173 (also reachable on LAN)
npm run build
```

The frontend derives the backend host from `window.location.hostname`
automatically, so opening `http://<mac-LAN-ip>:5173` from a phone on the
same Wi-Fi works without configuration.

---

## 7. Frontend Architecture Notes

### State stores (Pinia)
- `auth` — token + uid, persisted in localStorage.
- `messages` — `Map<peerKey, MessageDto[]>`, with `append` and
  `updateByMsgId` (the latter is required to mutate messages **through** the
  reactive Proxy; mutating the raw object reference does not trigger
  re-renders).
- `conversation` — list / active / `upsertFromMessage` / `markActiveRead` /
  `openLocal` (creates a local conversation entry so the user can start
  typing before any message exists).
- `websocket` — connect / disconnect / handler dispatch with auto-reconnect
  every 3 s.

### Optimistic send
1. Build `localMsg` with `pending: true` and append to messages store.
2. POST `/message/send`.
3. On success → `updateByMsgId` to clear `pending` and fill `seq` / `sendTime`.
4. On failure → mark `failed: true`.

### Dynamic API base URL
`api/client.js` and `stores/websocket.js` derive their base from
`window.location.hostname`, so the same build works for `localhost` and any
LAN IP without env variables.

---

## 8. Acceptance Tests

`src/test/kotlin/ImEndToEndTest.kt` covers (all passing):

1. Single chat — offline insertion, sync after connect, confirm drains queue.
2. Idempotent send — same `msgId` returns the original record.
3. Conversation list / unread / mark-as-read.
4. Group chat — message dispatched to every member except sender.
5. 401 on unauthenticated requests.

Run with `./gradlew test`.

---

## 9. Configuration

`src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/koim
    username: koim
    password: koim
koim:
  jwt:
    secret: <32+ byte secret>
    expire-minutes: 1440
  ws:
    path: /ws/chat
```

CORS is open to all origins (see [SecurityConfig](src/main/kotlin/security/SecurityConfig.kt)) for development convenience.

---

## 10. Limitations / Future Work

- Single-instance only — `OnlineUserManager` is an in-memory map. Multi-node
  deployments need Redis pub/sub or a session registry.
- No history pagination — `/message/sync` only returns the offline queue.
  A `/message/history?peerType&peerId&beforeSeq` endpoint would back a
  scroll-up roaming UI.
- No file / image messages — backend stores `content` as text only.
- No read receipts beyond per-conversation `unreadCount`.
