# FontsReviewer - AI Agent Instructions

> **Quick Start for AI Agents:** Before making any changes, read the complete `implementation/` directory to understand the project architecture, current status, and implementation plans.

## 📋 Project Overview

**FontsReviewer** is an Android application for rating and reviewing 1,745 public fountains in Barcelona. The app allows anonymous browsing and authenticated user reviews with a social leaderboard system.

### Key Information
- **Platform:** Android (Kotlin, Jetpack Compose)
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36
- **Package:** `com.watxaut.fontsreviewer`
- **Languages:** English, Spanish, Catalan (full i18n support)
- **Backend:** Supabase (PostgreSQL + Auth)
- **Maps:** Mapbox Android SDK
- **Architecture:** MVVM + Clean Architecture + Hilt DI

---

## 🎯 Core Features

1. **Map View** - Display all 1,745 Barcelona fountains with clustering
2. **Anonymous Browsing** - View reviews without authentication
3. **User Authentication** - Email/password (Supabase Auth)
4. **Review System** - 4 rating categories per fountain:
   - Taste (Sabor / Sabor)
   - Freshness (Frescor / Frescor)
   - Location (Localització / Ubicación)
   - Aesthetics (Estètica / Estética)
5. **User Statistics** - Personal metrics and best-rated fountain
6. **Leaderboard** - Global user rankings by fountains rated

---

## 📁 Project Structure

```
FontsReviewer/
├── app/
│   ├── src/main/
│   │   ├── java/com/watxaut/fontsreviewer/
│   │   │   ├── data/                    # Data layer
│   │   │   │   ├── local/               # Room database
│   │   │   │   │   ├── dao/             # DAOs for local data
│   │   │   │   │   ├── database/        # Room database setup
│   │   │   │   │   └── entity/          # Room entities
│   │   │   │   ├── remote/              # Supabase integration
│   │   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   │   └── service/         # API services
│   │   │   │   ├── repository/          # Repository implementations
│   │   │   │   └── mapper/              # Entity <-> Domain mappers
│   │   │   ├── domain/                  # Business logic layer
│   │   │   │   ├── model/               # Domain models
│   │   │   │   ├── repository/          # Repository interfaces
│   │   │   │   └── usecase/             # Use cases
│   │   │   ├── presentation/            # UI layer
│   │   │   │   ├── auth/                # Login/Register screens
│   │   │   │   ├── map/                 # Map screen + ViewModel
│   │   │   │   ├── details/             # Fountain details
│   │   │   │   ├── review/              # Review submission
│   │   │   │   ├── stats/               # User statistics
│   │   │   │   ├── leaderboard/         # Global rankings
│   │   │   │   ├── profile/             # User profile
│   │   │   │   └── navigation/          # Navigation setup
│   │   │   ├── di/                      # Dependency injection modules
│   │   │   ├── ui/                      # Theme and common UI
│   │   │   ├── util/                    # Utilities (CSV parser, etc.)
│   │   │   ├── FontsReviewerApp.kt      # Application class
│   │   │   └── MainActivity.kt          # Main activity
│   │   ├── assets/
│   │   │   └── 2025_fonts_bcn.csv       # 1,745 fountain data
│   │   └── res/
│   │       ├── values/                   # English strings
│   │       ├── values-es/                # Spanish strings
│   │       └── values-ca/                # Catalan strings
│   └── build.gradle.kts                  # App-level Gradle config
├── implementation/                       # 📖 READ THIS FIRST!
│   ├── IMPLEMENTATION_PLANS.md           # Detailed architecture plans
│   ├── IMPLEMENTATION_STATUS.md          # Current implementation status
│   └── SUPABASE_IMPLEMENTATION_GUIDE.md  # Backend setup guide
├── build.gradle.kts                      # Project-level Gradle config
├── settings.gradle.kts                   # Gradle settings
├── local.properties                      # 🔒 SECRET CREDENTIALS (gitignored)
├── .gitignore                            # Git ignore rules
└── agents.md                             # This file
```

---

## 🔍 Essential Reading for AI Agents

Before making any changes, **YOU MUST READ** these files in order:

1. **`implementation/IMPLEMENTATION_STATUS.md`**
   - Current implementation progress
   - What's completed vs. what's pending
   - Known issues and blockers

2. **`implementation/IMPLEMENTATION_PLANS.md`**
   - Complete architecture overview
   - Technology decisions and rationale
   - Database schemas (local + remote)
   - Alternative approaches considered

3. **`implementation/SUPABASE_IMPLEMENTATION_GUIDE.md`**
   - Backend setup instructions
   - SQL schemas and RLS policies
   - Repository patterns and examples
   - Phase-by-phase implementation guide

