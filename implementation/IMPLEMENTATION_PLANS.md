# Barcelona Fountains Reviewer App - Implementation Plans

## Project Overview

**App Name:** FontsReviewer
**Target:** Barcelona fountains rating and review application
**Platform:** Android (minSdk 29 - Android 10)
**Data Source:** ~1745 fountains from 2025_fonts_bcn.csv
**Scale:** <10 concurrent users, max 100 monthly users
**Languages:** English, Spanish, Catalan

### Core Features
1. **Map View** - Main screen showing all Barcelona fountains
2. **Anonymous Browsing** - View reviews without login
3. **User Authentication** - Nickname + Password only
4. **Review System** - 4 rating categories per fountain:
   - Taste (Sabor / Sabor)
   - Freshness (Frescor / Frescor)
   - Location (Localització / Ubicación)
   - Aesthetics (Estètica / Estética)
5. **User Statistics** - Personal stats page showing:
   - Fountains rated count
   - Average scores
   - Best rated fountain (starred on map)
6. **Leaderboard** - Global ranking of users by fountains rated

---

## Plan A: Firebase Backend Solution

### Architecture Overview
```
Android App (Jetpack Compose)
    ↓
Firebase Services
    ├── Firestore Database
    ├── Firebase Authentication
    └── Cloud Functions (optional)
```

### Technology Stack

#### Android App
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt/Koin
- **Navigation:** Compose Navigation
- **Map:** Google Maps Compose SDK
- **Networking:** Firebase SDK
- **Local Storage:** Room Database (offline fountain data)
- **Image Loading:** Coil
- **State Management:** StateFlow/SharedFlow

#### Backend (Firebase)
- **Database:** Cloud Firestore
- **Authentication:** Firebase Auth (Email/Password)
- **Storage:** Fountain data embedded in app
- **Hosting:** Firebase Hosting (for web version if needed)

### Database Schema (Firestore)

```
users/
  {userId}/
    nickname: string
    createdAt: timestamp
    totalRatings: number
    averageScore: number
    bestFountainId: string

reviews/
  {reviewId}/
    fountainId: string
    userId: string
    userNickname: string
    taste: number (1-5)
    freshness: number (1-5)
    location: number (1-5)
    aesthetics: number (1-5)
    overall: number (calculated)
    comment: string (optional)
    createdAt: timestamp
    updatedAt: timestamp

fountains/ (optional - can be local only)
  {fountainId}/
    codi: string
    nom: string
    carrer: string
    latitude: number
    longitude: number
    averageRating: number
    totalReviews: number
```

### Android App Structure

```
app/src/main/java/com/watxaut/fontsreviewer/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── FountainDao.kt
│   │   ├── database/
│   │   │   └── AppDatabase.kt
│   │   └── entity/
│   │       └── FountainEntity.kt
│   ├── remote/
│   │   ├── FirebaseAuthService.kt
│   │   └── FirestoreService.kt
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── FountainRepository.kt
│   │   └── ReviewRepository.kt
│   └── model/
│       ├── User.kt
│       ├── Fountain.kt
│       └── Review.kt
├── domain/
│   ├── usecase/
│   │   ├── LoginUseCase.kt
│   │   ├── RegisterUseCase.kt
│   │   ├── GetFountainsUseCase.kt
│   │   ├── SubmitReviewUseCase.kt
│   │   ├── GetUserStatsUseCase.kt
│   │   └── GetLeaderboardUseCase.kt
│   └── repository/ (interfaces)
├── presentation/
│   ├── map/
│   │   ├── MapScreen.kt
│   │   ├── MapViewModel.kt
│   │   └── components/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   └── AuthViewModel.kt
│   ├── review/
│   │   ├── ReviewScreen.kt
│   │   ├── ReviewListScreen.kt
│   │   └── ReviewViewModel.kt
│   ├── stats/
│   │   ├── StatsScreen.kt
│   │   └── StatsViewModel.kt
│   ├── leaderboard/
│   │   ├── LeaderboardScreen.kt
│   │   └── LeaderboardViewModel.kt
│   └── navigation/
│       └── NavGraph.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── FirebaseModule.kt
└── util/
    ├── LocalizationHelper.kt
    ├── CsvParser.kt
    └── Constants.kt
```

