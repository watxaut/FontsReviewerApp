# FontsReviewer - Supabase Implementation Guide

## 🎯 Project Overview

**App Name:** FontsReviewer
**Backend:** Supabase (PostgreSQL + Auth)
**Maps:** Mapbox Android SDK
**Architecture:** Jetpack Compose + MVVM + Clean Architecture
**Languages:** English, Spanish, Catalan
**Target:** 1745 Barcelona fountains, <100 monthly users

---

## 📋 Phase-by-Phase Implementation Plan

### Phase 1: Backend Setup (Day 1) ⏱️ 4-6 hours

#### 1.1 Create Supabase Project

**Steps:**
1. Go to [supabase.com](https://supabase.com)
2. Sign up with GitHub/Email
3. Click "New Project"
4. Fill in:
   - **Name:** fontsreviewer
   - **Database Password:** (save securely!)
   - **Region:** West EU (closest to Barcelona)
   - **Pricing Plan:** Free
5. Wait 2-3 minutes for project provisioning

**Save These Credentials:**
```
Project URL: https://xxxxx.supabase.co
Anon (public) key: eyJhbGc...
Service role key: eyJhbGc... (keep secret!)
```

Location: `Settings` → `API`

#### 1.2 Create Database Schema

Go to `SQL Editor` → `New Query` and execute:

```sql
-- =====================================================
-- FONTS REVIEWER DATABASE SCHEMA
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- PROFILES TABLE (extends auth.users)
-- =====================================================
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  nickname TEXT UNIQUE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  total_ratings INTEGER DEFAULT 0,
  average_score DECIMAL(4,2) DEFAULT 0.00,
  best_fountain_id TEXT,
  CONSTRAINT nickname_length CHECK (char_length(nickname) >= 3 AND char_length(nickname) <= 20),
  CONSTRAINT average_score_range CHECK (average_score >= 0 AND average_score <= 5)
);

-- =====================================================
-- REVIEWS TABLE
-- =====================================================
CREATE TABLE reviews (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  fountain_id TEXT NOT NULL,
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  user_nickname TEXT NOT NULL,

  -- Rating categories (1-5)
  taste INTEGER NOT NULL CHECK (taste >= 1 AND taste <= 5),
  freshness INTEGER NOT NULL CHECK (freshness >= 1 AND freshness <= 5),
  location_rating INTEGER NOT NULL CHECK (location_rating >= 1 AND location_rating <= 5),
  aesthetics INTEGER NOT NULL CHECK (aesthetics >= 1 AND aesthetics <= 5),

  -- Calculated overall score
  overall DECIMAL(4,2) GENERATED ALWAYS AS (
    (taste + freshness + location_rating + aesthetics) / 4.0
  ) STORED,

  -- Optional comment
  comment TEXT,

  -- Timestamps
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

  -- Constraints
  CONSTRAINT unique_user_fountain UNIQUE(user_id, fountain_id),
  CONSTRAINT comment_length CHECK (comment IS NULL OR char_length(comment) <= 500)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================
CREATE INDEX idx_reviews_fountain_id ON reviews(fountain_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);
CREATE INDEX idx_reviews_overall ON reviews(overall DESC);
CREATE INDEX idx_profiles_nickname ON profiles(nickname);
CREATE INDEX idx_profiles_total_ratings ON profiles(total_ratings DESC);

-- =====================================================
-- FUNCTION: Update User Statistics
-- =====================================================
CREATE OR REPLACE FUNCTION update_user_stats()
RETURNS TRIGGER AS $$
DECLARE
  user_uuid UUID;
BEGIN
  -- Determine which user to update
  IF TG_OP = 'DELETE' THEN
    user_uuid := OLD.user_id;
  ELSE
    user_uuid := NEW.user_id;
  END IF;

  -- Update user statistics
  UPDATE profiles
  SET
    total_ratings = (
      SELECT COUNT(*)
      FROM reviews
      WHERE user_id = user_uuid
    ),
    average_score = COALESCE((
      SELECT ROUND(AVG(overall)::numeric, 2)
      FROM reviews
      WHERE user_id = user_uuid
    ), 0),
    best_fountain_id = (
      SELECT fountain_id
      FROM reviews
      WHERE user_id = user_uuid
      ORDER BY overall DESC, created_at DESC
      LIMIT 1
    ),
    updated_at = NOW()
  WHERE id = user_uuid;

  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- TRIGGERS: Auto-update Stats
-- =====================================================
CREATE TRIGGER trigger_update_stats_on_insert
AFTER INSERT ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_user_stats();

CREATE TRIGGER trigger_update_stats_on_update
AFTER UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_user_stats();

CREATE TRIGGER trigger_update_stats_on_delete
AFTER DELETE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_user_stats();

-- =====================================================
-- FUNCTION: Update Updated_At Timestamp
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to profiles
CREATE TRIGGER trigger_profiles_updated_at
BEFORE UPDATE ON profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Apply to reviews
CREATE TRIGGER trigger_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- FUNCTION: Create Profile on User Signup
-- =====================================================
-- IMPORTANT: SECURITY DEFINER is critical to bypass RLS policies
-- The trigger runs before the auth session is fully established,
-- so auth.uid() won't work in RLS checks during signup.
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER 
SECURITY DEFINER  -- Critical: Bypasses RLS policies
SET search_path = public  -- Ensures correct schema
AS $$
BEGIN
  INSERT INTO public.profiles (id, nickname)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'nickname', 'User' || substring(NEW.id::text, 1, 8))
  );
  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    -- Log the error but don't fail the signup
    RAISE WARNING 'Failed to create profile: %', SQLERRM;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger on auth.users signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW
EXECUTE FUNCTION handle_new_user();

-- =====================================================
-- VIEW: Leaderboard
-- =====================================================
CREATE OR REPLACE VIEW leaderboard AS
SELECT
  nickname,
  total_ratings,
  average_score,
  RANK() OVER (ORDER BY total_ratings DESC, average_score DESC) as rank
FROM profiles
WHERE total_ratings > 0
ORDER BY rank
LIMIT 100;

-- =====================================================
-- VIEW: Fountain Statistics
-- =====================================================
CREATE OR REPLACE VIEW fountain_stats AS
SELECT
  fountain_id,
  COUNT(*) as total_reviews,
  ROUND(AVG(overall)::numeric, 2) as average_rating,
  ROUND(AVG(taste)::numeric, 2) as avg_taste,
  ROUND(AVG(freshness)::numeric, 2) as avg_freshness,
  ROUND(AVG(location_rating)::numeric, 2) as avg_location,
  ROUND(AVG(aesthetics)::numeric, 2) as avg_aesthetics,
  MAX(created_at) as last_reviewed_at
FROM reviews
GROUP BY fountain_id;

-- =====================================================
-- GRANT PERMISSIONS (anon role can read public data)
-- =====================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON leaderboard TO anon, authenticated;
GRANT SELECT ON fountain_stats TO anon, authenticated;
```

#### 1.3 Set Up Row Level Security (RLS)

Execute this in SQL Editor:

```sql
-- =====================================================
-- ROW LEVEL SECURITY POLICIES
-- =====================================================

-- Enable RLS on tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- PROFILES POLICIES
-- =====================================================

-- Everyone can view all profiles (for leaderboard)
CREATE POLICY "Profiles are viewable by everyone"
ON profiles
FOR SELECT
USING (true);

-- Users can insert their own profile (handled by trigger mostly)
-- Note: WITH CHECK allows auth.uid() IS NULL for trigger context
CREATE POLICY "Users can insert own profile"
ON profiles
FOR INSERT
TO public, authenticated, service_role
WITH CHECK (
  auth.uid() = id OR  -- Allow user to insert their own profile
  auth.uid() IS NULL  -- Allow trigger (runs without auth context during signup)
);

-- Users can update only their own profile
CREATE POLICY "Users can update own profile"
ON profiles
FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Users cannot delete profiles (cascade from auth.users)
CREATE POLICY "No one can delete profiles"
ON profiles
FOR DELETE
USING (false);

-- =====================================================
-- REVIEWS POLICIES
-- =====================================================

-- Everyone (including anonymous) can read reviews
CREATE POLICY "Reviews are viewable by everyone"
ON reviews
FOR SELECT
USING (true);

-- Only authenticated users can create reviews
-- And only for themselves
CREATE POLICY "Authenticated users can create reviews"
ON reviews
FOR INSERT
TO authenticated
WITH CHECK (
  auth.uid() = user_id AND
  user_nickname = (SELECT nickname FROM profiles WHERE id = auth.uid())
);

-- Users can update only their own reviews
CREATE POLICY "Users can update own reviews"
ON reviews
FOR UPDATE
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Users can delete only their own reviews
CREATE POLICY "Users can delete own reviews"
ON reviews
FOR DELETE
TO authenticated
USING (auth.uid() = user_id);
```

#### 1.4 Configure Authentication

1. Go to `Authentication` → `Settings`
2. Under **Auth Providers**, ensure **Email** is enabled
3. **Disable email confirmation** (for simplicity):
   - Scroll to "Email Auth"
   - Toggle OFF "Enable email confirmations"
4. Set **Site URL**: `fontsreviewer://app` (for deep linking)
5. Add **Redirect URLs**:
   - `fontsreviewer://callback`
   - `http://localhost` (for testing)

**⚠️ Important Note on Database Trigger:**
The `handle_new_user()` trigger MUST have `SECURITY DEFINER` to bypass RLS policies during signup. Without it, you'll get "Database error saving new user" errors because `auth.uid()` is not available in the trigger context during user creation.

#### 1.5 Test Database

Execute test queries in SQL Editor:

```sql
-- Test: Check if views work
SELECT * FROM leaderboard;
SELECT * FROM fountain_stats;

-- Test: Check triggers exist and are properly configured
SELECT 
    trigger_name, 
    event_manipulation, 
    action_timing,
    action_statement
FROM information_schema.triggers
WHERE trigger_name = 'on_auth_user_created';

-- Test: Verify function has SECURITY DEFINER
SELECT 
    routine_name,
    routine_type,
    security_type  -- Should show 'DEFINER'
FROM information_schema.routines 
WHERE routine_name = 'handle_new_user';

-- Test: Check RLS policies on profiles table
SELECT 
    schemaname, 
    tablename, 
    policyname, 
    cmd
FROM pg_policies
WHERE tablename = 'profiles';
```

**Expected Results:**
- Trigger should show `AFTER INSERT` on `auth.users`
- Function should have `security_type = 'DEFINER'`
- Profiles table should have 4 policies: SELECT, INSERT, UPDATE, DELETE

#### 1.6 Save Configuration

Create file: `app/src/main/res/values/supabase_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- DO NOT commit this file to git! Add to .gitignore -->
    <string name="supabase_url">YOUR_PROJECT_URL</string>
    <string name="supabase_anon_key">YOUR_ANON_KEY</string>
</resources>
```

**Add to `.gitignore`:**
```
app/src/main/res/values/supabase_config.xml
```

---

### Phase 2: Android Project Setup (Day 2) ⏱️ 6-8 hours

#### 2.1 Update Dependencies

**File:** `build.gradle.kts` (project level)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
}
```

**File:** `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.watxaut.fontsreviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.watxaut.fontsreviewer"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load Mapbox token from local.properties
        val mapboxToken = project.findProperty("MAPBOX_ACCESS_TOKEN") ?: ""
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Existing Compose dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor (required by Supabase)
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-utils:2.3.11")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Mapbox Maps
    implementation("com.mapbox.maps:android:11.3.0")
    implementation("com.mapbox.extension:maps-compose:11.3.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

#### 2.2 Setup Mapbox

**File:** `local.properties`

```properties
MAPBOX_ACCESS_TOKEN=pk.your_mapbox_token_here
```

**Get Mapbox Token:**
1. Go to [mapbox.com](https://mapbox.com)
2. Sign up (free tier: 50K loads/month)
3. Go to Account → Tokens
4. Copy default public token or create new one
5. Paste into `local.properties`

**File:** `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".FontsReviewerApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FontsReviewer"
        android:usesCleartextTraffic="false">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FontsReviewer"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

#### 2.3 Create Application Class

**File:** `app/src/main/java/com/watxaut/fontsreviewer/FontsReviewerApp.kt`

```kotlin
package com.watxaut.fontsreviewer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FontsReviewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

#### 2.4 Project Structure

Create the following package structure:

```
app/src/main/java/com/watxaut/fontsreviewer/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/
│   │   └── entity/
│   ├── remote/
│   │   ├── dto/
│   │   └── service/
│   ├── repository/
│   └── mapper/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── auth/
│   │   ├── login/
│   │   └── register/
│   ├── map/
│   ├── review/
│   ├── stats/
│   ├── leaderboard/
│   ├── navigation/
│   └── components/
├── di/
├── util/
└── FontsReviewerApp.kt
```

---

### Phase 3: Data Layer (Day 3) ⏱️ 6-8 hours

#### 3.1 Domain Models

**File:** `domain/model/Fountain.kt`

```kotlin
package com.watxaut.fontsreviewer.domain.model

data class Fountain(
    val codi: String,
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0
)
```

**File:** `domain/model/Review.kt`

```kotlin
package com.watxaut.fontsreviewer.domain.model

import java.time.Instant

data class Review(
    val id: String,
    val fountainId: String,
    val userId: String,
    val userNickname: String,
    val taste: Int,
    val freshness: Int,
    val locationRating: Int,
    val aesthetics: Int,
    val overall: Double,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateReviewRequest(
    val fountainId: String,
    val taste: Int,
    val freshness: Int,
    val locationRating: Int,
    val aesthetics: Int,
    val comment: String? = null
)
```

**File:** `domain/model/User.kt`

```kotlin
package com.watxaut.fontsreviewer.domain.model

data class User(
    val id: String,
    val nickname: String,
    val totalRatings: Int = 0,
    val averageScore: Double = 0.0,
    val bestFountainId: String? = null
)

data class UserStats(
    val totalRatings: Int,
    val averageScore: Double,
    val bestFountain: Fountain?
)

data class LeaderboardEntry(
    val nickname: String,
    val totalRatings: Int,
    val averageScore: Double,
    val rank: Int
)
```

#### 3.2 Local Database (Room)

**File:** `data/local/entity/FountainEntity.kt`

```kotlin
package com.watxaut.fontsreviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fountains")
data class FountainEntity(
    @PrimaryKey
    val codi: String,
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double
)
```

**File:** `data/local/dao/FountainDao.kt`

```kotlin
package com.watxaut.fontsreviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FountainDao {

    @Query("SELECT * FROM fountains")
    fun getAllFountains(): Flow<List<FountainEntity>>

    @Query("SELECT * FROM fountains WHERE codi = :codi")
    suspend fun getFountainByCodi(codi: String): FountainEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fountains: List<FountainEntity>)

    @Query("SELECT COUNT(*) FROM fountains")
    suspend fun getFountainCount(): Int

    @Query("DELETE FROM fountains")
    suspend fun deleteAll()
}
```

**File:** `data/local/database/AppDatabase.kt`

```kotlin
package com.watxaut.fontsreviewer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity

@Database(
    entities = [FountainEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fountainDao(): FountainDao
}
```

#### 3.3 Remote DTOs (Supabase)

**File:** `data/remote/dto/ProfileDto.kt`

```kotlin
package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id")
    val id: String,

    @SerialName("nickname")
    val nickname: String,

    @SerialName("total_ratings")
    val totalRatings: Int = 0,

    @SerialName("average_score")
    val averageScore: Double = 0.0,

    @SerialName("best_fountain_id")
    val bestFountainId: String? = null
)

@Serializable
data class CreateProfileDto(
    @SerialName("id")
    val id: String,

    @SerialName("nickname")
    val nickname: String
)
```

**File:** `data/remote/dto/ReviewDto.kt`

```kotlin
package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("id")
    val id: String,

    @SerialName("fountain_id")
    val fountainId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_nickname")
    val userNickname: String,

    @SerialName("taste")
    val taste: Int,

    @SerialName("freshness")
    val freshness: Int,

    @SerialName("location_rating")
    val locationRating: Int,

    @SerialName("aesthetics")
    val aesthetics: Int,

    @SerialName("overall")
    val overall: Double,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class CreateReviewDto(
    @SerialName("fountain_id")
    val fountainId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_nickname")
    val userNickname: String,

    @SerialName("taste")
    val taste: Int,

    @SerialName("freshness")
    val freshness: Int,

    @SerialName("location_rating")
    val locationRating: Int,

    @SerialName("aesthetics")
    val aesthetics: Int,

    @SerialName("comment")
    val comment: String? = null
)

@Serializable
data class LeaderboardDto(
    @SerialName("nickname")
    val nickname: String,

    @SerialName("total_ratings")
    val totalRatings: Int,

    @SerialName("average_score")
    val averageScore: Double,

    @SerialName("rank")
    val rank: Int
)
```

#### 3.4 Supabase Service

**File:** `data/remote/service/SupabaseService.kt`

```kotlin
package com.watxaut.fontsreviewer.data.remote.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import com.watxaut.fontsreviewer.data.remote.dto.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseService @Inject constructor(
    private val client: SupabaseClient
) {

    // ==================== Authentication ====================

    suspend fun signUp(email: String, password: String, nickname: String): Result<String> {
        return try {
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("nickname", nickname)
                }
            }

            // Manually create profile if trigger didn't work
            val userId = result.user?.id ?: return Result.failure(Exception("No user ID"))
            createProfile(userId, nickname)

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = result.user?.id ?: return Result.failure(Exception("No user ID"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun isUserLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }

    // ==================== Profiles ====================

    private suspend fun createProfile(userId: String, nickname: String) {
        client.from("profiles").insert(
            CreateProfileDto(id = userId, nickname = nickname)
        )
    }

    suspend fun getProfile(userId: String): Result<ProfileDto> {
        return try {
            val profile = client.from("profiles")
                .select()
                .eq("id", userId)
                .decodeSingle<ProfileDto>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNickname(userId: String, newNickname: String): Result<Unit> {
        return try {
            client.from("profiles")
                .update({
                    set("nickname", newNickname)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Reviews ====================

    suspend fun createReview(review: CreateReviewDto): Result<ReviewDto> {
        return try {
            val created = client.from("reviews")
                .insert(review) {
                    select()
                }
                .decodeSingle<ReviewDto>()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsForFountain(fountainId: String): Result<List<ReviewDto>> {
        return try {
            val reviews = client.from("reviews")
                .select()
                .eq("fountain_id", fountainId)
                .order("created_at", ascending = false)
                .decodeList<ReviewDto>()
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReview(userId: String, fountainId: String): Result<ReviewDto?> {
        return try {
            val review = client.from("reviews")
                .select()
                .eq("user_id", userId)
                .eq("fountain_id", fountainId)
                .decodeSingleOrNull<ReviewDto>()
            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReview(reviewId: String, updates: CreateReviewDto): Result<ReviewDto> {
        return try {
            val updated = client.from("reviews")
                .update({
                    set("taste", updates.taste)
                    set("freshness", updates.freshness)
                    set("location_rating", updates.locationRating)
                    set("aesthetics", updates.aesthetics)
                    updates.comment?.let { set("comment", it) }
                }) {
                    filter {
                        eq("id", reviewId)
                    }
                    select()
                }
                .decodeSingle<ReviewDto>()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            client.from("reviews")
                .delete {
                    filter {
                        eq("id", reviewId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Leaderboard ====================

    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardDto>> {
        return try {
            val leaderboard = client.from("leaderboard")
                .select()
                .limit(limit.toLong())
                .decodeList<LeaderboardDto>()
            Result.success(leaderboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### Phase 4: Dependency Injection (Day 3 continued) ⏱️ 2-3 hours

#### 4.1 Network Module

**File:** `di/NetworkModule.kt`

```kotlin
package com.watxaut.fontsreviewer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import com.watxaut.fontsreviewer.R
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        val supabaseUrl = context.getString(R.string.supabase_url)
        val supabaseKey = context.getString(R.string.supabase_anon_key)

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
```

#### 4.2 Database Module

**File:** `di/DatabaseModule.kt`

```kotlin
package com.watxaut.fontsreviewer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.local.database.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fonts_reviewer_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFountainDao(database: AppDatabase): FountainDao {
        return database.fountainDao()
    }
}
```

#### 4.3 Repository Module

**File:** `di/RepositoryModule.kt`

```kotlin
package com.watxaut.fontsreviewer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.watxaut.fontsreviewer.data.repository.*
import com.watxaut.fontsreviewer.domain.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFountainRepository(
        impl: FountainRepositoryImpl
    ): FountainRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        impl: ReviewRepositoryImpl
    ): ReviewRepository
}
```

---

### Phase 5: Parse CSV & Load Fountains (Day 4) ⏱️ 4-5 hours

#### 5.1 CSV Parser Utility

**File:** `util/CsvParser.kt`

```kotlin
package com.watxaut.fontsreviewer.util