4. **`MAPBOX_SETUP_INSTRUCTIONS.md`** (in project root)
   - Map integration setup
   - Token configuration
   - Troubleshooting guide

---

## 🏗️ Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────────────┐
│         Presentation Layer                   │
│  (Jetpack Compose + ViewModels + StateFlow) │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│           Domain Layer                       │
│   (Business Logic + Use Cases + Interfaces) │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│            Data Layer                        │
│  (Repositories + Room DB + Supabase API)    │
└─────────────────────────────────────────────┘
```

### Key Technologies

- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt (Dagger)
- **Navigation:** Navigation Compose
- **Local DB:** Room (for fountain data cache)
- **Remote API:** Supabase (PostgreSQL + Auth)
- **Maps:** Mapbox Android SDK
- **Networking:** Ktor (Supabase client)
- **Async:** Kotlin Coroutines + Flow
- **Serialization:** kotlinx.serialization

---

## 🔐 Security & Credentials

### ⚠️ CRITICAL: Never Commit Secrets!

The following files contain sensitive credentials and **MUST NEVER** be committed:

1. **`local.properties`** - Contains:
   - `SUPABASE_URL` - Backend database URL
   - `SUPABASE_KEY` - Anon API key
   - `MAPBOX_PUBLIC_TOKEN` - Public map token
   - `MAPBOX_DOWNLOADS_TOKEN` - Secret download token (critical!)

2. **`app/src/main/res/values/supabase_config.xml`** - If it exists (currently not used)

3. **`logs/`** - May contain runtime secrets

### ✅ These are already in `.gitignore` - verify before any commit!

### How Credentials Are Used

```kotlin
// In app/build.gradle.kts - loaded from local.properties
buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"$mapboxPublicToken\"")

// In code - accessed via BuildConfig
val supabaseUrl = BuildConfig.SUPABASE_URL
val mapboxToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
```

---

## 📊 Database Architecture

### Local Database (Room)

**Purpose:** Offline-first fountain data storage

```kotlin
// Entity: FountainEntity
@Entity(tableName = "fountains")
data class FountainEntity(
    @PrimaryKey val codi: String,
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double
)
```

**Location:** `app/src/main/java/com/watxaut/fontsreviewer/data/local/`

### Remote Database (Supabase PostgreSQL)

**Tables:**
- `profiles` - User accounts (extends `auth.users`)
- `reviews` - Fountain reviews with 4 rating categories
- `leaderboard` (VIEW) - User rankings
- `fountain_stats` (VIEW) - Aggregated fountain metrics

**Key Features:**
- Row Level Security (RLS) policies
- Automatic triggers for stats updates
- Foreign key constraints
- Calculated fields (e.g., `overall` score)

**Location:** See `implementation/SUPABASE_IMPLEMENTATION_GUIDE.md` for SQL schema

---

## 🎨 UI/UX Guidelines

### Screens & Navigation

```
Map (default) ────┬──→ Fountain Details ──→ Review Screen
Stats ────────────┤
Leaderboard ──────┤
Profile ──────────┴──→ Login ──→ Register
```

### Bottom Navigation (Main Screens)
1. **Map** - Browse all fountains
2. **Stats** - Personal statistics (requires auth)
3. **Leaderboard** - Global rankings
4. **Profile** - User account/settings

### Material 3 Design
- Use `FontsReviewerTheme` wrapper
- Follow Material 3 color scheme
- Support dynamic colors
- Implement proper loading/error states

### Multi-Language Support

**Strings are localized in 3 languages:**
- `res/values/strings.xml` (English - default)
- `res/values-es/strings.xml` (Spanish)
- `res/values-ca/strings.xml` (Catalan)

**Always use string resources:**
```kotlin
// ✅ Correct
Text(text = stringResource(R.string.fountain_name))

// ❌ Wrong - never hardcode strings
Text(text = "Fountain Name")
```

---

## 🔨 Development Guidelines for AI Agents

### Before Making Changes

1. **Read implementation docs** (see Essential Reading section)
2. **Check current status** in `IMPLEMENTATION_STATUS.md`
3. **Search for existing code** - don't recreate what exists
4. **Verify dependencies** in `app/build.gradle.kts`
5. **Check for TODO comments** in the codebase

### Code Style & Conventions

#### Naming Conventions
- **Screens:** `*Screen.kt` (e.g., `MapScreen.kt`)
- **ViewModels:** `*ViewModel.kt` (e.g., `MapViewModel.kt`)
- **Use Cases:** `*UseCase.kt` (e.g., `GetFountainsUseCase.kt`)
- **Repositories:** `*Repository.kt` interface, `*RepositoryImpl.kt` implementation
- **DTOs:** `*Dto.kt` (e.g., `ReviewDto.kt`)
- **Entities:** `*Entity.kt` (e.g., `FountainEntity.kt`)

#### Package Organization
```
com.watxaut.fontsreviewer
├── data          # Data sources and repositories
├── domain        # Business logic (pure Kotlin)
├── presentation  # UI and ViewModels
├── di            # Dependency injection
├── ui            # Theme and reusable UI
└── util          # Helper functions
```

#### Dependency Injection Pattern
```kotlin
// ✅ Use constructor injection with Hilt
@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFountainsUseCase: GetFountainsUseCase
) : ViewModel()

