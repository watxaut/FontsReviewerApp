# FontsReviewer - AI Agent Guide

> **Quick Start:** Read `implementation/` directory in order. Start with `02-CURRENT_STATE/APP_STATUS.md` for current snapshot, then `01-MIGRATIONS/MIGRATION_HISTORY.md` to understand database evolution.

---

## 📋 Project Overview

**FontsReviewer** - Android app for rating 1,745 Barcelona fountains with social leaderboard.

### Key Info
- **Platform:** Android (Kotlin, Jetpack Compose)
- **Min SDK:** 29 | **Target SDK:** 36
- **Backend:** Supabase (PostgreSQL + Auth)
- **Maps:** Mapbox Android SDK
- **Architecture:** MVVM + Clean Architecture + Hilt DI
- **Languages:** English, Spanish, Catalan
- **Status:** 🟢 95% Production Ready

### Core Features
1. **Map View** - All 1,745 fountains with Mapbox clustering
2. **Anonymous Browsing** - View reviews without login
3. **Authentication** - Email/password via Supabase
4. **Review System** - 6 rating categories (taste, freshness, location, aesthetics, splash, jet)
5. **User Stats** - Personal metrics and best-rated fountain
6. **Leaderboard** - Global rankings
7. **User Roles** - Admin (unrestricted) vs Operator (300m radius)

---

## 📁 Project Structure

```
FontsReviewer/
├── app/src/main/java/com/watxaut/fontsreviewer/
│   ├── data/                    # Data layer
│   │   ├── remote/              # Supabase DTOs & service
│   │   ├── repository/          # Repository implementations
│   │   └── mapper/              # DTO <-> Domain mappers
│   ├── domain/                  # Business logic
│   │   ├── model/               # Domain models
│   │   ├── repository/          # Repository interfaces
│   │   └── usecase/             # Use cases
│   ├── presentation/            # UI layer (Compose)
│   │   ├── auth/, map/, details/, review/, stats/, leaderboard/, profile/
│   │   └── navigation/          # Nav graph
│   ├── di/                      # Hilt modules
│   ├── ui/                      # Theme
│   └── util/                    # Utilities
│
├── implementation/              # 📖 ESSENTIAL DOCS (Organized by Purpose)
│   │
│   ├── 01-MIGRATIONS/           # Database evolution (read in order!)
│   │   ├── MIGRATION_HISTORY.md        # ⭐ Migration timeline & verification
│   │   ├── 01-FOUNTAINS_MIGRATION.sql
│   │   ├── 02-USER_ROLES_MIGRATION.sql
│   │   ├── 03-ADMIN_FOUNTAIN_MANAGEMENT_MIGRATION.sql
│   │   └── 04-DELETE_ACCOUNT_MIGRATION.sql
│   │
│   ├── 02-CURRENT_STATE/        # Where we are NOW
│   │   ├── APP_STATUS.md               # ⭐ Current implementation snapshot
│   │   └── KNOWN_ISSUES.md             # Technical debt & bugs
│   │
│   ├── 03-PRODUCTION/           # Launch preparation
│   │   ├── DEPLOYMENT_GUIDE.md         # Step-by-step launch process
│   │   ├── SECURITY_CHECKLIST.md       # Security hardening
│   │   └── CONTENT_MODERATION_STRATEGY.md  # Community moderation
│   │
│   └── 04-SETUP_GUIDES/         # Initial setup instructions
│       ├── SUPABASE_SETUP.md           # Backend configuration
│       ├── EDGE_FUNCTION_SETUP.md      # Account deletion function
│       └── MAPBOX_SETUP.md             # Map provider setup
│
├── local.properties             # 🔒 Credentials (gitignored)
└── LICENSE                      # MIT License
```

---

## 🔍 Essential Reading Order

**For understanding the current state:**

1. **`implementation/02-CURRENT_STATE/APP_STATUS.md`** ⭐ START HERE
   - Complete current status snapshot (95% production ready)
   - What's working, what needs attention
   - Confidence levels and metrics
   - Path to launch

2. **`implementation/01-MIGRATIONS/MIGRATION_HISTORY.md`** ⭐ DATABASE EVOLUTION
   - Chronological database changes
   - 4 migrations applied in order
   - Verification queries
   - Current production state