### Dependencies (build.gradle.kts)

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// Google Maps
implementation("com.google.maps.android:maps-compose:4.3.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Coil for images
implementation("io.coil-kt:coil-compose:2.5.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### Multi-language Support

Create string resources:
```
res/
├── values/strings.xml (English)
├── values-es/strings.xml (Spanish)
└── values-ca/strings.xml (Catalan)
```

Example strings.xml:
```xml
<resources>
    <string name="app_name">Fonts Reviewer</string>
    <string name="taste">Taste</string>
    <string name="freshness">Freshness</string>
    <string name="location">Location</string>
    <string name="aesthetics">Aesthetics</string>
    <string name="login">Login</string>
    <string name="register">Register</string>
    <string name="nickname">Nickname</string>
    <string name="password">Password</string>
    <string name="submit_review">Submit Review</string>
    <string name="my_stats">My Statistics</string>
    <string name="leaderboard">Leaderboard</string>
    <string name="fountains_rated">Fountains Rated</string>
    <string name="average_score">Average Score</string>
    <string name="best_fountain">Best Fountain</string>
</resources>
```

### Implementation Steps

1. **Setup Firebase Project**
   - Create Firebase project at console.firebase.google.com
   - Add Android app with package name
   - Download google-services.json
   - Enable Firebase Authentication (Email/Password)
   - Create Firestore database (start in test mode, then add security rules)

2. **Security Rules (Firestore)**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read their own data
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Reviews are readable by anyone, writable by authenticated users
    match /reviews/{reviewId} {
      allow read: if true;
      allow create: if request.auth != null &&
                      request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null &&
                              resource.data.userId == request.auth.uid;
    }

    // Fountains are read-only (if using Firestore)
    match /fountains/{fountainId} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

3. **Parse CSV and Store Locally**
   - Create Room database for fountains
   - Parse CSV on first launch
   - Insert all fountains into local DB
   - Use WorkManager for background sync if needed

4. **Implement Core Features**
   - Map screen with Google Maps
   - Marker clustering for 1745 fountains
   - Click fountain → show reviews
   - Login/Register flow
   - Review submission form
   - Stats calculation
   - Leaderboard with Firestore queries

5. **Google Maps API Key**
   - Get API key from Google Cloud Console
   - Enable Maps SDK for Android
   - Add to AndroidManifest.xml:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

### Cost Analysis (Plan A)

**Firebase Free Tier:**
- **Firestore:**
  - 50K reads/day
  - 20K writes/day
  - 1 GB storage
  - Estimated usage: ~100 users × 50 reviews = 5K writes/month ✓

- **Authentication:**
  - Unlimited for Email/Password

- **Bandwidth:**
  - 10 GB/month download
  - 1 GB/month upload

**Google Maps:**
- Dynamic Maps: $7/1000 loads (First 28,500/month FREE)
- Estimated: <10 users × 30 days × 10 opens = 3,000 loads/month ✓ FREE

**Total Monthly Cost:** $0 (within free tier)

### Pros & Cons

#### Pros
✓ Zero infrastructure management
✓ Real-time data sync
✓ Excellent Android SDK support
✓ Built-in authentication
✓ Generous free tier
✓ Auto-scaling
✓ Good documentation
✓ Offline support with Firestore cache

#### Cons
✗ Vendor lock-in (Google)
✗ Limited query capabilities (Firestore)
✗ Costs can spike unexpectedly if app grows
✗ Google Maps requires credit card for API key
✗ Less control over backend logic

---

## Plan B: Supabase Backend Solution

### Architecture Overview
```
Android App (Jetpack Compose)
    ↓
Supabase Services
    ├── PostgreSQL Database
    ├── Supabase Auth
    ├── REST API / Realtime
    └── Storage (if needed)
```

### Technology Stack

#### Android App
- Same as Plan A (Jetpack Compose, MVVM, etc.)
- **Networking:** Supabase Kotlin SDK + Ktor
- **Map:** Google Maps or Mapbox (more free tier friendly)

#### Backend (Supabase)
- **Database:** PostgreSQL
- **Authentication:** Supabase Auth
- **API:** Auto-generated REST + Realtime subscriptions
- **Hosting:** Supabase Cloud (free tier)

### Database Schema (PostgreSQL)

```sql
-- Users table (extends Supabase auth.users)
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  nickname TEXT UNIQUE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  total_ratings INTEGER DEFAULT 0,
  average_score DECIMAL(3,2),
  best_fountain_id TEXT
);

-- Reviews table
CREATE TABLE reviews (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  fountain_id TEXT NOT NULL,
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
  user_nickname TEXT NOT NULL,
  taste INTEGER CHECK (taste >= 1 AND taste <= 5),
  freshness INTEGER CHECK (freshness >= 1 AND freshness <= 5),
  location_rating INTEGER CHECK (location_rating >= 1 AND location_rating <= 5),
  aesthetics INTEGER CHECK (aesthetics >= 1 AND aesthetics <= 5),
  overall DECIMAL(3,2) GENERATED ALWAYS AS ((taste + freshness + location_rating + aesthetics) / 4.0) STORED,
  comment TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(fountain_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_reviews_fountain ON reviews(fountain_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
CREATE INDEX idx_reviews_created ON reviews(created_at DESC);

-- Function to update user stats
CREATE OR REPLACE FUNCTION update_user_stats()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE profiles
  SET
    total_ratings = (SELECT COUNT(*) FROM reviews WHERE user_id = NEW.user_id),
    average_score = (SELECT AVG(overall) FROM reviews WHERE user_id = NEW.user_id),
    best_fountain_id = (SELECT fountain_id FROM reviews WHERE user_id = NEW.user_id ORDER BY overall DESC LIMIT 1)
  WHERE id = NEW.user_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update stats
CREATE TRIGGER trigger_update_stats
AFTER INSERT OR UPDATE OR DELETE ON reviews
FOR EACH ROW EXECUTE FUNCTION update_user_stats();

-- View for leaderboard
CREATE VIEW leaderboard AS
SELECT
  nickname,
  total_ratings,
  average_score,
  RANK() OVER (ORDER BY total_ratings DESC, average_score DESC) as rank
FROM profiles
WHERE total_ratings > 0
ORDER BY rank;
```

### Row Level Security (RLS) Policies

```sql
-- Enable RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Profiles are viewable by everyone"
  ON profiles FOR SELECT
  USING (true);

CREATE POLICY "Users can update own profile"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

-- Reviews policies
CREATE POLICY "Reviews are viewable by everyone"
  ON reviews FOR SELECT
  USING (true);

CREATE POLICY "Authenticated users can create reviews"
  ON reviews FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own reviews"
  ON reviews FOR UPDATE
  USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own reviews"
  ON reviews FOR DELETE
  USING (auth.uid() = user_id);
```

### Android App Integration

#### Supabase Setup
```kotlin
// In your Application class or DI module
val supabase = createSupabaseClient(
    supabaseUrl = "YOUR_SUPABASE_URL",
    supabaseKey = "YOUR_SUPABASE_ANON_KEY"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}
```

#### Repository Example
```kotlin
class SupabaseReviewRepository(
    private val supabase: SupabaseClient
) : ReviewRepository {

    override suspend fun submitReview(review: Review): Result<Unit> {
        return try {
            supabase.from("reviews")
                .insert(review.toJson())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewsForFountain(fountainId: String): List<Review> {
        return supabase.from("reviews")
            .select()
            .eq("fountain_id", fountainId)
            .order("created_at", ascending = false)
            .decodeList<Review>()
    }

    override suspend fun getUserStats(userId: String): UserStats {
        return supabase.from("profiles")
            .select()
            .eq("id", userId)
            .decodeSingle<UserStats>()
    }

    override suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return supabase.from("leaderboard")
            .select()
            .limit(100)
            .decodeList<LeaderboardEntry>()
    }
}
```

### Dependencies (build.gradle.kts)

```kotlin
// Supabase
implementation(platform("io.github.jan-tennert.supabase:bom:2.0.0"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.github.jan-tennert.supabase:realtime-kt")

// Ktor (required by Supabase)
implementation("io.ktor:ktor-client-android:2.3.7")
implementation("io.ktor:ktor-client-core:2.3.7")

// Mapbox (free alternative to Google Maps)
implementation("com.mapbox.maps:android:11.0.0")

// Everything else same as Plan A
```

### Implementation Steps

1. **Create Supabase Project**
   - Sign up at supabase.com
   - Create new project (choose free tier)
   - Get URL and anon key from Settings → API
   - Execute SQL schema in SQL Editor

2. **Configure Authentication**
   - Enable Email provider in Auth settings
   - Disable email confirmation for simplicity
   - Set up custom SMTP (optional, or use Supabase default)

3. **Fountain Data Storage**
   - Option 1: Store fountains in PostgreSQL table (slower initial load)
   - Option 2: Bundle CSV in app + local Room DB (recommended)
   - Option 3: Hybrid - store in Supabase but cache locally

4. **Map Integration**
   - Use Mapbox instead of Google Maps (50K free map loads/month)
   - Or stick with Google Maps if preferred

5. **Implement Same Features as Plan A**
   - Use Supabase SDK instead of Firebase
   - Leverage PostgreSQL views for complex queries
   - Real-time subscriptions for live review updates (optional)

### Cost Analysis (Plan B)

**Supabase Free Tier:**
- **Database:** 500 MB storage (plenty for user data)
- **Auth:** Unlimited users
- **API Requests:** 50K/month
- **Bandwidth:** 2 GB egress/month
- **Realtime:** 200 concurrent connections

**Mapbox (if used instead of Google Maps):**
- 50K free map loads/month
- Much better than Google Maps free tier

**Total Monthly Cost:** $0 (within free tier)

### Pros & Cons

#### Pros
✓ Open source (can self-host if needed)
✓ PostgreSQL = powerful queries & relations
✓ Better free tier than Firebase
✓ Real-time subscriptions
✓ Automatic REST API generation
✓ Better data portability
✓ SQL triggers for business logic
✓ No credit card required
✓ Can use Mapbox for better free maps

#### Cons
✗ Smaller community than Firebase
✗ Less mature mobile SDKs
✗ Fewer tutorials/resources
✗ More manual setup (SQL schema)
✗ No offline-first like Firestore

---

## Plan C: Appwrite Backend Solution

### Architecture Overview
```
Android App (Jetpack Compose)
    ↓
Appwrite Cloud/Self-hosted
    ├── Database (Collections)
    ├── Authentication
    ├── Functions (optional)
    └── Storage
```

### Technology Stack

#### Android App
- Same as Plans A & B
- **Networking:** Appwrite Android SDK

#### Backend (Appwrite)
- **Database:** Document database (similar to Firestore)
- **Authentication:** Appwrite Auth
- **Hosting:** Appwrite Cloud (free tier) or self-host

### Database Schema (Appwrite Collections)

**Users Collection (extends built-in auth)**
```json
{
  "nickname": "string (required, unique)",
  "totalRatings": "integer (default: 0)",
  "averageScore": "float",
  "bestFountainId": "string"
}
```

**Reviews Collection**
```json
{
  "fountainId": "string (required, indexed)",
  "userId": "string (required, indexed)",
  "userNickname": "string (required)",
  "taste": "integer (1-5)",
  "freshness": "integer (1-5)",
  "locationRating": "integer (1-5)",
  "aesthetics": "integer (1-5)",
  "overall": "float (calculated)",
  "comment": "string (optional)",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### Android Integration

```kotlin
// Initialize Appwrite
val client = Client(context)
    .setEndpoint("https://cloud.appwrite.io/v1")
    .setProject("YOUR_PROJECT_ID")

val account = Account(client)
val databases = Databases(client)

// Repository example
class AppwriteReviewRepository(
    private val databases: Databases
) : ReviewRepository {

    override suspend fun submitReview(review: Review): Result<Unit> {
        return try {
            databases.createDocument(
                databaseId = "main",
                collectionId = "reviews",
                documentId = ID.unique(),
                data = review.toMap()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewsForFountain(fountainId: String): List<Review> {
        return databases.listDocuments(
            databaseId = "main",
            collectionId = "reviews",
            queries = listOf(
                Query.equal("fountainId", fountainId),
                Query.orderDesc("createdAt"),
                Query.limit(100)
            )
        ).documents.map { it.toReview() }
    }
}
```

### Dependencies

```kotlin
// Appwrite
implementation("io.appwrite:sdk-for-android:4.0.0")

// Rest same as previous plans
```

### Implementation Steps

1. Create Appwrite Cloud account or self-host
2. Create project and get credentials
3. Setup collections with permissions
4. Implement same features as other plans

### Cost Analysis (Plan C)

**Appwrite Cloud Free Tier:**
- 75K requests/month
- 2 GB bandwidth
- 2 GB storage
- Unlimited users

**Self-hosted Option:**
- Host on free services (Railway, Render, Oracle Cloud)
- Full control but requires maintenance

**Total Monthly Cost:** $0 (cloud free tier) or $0 (self-hosted on free tier VPS)

### Pros & Cons

#### Pros
✓ Open source & self-hostable
✓ All-in-one backend (auth, DB, storage, functions)
✓ Good Android SDK
✓ Simple dashboard UI
✓ Can self-host for full control
✓ Active development

#### Cons
✗ Smaller community
✗ Less mature than Firebase/Supabase
✗ Query capabilities limited vs SQL
✗ Self-hosting requires DevOps knowledge
✗ Free cloud tier more limited
✗ Fewer learning resources

---

## Comparison Matrix

| Feature | Plan A (Firebase) | Plan B (Supabase) | Plan C (Appwrite) |
|---------|------------------|-------------------|-------------------|
| **Setup Complexity** | Easy | Medium | Easy |
| **Free Tier Limits** | Good | Excellent | Good |
| **Query Power** | Limited | Excellent (SQL) | Limited |
| **Android SDK** | Excellent | Good | Good |
| **Community/Docs** | Excellent | Good | Fair |
| **Vendor Lock-in** | High | Low | Low |
| **Offline Support** | Excellent | Fair | Fair |
| **Real-time** | Yes | Yes | Yes |
| **Self-host Option** | No | Yes | Yes |
| **Credit Card Required** | Yes (for Maps) | No | No |
| **Maintenance** | Zero | Very Low | Low |
| **Data Portability** | Low | High | Medium |
| **Scalability** | Excellent | Excellent | Good |

---

## RECOMMENDATION: Plan B (Supabase)

### Why Supabase is the Best Choice

After analyzing all three options, **Plan B (Supabase)** is the optimal solution for this project:

#### 1. **Best Free Tier**
- No credit card required
- 50K API requests/month (plenty for 100 users)
- 500 MB database (sufficient for reviews only)
- Can use Mapbox for maps (50K free loads vs Google's paid tier)

#### 2. **PostgreSQL Power**
- Complex queries for leaderboard without limitations
- SQL triggers auto-calculate user stats
- Views for efficient leaderboard queries
- Proper relational data model
- Better data integrity with foreign keys & constraints

#### 3. **Easy Maintenance**
- Managed hosting (no server maintenance)
- Automatic backups
- Built-in security with Row Level Security
- Simple SQL migrations
- Good monitoring dashboard

#### 4. **Future-Proof**
- Open source (can migrate data easily)
- Can self-host if needed
- No vendor lock-in
- PostgreSQL is industry standard
- Active community

#### 5. **Developer Experience**
- Auto-generated REST API from schema
- Type-safe Kotlin SDK
- Real-time subscriptions (optional feature)
- Good documentation
- Built-in auth with JWT

#### 6. **Cost-Effective Scaling**
- If app grows beyond 100 users:
  - Supabase Pro: $25/month (100K requests)
  - Firebase Blaze: Variable, can spike unexpectedly
  - Appwrite: Self-host complexity increases

### Implementation Roadmap (Supabase)

**Phase 1: Backend Setup (Day 1)**
1. Create Supabase project
2. Execute SQL schema
3. Configure RLS policies
4. Test with API calls

**Phase 2: Android Core (Days 2-3)**
1. Setup Jetpack Compose + MVVM structure
2. Implement Room DB for fountains
3. Parse CSV and populate local DB
4. Create data layer with repositories

**Phase 3: Map Feature (Days 4-5)**
1. Integrate Mapbox Android SDK
2. Display all 1745 fountain markers
3. Implement marker clustering
4. Marker click → show fountain details
5. Display reviews for selected fountain

**Phase 4: Authentication (Day 6)**
1. Login screen
2. Registration screen
3. Supabase Auth integration
4. Session management
5. Protected routes

**Phase 5: Review System (Days 7-8)**
1. Review submission form
2. 4-category rating inputs
3. Submit to Supabase
4. Update local cache
5. Show user's own reviews

**Phase 6: Stats & Leaderboard (Days 9-10)**
1. User stats screen
2. Fetch from Supabase
3. Display personal metrics
4. Leaderboard screen
5. Star best fountain on map

**Phase 7: Localization (Day 11)**
1. Create strings.xml for all 3 languages
2. Test language switching
3. Format dates/numbers per locale

**Phase 8: Polish & Testing (Days 12-14)**
1. Error handling
2. Loading states
3. Offline mode
4. Unit tests
5. UI tests
6. Bug fixes

**Total Estimated Time: 14 days (solo developer)**

---

## Alternative: Quick Win Hybrid Approach

If you want fastest time-to-market, consider:

**Backend:** Supabase (recommended above)
**Maps:** OpenStreetMap + osmdroid library (100% free, no API key)
**Auth:** Supabase Auth (free unlimited)
**Hosting:** Bundle fountain CSV in app (no backend storage needed)

This eliminates:
- Google Maps API key setup
- Credit card requirements
- Any map costs
- Complex backend fountain management

Trade-offs:
- OSM maps less polished than Google/Mapbox
- App size increases (~500KB for CSV)
- No fountain data updates without app update

---

## Next Steps

1. **Review this document** and confirm Plan B (Supabase) approach
2. **Get Mapbox access token** (or choose Google Maps if preferred)
3. **Create Supabase account** and project
4. **Setup Android project** with required dependencies
5. **Start Phase 1** (Backend Setup)

---

## Additional Considerations

### Security
- Use HTTPS only
- Implement rate limiting (Supabase has built-in)
- Validate all inputs on backend
- Use RLS for data protection
- Store API keys in local.properties (not in git)

### Performance
- Lazy load fountain markers (viewport-based)
- Implement pagination for reviews
- Cache frequently accessed data
- Use image optimization for any photos
- Minimize API calls with proper caching

### User Experience
- Offline mode with cached data
- Loading skeletons
- Error messages in user's language
- Pull-to-refresh
- Swipe gestures
- Dark mode support (bonus)

### Analytics (Optional)
- Firebase Analytics (free, even with Supabase)
- Track: fountain views, reviews submitted, feature usage
- No PII collection

### Future Enhancements
- Photos for fountains
- Comments on reviews
- Fountain favorites
- Share functionality
- Push notifications for nearby fountains
- Water quality reports
- Route planning (visit multiple fountains)

---

## Conclusion

**Recommended Approach: Plan B (Supabase) with Mapbox**

This combination provides:
- ✓ $0 monthly cost
- ✓ No credit card required
- ✓ Robust PostgreSQL database
- ✓ Easy maintenance
- ✓ Great developer experience
- ✓ Future-proof architecture
- ✓ Scales to 100+ users easily

Start with the implementation roadmap above and you'll have a production-ready app in approximately 2 weeks of focused development.

Good luck with your Barcelona Fountains Reviewer app! 🚰💧