// Repository implementations
@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val fountainDao: FountainDao,
    @ApplicationContext private val context: Context
) : FountainRepository
```

#### State Management
```kotlin
// ✅ Use StateFlow for UI state
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Collect in Composables
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

#### Error Handling
```kotlin
// ✅ Use Result type for operations that can fail
suspend fun getReviews(fountainId: String): Result<List<Review>> {
    return try {
        val reviews = api.fetchReviews(fountainId)
        Result.success(reviews)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Testing Guidelines

**Unit Tests:** `app/src/test/java/`
- Test use cases and ViewModels
- Mock repositories with MockK

**UI Tests:** `app/src/androidTest/java/`
- Test navigation flows
- Test user interactions

**Dependencies Available:**
- JUnit 4
- Kotlin Coroutines Test
- MockK
- Espresso
- Compose UI Test

---

## 🚀 Common Tasks for AI Agents

### Task: Add a New Screen

1. **Create UI file:** `presentation/[feature]/[Feature]Screen.kt`
2. **Create ViewModel:** `presentation/[feature]/[Feature]ViewModel.kt`
3. **Add to navigation:**
   - Add route to `Screen.kt`
   - Update `NavGraph.kt`
4. **Add strings:** Update all 3 language files (EN, ES, CA)
5. **Test:** Verify navigation and state management

### Task: Add a New Use Case

1. **Create interface:** `domain/usecase/[Action]UseCase.kt`
2. **Implement logic:** Use repository interfaces, not implementations
3. **Inject in ViewModel:** Constructor injection via Hilt
4. **Test:** Create unit test in `test/` directory

### Task: Add Supabase API Call

1. **Define DTO:** `data/remote/dto/[Entity]Dto.kt` with `@Serializable`
2. **Add method to `SupabaseService`:**
   ```kotlin
   suspend fun getEntity(): Result<EntityDto> {
       return try {
           val result = client.from("table").select().decodeSingle<EntityDto>()
           Result.success(result)
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```
3. **Update repository:** Call service in repository implementation
4. **Map to domain:** Use mapper to convert DTO → Domain model

### Task: Fix Build Issues

1. **Check dependencies:** `app/build.gradle.kts` and `gradle/libs.versions.toml`
2. **Verify credentials:** Ensure `local.properties` has all required keys
3. **Sync Gradle:** File → Sync Project with Gradle Files
4. **Clean build:** `./gradlew clean build`
5. **Check logs:** Look for specific error messages

### Task: Add Localized String

1. **English:** Add to `res/values/strings.xml`
2. **Spanish:** Add to `res/values-es/strings.xml`
3. **Catalan:** Add to `res/values-ca/strings.xml`
4. **Use in code:** `stringResource(R.string.your_key)`

**Example:**
```xml
<!-- values/strings.xml -->
<string name="submit_review">Submit Review</string>

<!-- values-es/strings.xml -->
<string name="submit_review">Enviar Reseña</string>

<!-- values-ca/strings.xml -->
<string name="submit_review">Enviar Ressenya</string>
```

---

## 🐛 Known Issues & Gotchas

### Supabase Auth Trigger
The `handle_new_user()` function **MUST** have `SECURITY DEFINER` to bypass RLS during signup. Without it, profile creation fails.

### Mapbox Token Setup
Two tokens required:
- **Public token** (`pk.*`) - For displaying maps
- **Secret token** (`sk.*`) - For Gradle downloads (needs `downloads:read` scope)

### Room Database Initialization
Fountains are loaded **lazily** on first app launch from `2025_fonts_bcn.csv`. Check logs if markers don't appear.

### Navigation State
Bottom nav is only visible on main screens (Map, Stats, Leaderboard, Profile). Detail screens hide it automatically.

---

## 📦 Dependencies Overview

### Core Dependencies (from `app/build.gradle.kts`)

```kotlin
// Jetpack Compose + Material 3
implementation(libs.androidx.compose.bom)
implementation(libs.androidx.material3)

// Hilt Dependency Injection
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)

// Room Database
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)

// Supabase (BOM ensures version compatibility)
implementation(platform(libs.supabase.bom))
implementation(libs.supabase.postgrest.kt)
implementation(libs.supabase.auth.kt)