3. **`implementation/02-CURRENT_STATE/KNOWN_ISSUES.md`**
   - Technical debt items
   - Known bugs and workarounds
   - Prioritized action items
   - None are launch blockers

**For making changes:**

4. **`implementation/04-SETUP_GUIDES/SUPABASE_SETUP.md`**
   - Database schema details
   - RLS policies
   - Triggers and functions

5. **`implementation/03-PRODUCTION/DEPLOYMENT_GUIDE.md`**
   - Launch checklist
   - App signing
   - Play Store setup

**For specific features:**

6. **`implementation/04-SETUP_GUIDES/EDGE_FUNCTION_SETUP.md`**
   - Account deletion (GDPR compliance)
   - Optional for MVP

7. **`implementation/03-PRODUCTION/SECURITY_CHECKLIST.md`**
   - Security audit results
   - Hardening recommendations

8. **`implementation/03-PRODUCTION/CONTENT_MODERATION_STRATEGY.md`**
   - Community moderation approach
   - AI-powered content filtering
   - Handling spam and inappropriate content

---

## 🏗️ Architecture

### Clean Architecture Layers
```
Presentation (Compose + ViewModels + StateFlow)
    ↓
Domain (Use Cases + Interfaces)
    ↓
Data (Repositories + Supabase API)
```

### Tech Stack
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Remote DB:** Supabase (PostgreSQL)
- **Maps:** Mapbox Android SDK
- **Networking:** Ktor (Supabase client)
- **Async:** Coroutines + Flow
- **Serialization:** kotlinx.serialization

### ⚠️ Important Note
**Room database code exists but is NOT USED.** All fountain data comes from Supabase. See `CODE_ANALYSIS_ISSUES.md` for cleanup recommendations.

---

## 🔐 Credentials

### Never Commit These Files:
- `local.properties` (contains API keys)
- `keystore.properties` (if exists)
- `logs/` directory

### Required in `local.properties`:
```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_KEY=eyJhbGc...
MAPBOX_PUBLIC_TOKEN=pk.xxxxx
```

### How They're Used:
```kotlin
// Build config reads from local.properties
buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")

// Code accesses via BuildConfig
val url = BuildConfig.SUPABASE_URL
```

---

## 📊 Database Schema

### Supabase PostgreSQL Tables:
- **`fountains`** - 1,745 Barcelona fountains (migrated to Supabase)
- **`profiles`** - User accounts (extends auth.users)
  - Fields: id, nickname, total_ratings, average_score, best_fountain_id, role
- **`reviews`** - Fountain reviews
  - Rating categories: taste, freshness, location_rating, aesthetics, splash, jet
  - Calculated: overall (average of 6 ratings)
- **`leaderboard`** (VIEW) - User rankings
- **`fountain_stats_detailed`** (VIEW) - Fountain aggregates with stats

### Key Features:
- Row Level Security (RLS) policies
- Automatic triggers for stats updates
- Generated columns (e.g., `overall` score)

---

## 🎨 UI Guidelines

### Navigation Flow
```
Map (start) ──→ Fountain Details ──→ Review Screen
Stats ────────→ (requires auth)
Leaderboard ──→ (public)
Profile ──────→ Login/Register
```

### Bottom Nav (Main Screens)
- **Map** | **Stats** | **Leaderboard** | **Profile**

### Multi-Language
**Always use string resources - NEVER hardcode text:**
```kotlin
✅ Text(text = stringResource(R.string.fountain_name))
❌ Text(text = "Fountain Name")
```

Supported: English (default), Spanish (`-es`), Catalan (`-ca`)

---

## 🔨 Development Guidelines

### Before Making Changes
1. ✅ Read `implementation/02-CURRENT_STATE/APP_STATUS.md` for current state
2. ✅ Read `implementation/02-CURRENT_STATE/KNOWN_ISSUES.md` for technical debt
3. ✅ Search for existing code (use Grep/SemanticSearch)
4. ✅ Check dependencies in `app/build.gradle.kts`
5. ✅ Follow existing patterns

