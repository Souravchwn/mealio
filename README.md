# 🍽 Mealio

> **High-integrity automation platform for shared living (messes) in Bangladesh & India.**
> Replaces manual ledgers with an ACID-compliant Spring Boot backend that treats every meal and bazaar expense as a verifiable data point for future AI health and economic insights.

---

## Table of Contents

- [Product Vision](#-product-vision)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture--ports--adapters--solid)
- [Project Structure](#-project-structure)
- [Database Schema](#-database-schema)
- [API Reference](#-api-reference)
- [Telegram Bot Setup](#-telegram-bot-setup)
- [Environment Variables](#-environment-variables)
- [Running Locally](#-running-locally)
- [Deployment](#-deployment-railwayrender-free-tier)
- [Concurrency & Thread Safety](#-concurrency--thread-safety)
- [Roadmap](#-roadmap)

---

## 🎯 Product Vision

Mealio is built for the **50+ person "mess" culture** of BD/IN — shared apartments where 5–30 people eat together, split bazaar costs, and manage daily meal attendance. Today this is done with WhatsApp messages and Excel sheets. Mealio replaces that chaos with:

| Problem | Mealio Solution |
|---|---|
| "Did I say /off today?" | Telegram Bot in the mess group — one command, instant confirmation |
| Manual Excel every month | Admin matrix auto-calculated from live data |
| Argument over the bill | ACID transactions — every taka is traceable |
| Cook doesn't know headcount | Firebase-cached real-time dashboard |

---

## 🛠 Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Backend** | Spring Boot 3.2 (Java 21) | Production-grade, free Railway/Render hosting |
| **Database** | PostgreSQL on [Neon.tech](https://neon.tech) | Serverless, free tier, ACID-compliant |
| **ORM** | Spring Data JPA + Hibernate | SOLID repository pattern, zero SQL for 90% of queries |
| **Cache** | Firebase Realtime DB | Sub-50ms cook's dashboard reads on 3G |
| **Bot** | Telegram Bot API (webhook-based) | No SDK needed — plain HTTP + Jackson |
| **Concurrency** | Spring Retry + JPA Pessimistic/Optimistic Locking | Financially safe under concurrent load |
| **Build** | Maven 3.9 | Standard, IDE-friendly |

---

## 🏗 Architecture — Ports & Adapters (SOLID)

Mealio uses **Hexagonal Architecture**. The domain (services, entities) knows **nothing** about Telegram, Firebase, or any external system. Switching platforms means swapping adapters — zero domain changes.

```
┌──────────────────────────────────────────────────────────────────────┐
│  ADAPTERS  (only these files know about external platforms)          │
│                                                                      │
│  controller/TelegramWebhookController  ← maps Telegram → BotCommand │
│  adapter/telegram/TelegramMemberIdentityAdapter  ← MemberIdentityPort│
│  adapter/telegram/TelegramBotReplyAdapter        ← BotReplyPort      │
│  adapter/cache/FirebaseHeadcountAdapter          ← HeadcountCachePort│
│  adapter/cache/NoOpHeadcountAdapter              ← (dev profile)     │
└───────────┬──────────────────────────────────────┬───────────────────┘
            │ port/in/                              │ port/out/
            ▼                                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PORTS  (interfaces — never change)                                  │
│                                                                      │
│  port/in/  MealToggleUseCase   BotCommand                            │
│            ExpenseUseCase      MonthCloseUseCase                     │
│            HeadcountUseCase                                          │
│  port/out/ MemberIdentityPort  BotReplyPort  HeadcountCachePort      │
└───────────┬──────────────────────────────────────────────────────────┘
            │ implements / injects
            ▼
┌──────────────────────────────────────────────────────────────────────┐
│  DOMAIN  (pure Java — zero platform imports)                         │
│                                                                      │
│  service/MealToggleService    service/ExpenseService                 │
│  service/HeadcountService     service/MonthCloseService              │
│  model/entity/*               repository/*                          │
└──────────────────────────────────────────────────────────────────────┘
```

### Adding a New Bot Platform (e.g. WhatsApp)

All you write is:
```java
// 1. New controller — maps WhatsApp payload → BotCommand
class WhatsAppWebhookController { ... }

// 2. New adapter — resolves member by phone
class WhatsAppMemberIdentityAdapter implements MemberIdentityPort {
    Optional<Member> resolve(String phone) {
        return memberRepo.findByPhone(phone); // already exists ✅
    }
}

// 3. New adapter — sends reply via Meta Cloud API
class WhatsAppBotReplyAdapter implements BotReplyPort { ... }

// Domain code changed: 0 files ✅
```

---

## 📁 Project Structure

```
src/main/java/com/mealio/
├── MealioApplication.java          (@SpringBootApplication + @EnableRetry)
│
├── port/
│   ├── in/                         ← Use-case interfaces (inbound)
│   │   ├── BotCommand.java         (platform-agnostic command record)
│   │   ├── MealToggleUseCase.java
│   │   ├── ExpenseUseCase.java
│   │   ├── MonthCloseUseCase.java
│   │   └── HeadcountUseCase.java
│   └── out/                        ← Outbound port interfaces
│       ├── MemberIdentityPort.java
│       ├── BotReplyPort.java
│       └── HeadcountCachePort.java
│
├── adapter/
│   ├── telegram/                   ← Telegram-specific implementations
│   │   ├── TelegramMemberIdentityAdapter.java
│   │   └── TelegramBotReplyAdapter.java
│   └── cache/                      ← Cache implementations
│       ├── FirebaseHeadcountAdapter.java  (prod)
│       └── NoOpHeadcountAdapter.java      (dev — no Firebase needed)
│
├── model/
│   ├── entity/                     ← JPA entities (the Data Goldmine)
│   │   ├── Mess.java               (@Version for OCC)
│   │   ├── Member.java             (@Version — guards balance)
│   │   ├── DailyLog.java           (@Version — guards meal toggles)
│   │   ├── Expense.java
│   │   ├── MonthlySnapshot.java    (@Version — prevents double close)
│   │   └── AuditTrail.java
│   └── enums/
│       ├── Role.java               (ADMIN / MANAGER / MEMBER)
│       ├── MealSlot.java           (BREAKFAST / LUNCH / DINNER)
│       ├── ExpenseCategory.java    (PROTEIN / CARB / VEGETABLE / ...)
│       └── MonthStatus.java        (OPEN / CLOSED)
│
├── service/                        ← Domain services (pure business logic)
│   ├── MealToggleService.java      (implements MealToggleUseCase)
│   ├── ExpenseService.java         (implements ExpenseUseCase)
│   ├── HeadcountService.java       (implements HeadcountUseCase)
│   ├── MonthCloseService.java      (implements MonthCloseUseCase)
│   ├── AuditService.java
│   └── TelegramBotService.java     (raw HTTP client for sendMessage API)
│
├── controller/                     ← HTTP adapters
│   ├── TelegramWebhookController.java
│   ├── ExpenseController.java
│   ├── AdminController.java
│   └── CookController.java
│
├── repository/                     ← Spring Data JPA (+ locking queries)
│   ├── MemberRepository.java       (findByIdWithLock, findAllByMessWithLock)
│   ├── DailyLogRepository.java
│   ├── ExpenseRepository.java
│   ├── MonthlySnapshotRepository.java  (findByMessAndYearMonthWithLock)
│   ├── MessRepository.java
│   └── AuditTrailRepository.java
│
├── dto/                            ← Request/Response records
│   ├── telegram/TelegramUpdate.java
│   ├── ExpenseRequest.java / ExpenseResponse.java
│   ├── DailyLogDto.java
│   ├── MonthMatrixResponse.java
│   ├── CloseMonthRequest.java
│   └── HeadcountResponse.java
│
├── config/
│   ├── CorsConfig.java             (PWA origins whitelist)
│   └── FirebaseConfig.java         (@ConditionalOnProperty — disabled in dev)
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java     (404)
    ├── CutOffTimeExceededException.java   (409)
    └── MonthAlreadyClosedException.java   (409)
```

---

## 🗃 Database Schema

```
┌──────────────────┐       ┌──────────────────────────────┐
│      mess        │       │           member             │
├──────────────────┤       ├──────────────────────────────┤
│ id (UUID PK)     │◄──────│ mess_id (FK)                 │
│ name             │       │ id (UUID PK)                 │
│ cut_off_time     │       │ name                         │
│ created_at       │       │ phone (unique, nullable)     │
└──────────────────┘       │ telegram_user_id (unique)    │
                           │ role (ADMIN/MANAGER/MEMBER)  │
                           │ balance (BigDecimal)         │
                           │ version (OCC)                │
                           └──────────────────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
         ┌─────────────────┐  ┌────────────────┐  ┌──────────────────┐
         │   daily_log     │  │    expense     │  │ monthly_snapshot  │
         ├─────────────────┤  ├────────────────┤  ├──────────────────┤
         │ id (UUID PK)    │  │ id (UUID PK)   │  │ id (UUID PK)     │
         │ member_id (FK)  │  │ mess_id (FK)   │  │ mess_id (FK)     │
         │ date            │  │ member_id (FK) │  │ year_month       │
         │ breakfast (bool)│  │ amount         │  │ total_expense    │
         │ lunch (bool)    │  │ category       │  │ meal_rate        │
         │ dinner (bool)   │  │ description    │  │ total_meals      │
         │ guest_count     │  │ date           │  │ status           │
         │ frozen (bool)   │  │ created_at     │  │ closed_at        │
         │ version (OCC)   │  └────────────────┘  │ version (OCC)    │
         └─────────────────┘                      └──────────────────┘

         ┌─────────────────────────────────────────┐
         │              audit_trail                │
         ├─────────────────────────────────────────┤
         │ id (UUID PK)   admin_id (FK → member)   │
         │ entity_type    entity_id (UUID)          │
         │ old_value (JSON TEXT)                    │
         │ new_value (JSON TEXT)                    │
         │ reason         created_at               │
         └─────────────────────────────────────────┘
```

### Expense Categories (the AI Data Foundation)

| Category | Examples |
|---|---|
| `PROTEIN` | meat, fish, eggs, lentils |
| `CARB` | rice, flour, bread |
| `VEGETABLE` | all vegetables |
| `SPICE` | turmeric, chilli, coriander |
| `OIL` | soybean, mustard oil |
| `UTILITY` | gas cylinder, electricity |
| `OTHER` | anything else |

These categories are the raw data points for the future **nutrition AI** and **commodity price tracking** vision.

---

## 📡 API Reference

### Telegram Bot Webhook

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/telegram/webhook` | Meta handshake (not needed for Telegram) |
| `POST` | `/api/telegram/webhook` | Receives all Telegram Updates |
| `GET` | `/api/telegram/health` | Liveness check |

### Expense Management

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/expenses` | Add a categorized bazaar expense |
| `GET` | `/api/expenses?messId=&yearMonth=2026-03` | List expenses for a month |
| `GET` | `/api/expenses/meal-rate?messId=&yearMonth=` | Live meal rate (total ÷ meals) |

**Example — add expense:**
```json
POST /api/expenses
{
  "messId": "uuid",
  "memberId": "uuid",
  "amount": 850.00,
  "category": "PROTEIN",
  "description": "Rui fish from Karwan Bazar",
  "date": "2026-03-02"
}
```

**Response includes live meal rate:**
```json
{
  "id": "uuid",
  "amount": 850.00,
  "category": "PROTEIN",
  "liveMealRate": 87.50
}
```

### Admin Matrix & Month-Close

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/matrix?messId=&yearMonth=` | Full month grid (all members × all days) |
| `POST` | `/api/admin/close-month` | Atomically freeze, calculate, snapshot, rollover |

**Example — close month:**
```json
POST /api/admin/close-month
{
  "messId": "uuid",
  "adminId": "uuid",
  "yearMonth": "2026-02"
}
```

### Cook's Dashboard

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/cook/headcount?messId=` | Today's member + guest count |

**Response:**
```json
{
  "messName": "Bashundhara Mess",
  "date": "2026-03-02",
  "memberCount": 14,
  "guestCount": 2,
  "totalHeadcount": 16,
  "source": "database"
}
```

---

## 🤖 Telegram Bot Setup

### Step 1 — Create the bot (one-time)

```
1. Open Telegram → search @BotFather
2. Send: /newbot
3. Choose a name: "Bashundhara Mess Bot"
4. Choose a username: "Bashundhara_Mess_Bot"
5. Copy the API token → set as TELEGRAM_BOT_TOKEN env var
```

### Step 2 — Disable Privacy Mode (so bot reads group commands)

```
BotFather → /mybots → select your bot
→ Bot Settings → Group Privacy → Turn OFF
```

### Step 3 — Register the webhook (one-time, after deployment)

```bash
curl "https://api.telegram.org/bot{YOUR_TOKEN}/setWebhook?url=https://your-app.railway.app/api/telegram/webhook"

# Verify:
curl "https://api.telegram.org/bot{YOUR_TOKEN}/getWebhookInfo"
```

### Step 4 — Add bot to the mess group

Invite the bot to your Telegram group. Members then register their Telegram user ID with the admin (or a future `/register` flow).

### Member Commands

| Command | Action |
|---|---|
| `/on` | All three meals ON for today |
| `/off` | All three meals OFF for today |
| `/breakfast on\|off` | Toggle only breakfast |
| `/lunch on\|off` | Toggle only lunch |
| `/dinner on\|off` | Toggle only dinner |
| `/guest [n]` | Add n guests (default 1) |
| `/status` | Show today's meal status |
| `/help` | Show all commands |

> ⏰ Commands are rejected after the mess **cut-off time** (default 21:00 BD). Configurable per mess.

---

## 🔐 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | ✅ | PostgreSQL JDBC URL (e.g. Neon connection string) |
| `DB_USERNAME` | ✅ | Database username |
| `DB_PASSWORD` | ✅ | Database password |
| `TELEGRAM_BOT_TOKEN` | ✅ | From @BotFather |
| `TELEGRAM_BOT_USERNAME` | ❌ | Bot username (for @mention stripping) |
| `FIREBASE_ENABLED` | ❌ | `true` to enable Firebase cache (default: `false`) |
| `FIREBASE_SA_PATH` | ❌ | Path to service account JSON (if Firebase enabled) |
| `FIREBASE_DB_URL` | ❌ | Firebase Realtime DB URL |

> ⚠️ **Never commit `firebase-service-account.json` or `.env` files.** Both are in `.gitignore`.

---

## 🚀 Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+ (or use IntelliJ's bundled Maven)
- No Docker, no external DB needed for dev ✅

### 1. Clone and run (H2 in-memory profile)

```bash
git clone https://github.com/yourname/mealio.git
cd mealio

# Using IntelliJ's bundled Maven (Windows):
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" ^
  spring-boot:run -Dspring-boot.run.profiles=dev

# Or if Maven is on PATH:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The app starts on **http://localhost:8080** with:
- H2 in-memory database (schema auto-created)
- Firebase disabled (NoOpHeadcountAdapter active)
- H2 console at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:mealio`)

### 2. Run with real PostgreSQL (Neon)

```bash
export DB_URL="jdbc:postgresql://ep-xxx.us-east-1.aws.neon.tech/mealio?sslmode=require"
export DB_USERNAME="mealio_user"
export DB_PASSWORD="your-password"
export TELEGRAM_BOT_TOKEN="1234567890:ABCDEF..."

mvn spring-boot:run
```

### 3. Run tests

```bash
mvn test
# Context loads against H2 automatically via @ActiveProfiles("dev")
```

---

## ☁️ Deployment (Railway/Render Free Tier)

### Railway (recommended)

```bash
# 1. Install Railway CLI
npm install -g @railway/cli

# 2. Login and init
railway login
railway init

# 3. Add PostgreSQL plugin from Railway dashboard
# 4. Set environment variables in Railway dashboard
# 5. Deploy
railway up
```

Railway auto-detects the Maven project and runs `mvn package -DskipTests && java -jar target/*.jar`.

### Render

1. New Web Service → connect GitHub repo
2. Build command: `mvn clean package -DskipTests`
3. Start command: `java -jar target/mealio-0.0.1-SNAPSHOT.jar`
4. Add PostgreSQL from Render dashboard → copy connection string to `DB_URL`

> 💡 Both Railway and Render have generous free tiers that stay within NFR-3 (zero-cost for 50 pilot groups).

---

## 🔒 Concurrency & Thread Safety

Mealio handles concurrent meal toggles and financial operations safely using a **three-layer defense**:

```
Layer 1 — Pessimistic Locking (prevent the race)
  SELECT ... FOR UPDATE on the Member row before any DailyLog operation.
  Serializes concurrent toggles from the same member.
  Different members run fully in parallel — no performance impact.

Layer 2 — Optimistic Locking (catch any slip-through)
  @Version on DailyLog, Member, MonthlySnapshot.
  Hibernate rejects the second committer with OptimisticLockingFailureException.
  No silent lost updates, ever.

Layer 3 — @Retryable (graceful recovery)
  Spring Retry re-runs the method (fresh transaction) up to 3×
  with exponential jitter back-off on lock/OCC failures.
  @Recover sends a friendly "please retry" message after exhaustion.
```

### Month-Close (financially critical)

```
+ SERIALIZABLE transaction isolation
+ Pessimistic WRITE lock on MonthlySnapshot (prevents double-close)
+ Pessimistic WRITE lock on all Member rows (ORDER BY id — deadlock prevention)
+ Transaction timeout: 60 seconds
```

### Error responses from concurrency control

| HTTP Status | Meaning |
|---|---|
| `409 Conflict` | OCC version mismatch or month already closed — data safe |
| `503 Service Unavailable` | DB lock timeout (under extreme load) — retry in seconds |

---

## 🗺 Roadmap

### Current (v0.1 — Foundation)
- [x] Spring Boot project scaffold
- [x] PostgreSQL schema with 6 entities
- [x] Telegram Bot webhook (replaces WhatsApp for pilot)
- [x] Meal toggle service (B/L/D + guests + cut-off enforcement)
- [x] Bazaar expense management with live meal rate
- [x] Cook's dashboard headcount (Firebase cache / DB fallback)
- [x] Admin month-close (atomic freeze + balance + snapshot)
- [x] Full SOLID / Ports & Adapters architecture
- [x] Thread-safe with pessimistic + optimistic locking + @Retryable

### v0.2 — Member Self-Service
- [ ] `/register` bot command to self-enroll via Telegram
- [ ] `/balance` command to check personal balance
- [ ] Monthly statement PDF via `/statement` command

### v0.3 — PWA (React)
- [ ] Cook's dashboard real-time PWA (Firebase SWR)
- [ ] Expense entry PWA for the Meal Manager
- [ ] Admin Excel-Killer matrix with month-close button

### v1.0 — AI Insights (The Big Idea)
- [ ] Nutrition analysis per member (PROTEIN/CARB/VEGETABLE trends)
- [ ] Regional commodity price tracking from expense data
- [ ] Behavioral "meal-off" pattern prediction
- [ ] Personal health recommendations

---

## 📄 License

MIT — see [LICENSE](LICENSE)

---

> Built with ❤️ for the mess culture of 🇧🇩 Bangladesh & 🇮🇳 India.
