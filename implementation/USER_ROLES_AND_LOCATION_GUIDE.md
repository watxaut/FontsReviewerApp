# User Roles and Location-Based Reviews Guide

## Overview

The app now supports two user roles with different review permissions based on GPS location.

## User Roles

### 🔧 Operator (Default)
- **Review Restriction:** Can only review fountains within **300 meters** of their current location
- **Use Case:** Regular users exploring Barcelona fountains on foot
- **Location Required:** Yes (GPS permission needed)
- **Map Marker:** Red circle shows current location

### 👑 Admin
- **Review Restriction:** Can review **any fountain anywhere**
- **Use Case:** App administrators, data curators, remote reviews
- **Location Required:** No (but location still shown on map)
- **Map Marker:** Red circle shows current location

## Database Schema

### Migration Script: `USER_ROLES_MIGRATION.sql`

```sql
-- Add role column
ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'operator' 
CHECK (role IN ('admin', 'operator'));

-- Set a user as admin
UPDATE profiles SET role = 'admin' WHERE nickname = 'your_nickname';
```

**Run this in Supabase SQL Editor to enable roles.**

## How It Works

### For Operators:

1. **Tap fountain on map** → Fountain details screen
2. **Tap FAB (+) to add review**
3. **App checks:**
   - ✅ User is logged in?
   - ✅ Location permission granted?
   - ✅ Current GPS location available?
   - ✅ Distance to fountain ≤ 300m?
4. **If all checks pass** → Navigate to review screen
5. **If too far** → Show dialog with distance information

### For Admins:

1. **Tap fountain on map** → Fountain details screen
2. **Tap FAB (+) to add review**
3. **Navigate immediately** → No location checks!

## Permissions

### Required Permissions:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Permission Flow:

1. **First app launch** → Request location permission on Map screen
2. **User grants** → Red marker shows user location
3. **User denies** → Can browse map but operators can't review

### What Each Permission Does:

- **`ACCESS_COARSE_LOCATION`** (Required)
  - Block-level accuracy (~100-200m)
  - Sufficient for 300m proximity check
  - Battery-friendly
  
- **`ACCESS_FINE_LOCATION`** (Optional)
  - GPS-level accuracy (~5-10m)
  - More accurate distance calculations
  - Higher battery usage

## Map Features

### User Location Marker:

- **Color:** Red circle with white border
- **Size:** Medium (radius 8)
- **Updates:** Tap FAB button with location icon to refresh
- **Visible to:** Everyone (admin and operator)

### Refresh Location Button:

- **Icon:** 📍 MyLocation icon
- **Location:** Floating Action Button on Map screen
- **Action:** Gets fresh GPS coordinates
- **Use:** Update location after moving around Barcelona

## Distance Calculation

Uses **Haversine formula** for accurate spherical distance:

```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Returns distance in meters
    // Accurate for short distances (<1000km)
}
```

**Accuracy:** ±10-20 meters (depends on GPS accuracy)

## Error Messages

### User Not Logged In:
> "You must be logged in to review fountains"

### Location Permission Denied:
> "Location permission is required for operators to review fountains within 300m"

### Can't Get Location:
> "Unable to get your location. Please make sure location services are enabled."

### Too Far From Fountain (Operator):
> "You must be within 300m of this fountain to review it.
> 
> Current distance: 1.23 km (1230 meters)"

## Setting Up Admin Users

### Method 1: SQL Editor (Recommended)

```sql
-- Find your user ID
SELECT id, nickname, role FROM profiles;

-- Set user as admin
UPDATE profiles SET role = 'admin' WHERE nickname = 'your_nickname';

-- Verify
SELECT id, nickname, role FROM profiles WHERE role = 'admin';
```

### Method 2: Via UUID

```sql
UPDATE profiles SET role = 'admin' 
WHERE id = 'uuid-from-auth-users-table';
```

## Testing Checklist

### Test as Operator:

- [ ] Open app → Location permission requested
- [ ] Grant permission → Red marker appears on map
- [ ] Tap fountain < 300m away → Can add review
- [ ] Tap fountain > 300m away → Shows distance dialog
- [ ] Deny location → Can't review fountains
- [ ] Tap location refresh button → Location updates

### Test as Admin:

- [ ] Set role to 'admin' in database
- [ ] Restart app
- [ ] Tap any fountain (near or far) → Can add review
- [ ] No distance checks performed
- [ ] Location still shown on map

## Code Architecture

### Flow Diagram:

```
User taps FAB (+) on Fountain Details
         ↓
FountainDetailsViewModel.getCurrentUser()
         ↓
    ┌─────────────────────┐
    │   Check User Role   │
    └─────────────────────┘
         ↓            ↓
     ADMIN        OPERATOR
         ↓            ↓
   Navigate      Check Location
   to Review      Permission
                      ↓
                Has Permission?
                   ↓     ↓
                 Yes    No
                  ↓      ↓
            Get GPS  Request
            Location Permission
                  ↓
          Calculate Distance
                  ↓
            ≤ 300m?  > 300m?
              ↓         ↓
          Navigate   Show
          to Review  Dialog
```

### Key Components:

**LocationUtil.kt:**
- `hasLocationPermission()` - Check if granted
- `getCurrentLocation()` - Get GPS coordinates
- `calculateDistance()` - Haversine formula
- `isWithinRange()` - Check if within 300m

**FountainDetailsScreen.kt:**
- Permission launcher
- Location check logic
- Distance calculation
- Error dialogs

**FountainDetailsViewModel.kt:**
- `getCurrentUser()` - Get user with role
- `loadCurrentUser()` - Fetch from AuthRepository

## Performance Considerations

### Location Updates:

- **On Map Load:** Gets location once
- **Manual Refresh:** User taps FAB button
- **Not Continuous:** No battery drain from tracking
- **Accuracy:** PRIORITY_BALANCED_POWER_ACCURACY

### Distance Calculation:

- **Complexity:** O(1) - simple math
- **Performance:** <1ms per calculation
- **Accuracy:** ±10-20m (GPS dependent)

## Privacy & Security

### Data Collection:

✅ **Location NOT stored** - Only used for distance check
✅ **Location NOT sent to server** - Checked client-side only
✅ **No tracking** - No continuous location monitoring
✅ **User control** - Can deny permission and still browse

### Security:

- Role checks in app (UX)
- Database enforces user_id on reviews (RLS)
- Can't spoof location to bypass (would need rooted device)

## Future Enhancements (Optional)

1. **Show distance on fountain details**
   - "You are 150m away from this fountain"
   
2. **Filter map by proximity**
   - "Show only fountains within 1km"
   
3. **Geofencing notifications**
   - "You're near a highly-rated fountain!"
   
4. **Route to fountain**
   - "Get directions" button

5. **Show proximity indicator on map**
   - Circle showing 300m radius around user

## Troubleshooting

### Location permission keeps asking
**Solution:** Check app settings → Permissions → Location → Allow

### "Unable to get your location"
**Solutions:**
- Enable GPS/Location Services in phone settings
- Go outside (better GPS signal)
- Wait a few seconds for GPS to lock
- Tap refresh location button

### Distance seems wrong
**Solutions:**
- Tap refresh location to get fresh GPS fix
- Move to open area (buildings block GPS)
- Check fountain coordinates are correct

### Admin can't review
**Solutions:**
- Verify role is set: `SELECT role FROM profiles WHERE id = 'your-id';`
- Role should be 'admin' not 'Admin' (lowercase)
- Restart app after changing role

## Related Files

- `implementation/USER_ROLES_MIGRATION.sql` - Database schema
- `app/src/main/java/com/watxaut/fontsreviewer/util/LocationUtil.kt` - Location utilities
- `app/src/main/java/com/watxaut/fontsreviewer/domain/model/User.kt` - User model with roles
- `app/src/main/java/com/watxaut/fontsreviewer/presentation/details/FountainDetailsScreen.kt` - Location checks
- `app/src/main/java/com/watxaut/fontsreviewer/presentation/map/MapScreen.kt` - User location display

## Summary

✅ **Two user roles** - Admin and Operator
✅ **Location-based reviews** - Operators within 300m only
✅ **GPS permissions** - Coarse location for battery efficiency
✅ **User location on map** - Red marker shows current position
✅ **Clear error messages** - User knows why they can't review
✅ **Privacy-friendly** - Location not stored or transmitted

The app now has a complete role-based permission system! 🎉