### Naming Conventions
- **Screens:** `*Screen.kt` (MapScreen.kt)
- **ViewModels:** `*ViewModel.kt` (MapViewModel.kt)
- **Use Cases:** `*UseCase.kt` (GetFountainsUseCase.kt)
- **Repositories:** `*Repository.kt` + `*RepositoryImpl.kt`
- **DTOs:** `*Dto.kt` (ReviewDto.kt)

### Package Organization
```kotlin
com.watxaut.fontsreviewer
├── data          # Data sources & repositories
├── domain        # Business logic (pure Kotlin)
├── presentation  # UI & ViewModels
├── di            # Hilt modules
├── ui            # Theme
└── util          # Helpers
```

### Dependency Injection
```kotlin
// ✅ Constructor injection with Hilt
@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFountainsUseCase: GetFountainsUseCase
) : ViewModel()

// ✅ Repository implementation
@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    @ApplicationContext private val context: Context
) : FountainRepository
```

### State Management
```kotlin
// ✅ StateFlow for UI state
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Collect in Composables
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### Error Handling
```kotlin
// ✅ Use Result type
suspend fun getReviews(fountainId: String): Result<List<Review>> {
    return try {
        val reviews = api.fetchReviews(fountainId)
        Result.success(reviews)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 🚀 Common Tasks

### Add New Screen
1. Create `presentation/[feature]/[Feature]Screen.kt`
2. Create `presentation/[feature]/[Feature]ViewModel.kt`
3. Add route to `Screen.kt` sealed class
4. Update `NavGraph.kt` with composable
5. Add strings to all 3 language files (EN, ES, CA)

### Add New Use Case
1. Create `domain/usecase/[Action]UseCase.kt`
2. Use repository interfaces (not implementations)
3. Inject in ViewModel via Hilt
4. Follow single responsibility principle

### Add Supabase API Call
1. Define DTO in `data/remote/dto/[Entity]Dto.kt` with `@Serializable`
2. Add method to `SupabaseService`
3. Update repository implementation
4. Map DTO to domain model

### Add Localized String
```xml
<!-- values/strings.xml -->
<string name="submit_review">Submit Review</string>

<!-- values-es/strings.xml -->
<string name="submit_review">Enviar Reseña</string>

<!-- values-ca/strings.xml -->
<string name="submit_review">Enviar Ressenya</string>
```

---

## ⚠️ Known Issues

See **`implementation/02-CURRENT_STATE/KNOWN_ISSUES.md`** for detailed analysis.

### High Priority:
1. **Room Database Dead Code** - Entire `data/local/` directory unused (fountains now in Supabase)
2. **MapViewModel Architecture Violation** - Directly injects SupabaseService instead of using use case
3. **Empty Components Directory** - Should be deleted

### Action Required:
- Remove Room database code (~7 files)
- Create `GetUserReviewedFountainsUseCase`
- Update MapViewModel to follow proper architecture

**None are launch blockers** - app works fine, these are code quality improvements.

**See full report in `implementation/02-CURRENT_STATE/KNOWN_ISSUES.md`**

---

## 🐛 Common Pitfalls

### DON'T:
- ❌ Hardcode strings (use `stringResource()`)
- ❌ Mix architecture layers
- ❌ Commit `local.properties` or credentials
- ❌ Use Room database (it's dead code - use Supabase)
- ❌ Create duplicate code (search first!)
- ❌ Skip error handling
- ❌ Ignore i18n (support all 3 languages)

### DO:
- ✅ Read implementation docs first (start with APP_STATUS.md)
- ✅ Follow Clean Architecture
- ✅ Use Hilt for dependency injection
- ✅ Add strings in all 3 languages
- ✅ Handle errors with Result<T>
- ✅ Use StateFlow for UI state
- ✅ Test critical flows
- ✅ Check `KNOWN_ISSUES.md` before coding

---

## 📈 Project Status

### ✅ Completed (MVP Ready)
- [x] All 8 screens implemented
- [x] Supabase backend configured
- [x] Authentication working
- [x] Review system functional
- [x] Map with 1,745 fountains
- [x] Stats and leaderboard
- [x] Multi-language support (EN/ES/CA)
- [x] Material 3 design
- [x] Navigation setup
- [x] Hilt DI configured

### ⚠️ Before Production Launch (Not Blockers)
- [ ] App signing (keystore generation) - 30 min
- [ ] Privacy policy (write + host) - 2 hours
- [ ] Play Store listing (screenshots + descriptions) - 3 hours
- [ ] Remove Room database dead code (optional cleanup)
- [ ] Fix MapViewModel architecture (optional cleanup)
- [ ] Add unit tests (post-launch)

**Full status:** See `implementation/02-CURRENT_STATE/APP_STATUS.md`

**Launch timeline:** 1-2 weeks to Play Store live

---

## 📞 Quick Reference

### Build Commands
```bash
# Build
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Clean build
./gradlew clean build

# Run tests
./gradlew test
```

### Project Metrics
- **Kotlin Files:** 57
- **ViewModels:** 8
- **Screens:** 8
- **Use Cases:** 7
- **Repositories:** 3
- **Overall Grade:** B+ (85/100)

### Dependencies (Key)
- Jetpack Compose BOM
- Hilt 2.50
- Supabase BOM 2.5.4
- Mapbox Maps 11.3.0
- Ktor Client 2.3.11
- Navigation Compose 2.7.7

---

## 🎓 Resources

### Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Supabase Kotlin](https://supabase.com/docs/reference/kotlin)
- [Mapbox Android](https://docs.mapbox.com/android/)
- [Clean Architecture](https://developer.android.com/topic/architecture)

### Project Docs
- Current Status: `implementation/02-CURRENT_STATE/APP_STATUS.md`
- Database Migrations: `implementation/01-MIGRATIONS/MIGRATION_HISTORY.md`
- Known Issues: `implementation/02-CURRENT_STATE/KNOWN_ISSUES.md`
- Backend Setup: `implementation/04-SETUP_GUIDES/SUPABASE_SETUP.md`
- Deployment: `implementation/03-PRODUCTION/DEPLOYMENT_GUIDE.md`
- Security: `implementation/03-PRODUCTION/SECURITY_CHECKLIST.md`
- Content Moderation: `implementation/03-PRODUCTION/CONTENT_MODERATION_STRATEGY.md`

---

## 📝 Final Notes

**Project Status:** 🟢 **95% Production Ready** (1-2 weeks to launch)

**Key Strengths:**
- ✅ All 8 screens implemented and tested
- ✅ 1,745 fountains loaded in Supabase
- ✅ Solid Clean Architecture + MVVM
- ✅ Proper DI with Hilt
- ✅ Complete feature set (map, reviews, leaderboard, stats)
- ✅ Good error handling (Result<T> pattern)
- ✅ Multi-language support (EN/ES/CA)
- ✅ User roles (admin/operator)
- ✅ Security (RLS, HTTPS, ProGuard)

**Critical for Launch (5%):**
- 🔴 App signing configuration (30 min)
- 🔴 Privacy policy (2 hours)
- 🔴 Play Store listing (3 hours)

**Optional Code Quality Improvements:**
- 🟡 Remove Room dead code (~7 files, reduces APK size)
- 🟡 Fix MapViewModel pattern (architecture consistency)
- 🟢 Add unit tests (post-launch)

**Launch Timeline:**
- Week 1: Signing + privacy policy + Play Store setup
- Week 2: Beta testing with 10-20 users
- Week 3: Submit to production + Google review (3-7 days)
- Week 4: **LIVE ON PLAY STORE!** 🚀

**Last Updated:** 2025-10-12  
**License:** MIT  
**Maintainer:** watxaut

---

> 💡 **Quick Start for New AI Agents:**
> 1. Read `implementation/02-CURRENT_STATE/APP_STATUS.md` first - complete snapshot
> 2. Check `implementation/01-MIGRATIONS/MIGRATION_HISTORY.md` - understand database
> 3. Review `implementation/02-CURRENT_STATE/KNOWN_ISSUES.md` - technical debt
> 4. For deployment: `implementation/03-PRODUCTION/DEPLOYMENT_GUIDE.md`
> 
> **The app works great!** Only missing: app signing, privacy policy, and Play Store assets.