import android.content.Context
import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {

    suspend fun parseFountainsFromAssets(
        context: Context,
        fileName: String = "2025_fonts_bcn.csv"
    ): List<FountainEntity> = withContext(Dispatchers.IO) {
        val fountains = mutableListOf<FountainEntity>()

        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { csvLine ->
                            parseLine(csvLine)?.let { fountain ->
                                fountains.add(fountain)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fountains
    }

    private fun parseLine(line: String): FountainEntity? {
        return try {
            // Split by comma, handling quoted fields
            val fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                .map { it.trim().removeSurrounding("\"") }

            if (fields.size < 8) return null

            FountainEntity(
                codi = fields[0],
                nom = fields[1],
                carrer = fields[2],
                numeroCarrer = fields[3],
                latitude = fields[6].toDoubleOrNull() ?: return null,
                longitude = fields[7].toDoubleOrNull() ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }
}
```

**Action:** Move `2025_fonts_bcn.csv` to `app/src/main/assets/`

```bash
mkdir -p app/src/main/assets
cp 2025_fonts_bcn.csv app/src/main/assets/
```

#### 5.2 Repository Implementations

**File:** `domain/repository/FountainRepository.kt`

```kotlin
package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.Fountain
import kotlinx.coroutines.flow.Flow

interface FountainRepository {
    fun getAllFountains(): Flow<List<Fountain>>
    suspend fun getFountainByCodi(codi: String): Fountain?
    suspend fun initializeFountains()
    suspend fun getFountainCount(): Int
}
```

**File:** `data/repository/FountainRepositoryImpl.kt`

```kotlin
package com.watxaut.fontsreviewer.data.repository

import android.content.Context
import com.watxaut.fontsreviewer.data.local.dao.FountainDao
import com.watxaut.fontsreviewer.data.mapper.toFountain
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.util.CsvParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FountainRepositoryImpl @Inject constructor(
    private val fountainDao: FountainDao,
    @ApplicationContext private val context: Context
) : FountainRepository {

    override fun getAllFountains(): Flow<List<Fountain>> {
        return fountainDao.getAllFountains()
            .map { entities -> entities.map { it.toFountain() } }
    }

    override suspend fun getFountainByCodi(codi: String): Fountain? {
        return fountainDao.getFountainByCodi(codi)?.toFountain()
    }

    override suspend fun initializeFountains() {
        val count = fountainDao.getFountainCount()
        if (count == 0) {
            val fountains = CsvParser.parseFountainsFromAssets(context)
            fountainDao.insertAll(fountains)
        }
    }

    override suspend fun getFountainCount(): Int {
        return fountainDao.getFountainCount()
    }
}
```

**File:** `data/mapper/FountainMapper.kt`

```kotlin
package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import com.watxaut.fontsreviewer.domain.model.Fountain

fun FountainEntity.toFountain(): Fountain {
    return Fountain(
        codi = codi,
        nom = nom,
        carrer = carrer,
        numeroCarrer = numeroCarrer,
        latitude = latitude,
        longitude = longitude
    )
}
```

#### 5.3 Initialize on App Start

**File:** `FontsReviewerApp.kt` (update)

```kotlin
package com.watxaut.fontsreviewer

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FontsReviewerApp : Application() {

    @Inject
    lateinit var fountainRepository: FountainRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize fountains in background
        applicationScope.launch {
            fountainRepository.initializeFountains()
        }
    }
}
```

---

**Continue in next message with Phases 6-12...**

Would you like me to continue with the remaining phases (Map UI, Auth, Reviews, Stats, Leaderboard, Localization, Testing, and Polish)?
