# 🔐 Session Persistence Fix - Final Solution

**Date:** 2025-10-12  
**Issue:** User session not persisting after closing and reopening the app  
**Status:** ✅ RESOLVED

---

## 🐛 Problem Description

### Symptoms
- After login, user is authenticated and admin buttons appear
- When closing and reopening the app, session is lost
- `getCurrentUserId()` returns `null` on app restart
- User must log in again every time they open the app

### Logs
```
# After restart - session lost
10-12 22:37:36.647 E AuthRepositoryImpl: getCurrentUserId() = null
10-12 22:37:36.647 E AuthRepositoryImpl: No user ID - user not logged in
```

---

## 🔍 Root Cause Analysis

### Initial Hypothesis (INCORRECT ❌)
We thought session storage was disabled and needed explicit configuration like:
```kotlin
install(Auth) {
    alwaysAutoRefresh = true  // ❌ These don't exist in Supabase Kotlin v3
    autoLoadFromStorage = true // ❌ Not real properties
}
```

### Actual Root Cause (CORRECT ✅)

**Session storage IS enabled by default** in Supabase Kotlin client, but there's a **timing issue**:

1. **Synchronous vs Asynchronous**
   - `client.auth.currentUserOrNull()` is synchronous
   - Session loading from SharedPreferences is **asynchronous**
   - When `MapViewModel.init{}` runs immediately on app start, the session hasn't loaded yet

2. **No Session Loading Listener**
   - The app was calling `getCurrentUser()` once in `init{}`
   - If session hadn't loaded yet → returns `null`
   - App never knew when session finished loading

---

## ✅ Solution

### Listen to `sessionStatus` Flow

The Supabase Kotlin client provides a `sessionStatus` Flow that emits when:
- Session loads from storage (→ `SessionStatus.Authenticated` with `source = SessionSource.Storage`)
- User signs in (→ `SessionStatus.Authenticated` with `source = SessionSource.SignIn`)
- User signs out (→ `SessionStatus.NotAuthenticated`)

### Implementation

**File:** `app/src/main/java/com/watxaut/fontsreviewer/presentation/map/MapViewModel.kt`

```kotlin
// 1. Add required imports
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

// 2. Inject SupabaseClient
@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFountainsUseCase: GetFountainsUseCase,
    private val authRepository: AuthRepository,
    private val getUserReviewedFountainsUseCase: GetUserReviewedFountainsUseCase,
    private val supabaseClient: SupabaseClient  // ← Add this
) : ViewModel() {

    init {
        // Listen to session changes from Supabase
        listenToSessionChanges()
        loadCurrentUser()
        loadFountains()
    }
    
    // 3. Add session listener
    private fun listenToSessionChanges() {
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus.collect { status: SessionStatus ->
                android.util.Log.e("MapViewModel", "Session status changed: $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        android.util.Log.e("MapViewModel", "User authenticated! Source: ${status.source}")
                        // Reload user when session changes
                        loadCurrentUser()
                        loadFountains()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        android.util.Log.e("MapViewModel", "User not authenticated")
                        _currentUser.value = null
                        loadFountains()
                    }
                    else -> {
                        android.util.Log.e("MapViewModel", "Session status: $status")
                    }
                }
            }
        }
    }
}
```

---

## 🎯 How It Works

### App Start Flow (Before Fix ❌)
```
1. App starts
2. MapViewModel.init {} runs
3. loadCurrentUser() called immediately
4. getCurrentUserId() → calls currentUserOrNull() → returns null (session not loaded yet!)
5. currentUser = null
6. Admin buttons don't show
```

### App Start Flow (After Fix ✅)
```
1. App starts
2. MapViewModel.init {} runs
3. listenToSessionChanges() starts collecting sessionStatus
4. loadCurrentUser() called (might return null initially)
5. **Supabase finishes loading session from SharedPreferences**
6. sessionStatus emits: SessionStatus.Authenticated(source = Storage)
7. listenToSessionChanges() receives event → calls loadCurrentUser() again
8. getCurrentUserId() → now returns the user ID!
9. currentUser = User(id=..., role=ADMIN)
10. Admin buttons appear! ✅
```

---

## 📝 Key Learnings

### ✅ DO's
1. **Session storage is automatic** in Supabase Kotlin client (uses SharedPreferences on Android)
2. **Listen to `sessionStatus` Flow** to know when session loads
3. **Session loading is asynchronous** - don't assume it's ready immediately

### ❌ DON'Ts
1. Don't use properties like `alwaysAutoRefresh` or `autoLoadFromStorage` (they don't exist)
2. Don't rely on `currentUserOrNull()` during app initialization
3. Don't call `getCurrentUser()` only once in `init{}`

---

## 🧪 Testing

### Verification Steps
1. **Login to the app**
   - You should see admin buttons if you're an admin
   
2. **Close the app completely**
   - Swipe it away from recent apps
   
3. **Reopen the app**
   - Check logcat for: `Session status changed: Authenticated`
   - Check logcat for: `User authenticated! Source: Storage`
   - Admin buttons should appear immediately

### Expected Logs
```
MapViewModel: Session status changed: Initializing
MapViewModel: Session status changed: Authenticated(session=UserSession(...), source=Storage)
MapViewModel: User authenticated! Source: Storage
AuthRepositoryImpl: getCurrentUserId() = 32c7a1d1-21cb-42e1-84b2-756919118c8f
AuthRepositoryImpl: Profile = ProfileDto(..., role=admin)
```

---

## 📚 Documentation References

- [Supabase Kotlin - Listen to auth events](https://supabase.com/docs/reference/kotlin/auth-onauthstatechange)
- [Supabase Kotlin - Initializing](https://supabase.com/docs/reference/kotlin/initializing)
- [Session Storage is automatic](https://github.com/supabase-community/supabase-kt/tree/master/Auth#session-handling)

---

## 🔄 Related Files Modified

1. `app/src/main/java/com/watxaut/fontsreviewer/presentation/map/MapViewModel.kt`
   - Added `SupabaseClient` injection
   - Added `listenToSessionChanges()` function
   - Now reloads user when session is restored from storage

2. `app/src/main/java/com/watxaut/fontsreviewer/di/NetworkModule.kt`
   - Removed incorrect `alwaysAutoRefresh` and `autoLoadFromStorage` properties
   - Added comment explaining that session storage is automatic

---

## 🎉 Result

**Sessions now persist across app restarts!** Users stay logged in and admin features work immediately after reopening the app.