// Ktor (required by Supabase)
implementation(libs.ktor.client.android)
implementation(libs.ktor.client.core)

// Mapbox Maps
implementation(libs.mapbox.maps)
implementation(libs.mapbox.compose)

// Navigation
implementation(libs.navigation.compose)

// Coil (image loading)
implementation(libs.coil.compose)
```

### Version Catalog
Dependency versions are centralized in `gradle/libs.versions.toml`

---

## 🔄 Data Flow Example

### Submitting a Review

```
User Input (ReviewScreen)
    ↓
ReviewViewModel.submitReview()
    ↓
SubmitReviewUseCase.invoke()
    ↓
ReviewRepository.submitReview()
    ↓
SupabaseService.createReview()
    ↓
Supabase API (PostgreSQL)
    ↓
Trigger: update_user_stats()
    ↓
profiles.total_ratings++
```

### Loading Fountains

```
App Startup (FontsReviewerApp)
    ↓
InitializeFountainsUseCase()
    ↓
FountainRepository.initializeFountains()
    ↓
Check: fountainDao.getFountainCount()
    ↓ (if 0)
CsvParser.parseFountainsFromAssets()
    ↓
fountainDao.insertAll()
    ↓
MapScreen observes Flow<List<Fountain>>
```

---

## 🎯 Completion Checklist

### Phase 1: Backend Setup ✅
- [x] Supabase project created
- [x] SQL schema executed
- [x] RLS policies configured
- [x] Auth configured

### Phase 2: Data Layer ✅
- [x] Room database setup
- [x] Supabase integration
- [x] Repository pattern implemented
- [x] Use cases created

### Phase 3: UI Implementation ✅
- [x] Map screen with Mapbox
- [x] Login/Register screens
- [x] Fountain details screen
- [x] Review submission screen
- [x] Stats screen
- [x] Leaderboard screen
- [x] Profile screen
- [x] Bottom navigation

### Phase 4: Polish & Testing ⏳
- [ ] Unit tests for use cases
- [ ] UI tests for critical flows
- [ ] Error handling refinement
- [ ] Loading states optimization
- [ ] Offline mode improvements
- [ ] Performance optimization

---

## 📞 Getting Help

### For AI Agents Working on This Project

1. **Read the implementation docs first** - Most questions are answered there
2. **Check existing code** - Search before creating new components
3. **Follow Clean Architecture** - Don't mix layers
4. **Use Hilt DI** - Constructor injection everywhere
5. **Test your changes** - Build and run before committing

### Documentation Hierarchy
```
1. implementation/IMPLEMENTATION_STATUS.md  ← Start here
2. implementation/IMPLEMENTATION_PLANS.md   ← Architecture details
3. implementation/SUPABASE_IMPLEMENTATION_GUIDE.md ← Backend guide
4. agents.md (this file)                    ← AI agent reference
```

### Useful Commands

```bash
# Build the project
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on device
./gradlew installDebug

# Clean build
./gradlew clean build

# Check for unused dependencies
./gradlew buildHealth
```

---

## 🎓 Learning Resources

### Jetpack Compose
- [Official Compose Docs](https://developer.android.com/jetpack/compose)
- [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)

### Supabase
- [Supabase Docs](https://supabase.com/docs)
- [Kotlin Client Docs](https://supabase.com/docs/reference/kotlin)

### Mapbox
- [Android SDK Docs](https://docs.mapbox.com/android/)
- [Compose Integration](https://docs.mapbox.com/android/maps/guides/compose/)

### Clean Architecture
- [Uncle Bob's Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Clean Architecture Guide](https://developer.android.com/topic/architecture)

---

## 📝 Final Notes for AI Agents

### DO's ✅
- ✅ Read `implementation/` docs before making changes
- ✅ Follow Clean Architecture principles
- ✅ Use dependency injection (Hilt)
- ✅ Add strings in all 3 languages (EN, ES, CA)
- ✅ Handle errors with Result type
- ✅ Use StateFlow for UI state
- ✅ Test critical user flows
- ✅ Verify credentials in `local.properties`

### DON'Ts ❌
- ❌ Commit `local.properties` or secrets
- ❌ Hardcode strings (use string resources)
- ❌ Mix architecture layers
- ❌ Create duplicate code (search first!)
- ❌ Skip reading implementation docs
- ❌ Use deprecated Android APIs
- ❌ Ignore error handling
- ❌ Break existing functionality

---

**Project Status:** ✅ MVP Complete - Ready for Testing & Polish

**Last Updated:** 2025-10-11

**Maintainer:** watxaut

**License:** [Add license information]

---

> 💡 **Pro Tip for AI Agents:** When in doubt, check `implementation/IMPLEMENTATION_STATUS.md` for current progress and known issues. This file is updated as the project evolves.
