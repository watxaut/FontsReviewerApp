# Database Migration History

**Last Updated:** 2025-10-12  
**Database:** Supabase PostgreSQL  
**Project:** FontsReviewer Barcelona

---

## 📋 Overview

This directory contains all SQL migrations applied to the Supabase database in chronological order. These migrations evolved the database from initial setup to the current production-ready state.

**⚠️ IMPORTANT:** Always run migrations in the exact order listed below.

---

## 🗂️ Migration Timeline

### Migration 1: Fountains Table & Views
**File:** `01-FOUNTAINS_MIGRATION.sql`  
**Date Applied:** October 2025  
**Status:** ✅ Applied  

**Purpose:**
- Create the core `fountains` table to store 1,745 Barcelona public fountains
- Create aggregated views for fountain statistics
- Enable Row Level Security with public read access

**What This Migration Does:**

1. **Creates `fountains` table:**
   - `codi` (TEXT PRIMARY KEY) - Unique fountain code
   - `nom` (TEXT) - Fountain name
   - `carrer` (TEXT) - Street name
   - `numero_carrer` (TEXT) - Street number
   - `latitude` (DOUBLE PRECISION) - GPS latitude
   - `longitude` (DOUBLE PRECISION) - GPS longitude
   - `created_at`, `updated_at` - Timestamps

2. **Creates indexes for performance:**
   - `idx_fountains_location` on (latitude, longitude)
   - `idx_fountains_nom` on fountain name

3. **Creates `fountain_stats_detailed` VIEW:**
   - Joins fountains with reviews table
   - Aggregates: total_reviews, average_rating, avg_taste, avg_freshness, avg_location, avg_aesthetics
   - Used by the app to fetch all fountain data with stats in one query

4. **Creates `fountain_stats` VIEW:**
   - Backward compatibility view
   - Simplified version of fountain_stats_detailed

5. **Enables RLS (Row Level Security):**
   - Policy: "Fountains are viewable by everyone"
   - Allows SELECT for both `anon` and `authenticated` roles

**Data Import:**
After running this migration, you imported 1,745 fountains from `2025_fonts_bcn.csv` via Supabase Dashboard.

**Verification Query:**
```sql
SELECT COUNT(*) FROM fountains; -- Should return 1745
SELECT * FROM fountain_stats_detailed LIMIT 5;
```

---

### Migration 2: User Roles (Admin/Operator)
**File:** `02-USER_ROLES_MIGRATION.sql`  
**Date Applied:** October 2025  
**Status:** ✅ Applied

**Purpose:**
- Add role-based access control (admin vs operator)
- Operators: Can only review fountains within 300m (enforced in app)
- Admins: Can review any fountain anywhere

**What This Migration Does:**

1. **Adds `role` column to profiles:**
   - Type: TEXT
   - Default: 'operator'
   - Constraint: CHECK (role IN ('admin', 'operator'))

2. **Creates index:**
   - `idx_profiles_role` for efficient role queries

3. **Creates helper function:**
   - `is_admin(user_uuid UUID)` - Returns BOOLEAN
   - Used to check if a user has admin privileges

4. **Location Enforcement:**
   - ⚠️ Note: Location checks (300m radius) are enforced in the Android app, NOT in the database
   - This design allows better UX (showing distance, enabling/disabling buttons)
   - Database doesn't store or validate GPS coordinates

**Set Your Admin User:**
```sql
-- Replace with your actual user ID or nickname
UPDATE profiles SET role = 'admin' WHERE nickname = 'watxaut';

-- Or by user ID:
UPDATE profiles SET role = 'admin' WHERE id = 'your-uuid-here';
```

**Verification Query:**
```sql
SELECT id, nickname, role FROM profiles;
SELECT is_admin('your-user-id-here'); -- Should return true for admin
```

---

### Migration 3: Admin Fountain Management (Soft Delete)
**File:** `03-ADMIN_FOUNTAIN_MANAGEMENT_MIGRATION.sql`  
**Date Applied:** October 2025  
**Status:** ✅ Applied

**Purpose:**
- Allow admins to create, update, and soft-delete fountains
- Soft delete = mark as deleted but don't physically remove (data preservation)
- Non-deleted fountains visible to public, deleted ones only to admins

**What This Migration Does:**

1. **Adds `is_deleted` column to fountains:**
   - Type: BOOLEAN
   - Default: FALSE
   - Index: `idx_fountains_is_deleted` for performance

2. **Updates `fountain_stats_detailed` view:**
   - Now filters out deleted fountains: `WHERE f.is_deleted = FALSE`
   - Public users never see deleted fountains in stats

3. **Updates RLS Policies:**
   - **Policy 1:** "Anyone can view non-deleted fountains" (anon + authenticated)
   - **Policy 2:** "Admins can view all fountains" (including deleted)
   - **Policy 3:** "Admins can insert fountains"
   - **Policy 4:** "Admins can update fountains" (including soft delete)
   - **Policy 5:** "No one can delete fountains" (prevents accidental hard deletes)

4. **Creates helper functions:**
   - `soft_delete_fountain(fountain_codi TEXT)` - Marks fountain as deleted
   - `restore_fountain(fountain_codi TEXT)` - Restores a deleted fountain
   - Both check if user is admin before allowing operation

**Usage Examples:**
```sql
-- Soft delete a fountain (admin only)
SELECT soft_delete_fountain('12345');

-- Restore a fountain (admin only)
SELECT restore_fountain('12345');

-- Check if fountain is deleted
SELECT codi, nom, is_deleted FROM fountains WHERE codi = '12345';
```

**Important Notes:**
- Reviews for deleted fountains are preserved (not affected)
- To "undo" a soft delete, use `restore_fountain()`
- Physical deletion (DROP/DELETE) is blocked by RLS policy

**Verification Query:**
```sql
-- Check is_deleted column exists
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'fountains' AND column_name = 'is_deleted';

-- Check RLS policies
SELECT schemaname, tablename, policyname, cmd, roles
FROM pg_policies
WHERE tablename = 'fountains';
```

---

### Migration 4: Account Deletion (GDPR Compliance)
**File:** `04-DELETE_ACCOUNT_MIGRATION.sql`  
**Date Applied:** October 2025  
**Status:** ✅ Applied (Database) | ⚠️ Edge Function Optional

**Purpose:**
- Enable users to delete their accounts (GDPR "right to be forgotten")
- Cascade delete: when profile deleted → all reviews deleted automatically
- Note: Full auth.users deletion requires Supabase Edge Function (optional)

**What This Migration Does:**

1. **Updates Foreign Key Constraint:**
   - Modifies `reviews.user_id` foreign key
   - Changes from default to `ON DELETE CASCADE`
   - Now: Delete profile → automatically deletes all user's reviews

2. **Creates `delete_user_account()` function:**
   - Takes user UUID as parameter
   - Deletes profile (which cascades to reviews)
   - Note: Does NOT delete auth.users (requires admin privileges)

3. **Adds RLS Policy:**
   - "Users can delete their own profile"
   - Allows users to self-delete via app

4. **GDPR Compliance Status:**
   - ✅ Profile deletion: Works
   - ✅ Review cascade deletion: Works
   - ⚠️ Auth user deletion: Requires Edge Function (see notes below)

**Full Deletion Workflow:**

When a user deletes their account in the app:

1. **Database deletion** (this migration):
   ```sql
   DELETE FROM profiles WHERE id = 'user-uuid';
   -- Automatically deletes all reviews (CASCADE)
   ```

2. **Auth user deletion** (requires Edge Function):
   - The Supabase Kotlin SDK cannot delete auth.users directly
   - Options:
     - **Option A (MVP):** Manual deletion by admin in Supabase Dashboard
     - **Option B (Production):** Deploy Edge Function (see `EDGE_FUNCTION_SETUP.md`)
     - **Option C (Advanced):** Database trigger calling admin API

**For Barcelona Launch:**
- Option A (manual admin deletion) is acceptable for MVP
- Can implement Option B (Edge Function) post-launch

**Edge Function Reference:**
See `implementation/04-SETUP_GUIDES/EDGE_FUNCTION_SETUP.md` for complete guide to deploy the delete-user-account function.

**Verification Query:**
```sql
-- Check CASCADE delete is configured
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
    ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'reviews'
    AND kcu.column_name = 'user_id';
-- Expected: delete_rule = 'CASCADE'

-- Test (optional, in test environment only):
-- 1. Create test user
-- 2. Create test review
-- 3. Delete profile
-- 4. Verify review auto-deleted
```

---

## 🚀 How to Apply Migrations

### First-Time Setup (New Supabase Project)

**Prerequisites:**
1. Supabase account created
2. Project created (West EU region recommended for Barcelona)
3. Project credentials saved (URL + Anon Key)

**Step-by-Step:**

1. **Run Core Schema First:**
   - Go to Supabase Dashboard → SQL Editor
   - Run the schema from `04-SETUP_GUIDES/SUPABASE_SETUP.md`:
     - Creates `profiles` table
     - Creates `reviews` table
     - Creates triggers (`handle_new_user`, `update_user_stats`)
     - Creates `leaderboard` view
     - Enables RLS on all tables

2. **Run Migrations in Order:**
   ```sql
   -- Migration 1: Fountains
   -- Copy/paste contents of 01-FOUNTAINS_MIGRATION.sql
   -- Execute

   -- Migration 2: User Roles  
   -- Copy/paste contents of 02-USER_ROLES_MIGRATION.sql
   -- Execute

   -- Migration 3: Admin Fountain Management
   -- Copy/paste contents of 03-ADMIN_FOUNTAIN_MANAGEMENT_MIGRATION.sql
   -- Execute

   -- Migration 4: Account Deletion
   -- Copy/paste contents of 04-DELETE_ACCOUNT_MIGRATION.sql
   -- Execute
   ```

3. **Import Fountain Data:**
   - Go to: Table Editor → fountains → Insert → Import data from CSV
   - Upload: `2025_fonts_bcn.csv`
   - Map columns: codi, nom, carrer, numero_carrer, latitude, longitude
   - Import all 1,745 rows

4. **Set Admin User:**
   ```sql
   -- After you've created your first user in the app:
   UPDATE profiles SET role = 'admin' WHERE nickname = 'watxaut';
   ```

5. **Verify Everything:**
   ```sql
   -- Check all tables exist
   SELECT table_name FROM information_schema.tables 
   WHERE table_schema = 'public'
   ORDER BY table_name;
   -- Expected: fountains, leaderboard (view), profiles, reviews, 
   --           fountain_stats (view), fountain_stats_detailed (view)

   -- Check fountains loaded
   SELECT COUNT(*) FROM fountains; -- Should be 1745

   -- Check RLS enabled on all tables
   SELECT tablename, rowsecurity 
   FROM pg_tables 
   WHERE schemaname = 'public';
   -- All should show rowsecurity = true

   -- Check admin user
   SELECT id, nickname, role FROM profiles WHERE role = 'admin';
   ```

---

## 🔍 Current Production State

**Database Status:** ✅ All Migrations Applied

| Component | Status | Count/Details |
|-----------|--------|---------------|
| Fountains | ✅ Loaded | 1,745 |
| Tables | ✅ Created | profiles, reviews, fountains |
| Views | ✅ Created | leaderboard, fountain_stats, fountain_stats_detailed |
| RLS Policies | ✅ Enabled | All tables protected |
| Triggers | ✅ Active | handle_new_user, update_user_stats |
| Indexes | ✅ Created | Location, role, is_deleted |
| Admin Users | ✅ Configured | At least 1 |
| Edge Functions | ⚠️ Optional | delete-user-account (not required for MVP) |

**Last Verified:** 2025-10-12

---

## 📝 Future Migrations

When you need to add new features or modify the database:

1. **Create new migration file:**
   ```
   05-YOUR_FEATURE_NAME.sql
   ```

2. **Add entry to this history:**
   - Date applied
   - Purpose
   - What it does
   - Verification queries
   - Impact on existing data

3. **Test in development first:**
   - Create test Supabase project
   - Run migration
   - Verify no data loss
   - Test app functionality

4. **Apply to production:**
   - Backup database first
   - Run during low-traffic time
   - Verify immediately
   - Monitor for issues

---

## 🆘 Troubleshooting

### Migration Failed Mid-Execution

**Problem:** SQL script failed halfway through.

**Solution:**
1. Check which statements completed:
   ```sql
   SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
   ```
2. Manually rollback incomplete changes if needed
3. Fix the error in SQL
4. Re-run from failed point

### Duplicate Key Error

**Problem:** `ERROR: duplicate key value violates unique constraint`

**Solution:**
- Migration already applied
- Check: `SELECT column_name FROM information_schema.columns WHERE table_name = 'your_table';`
- If column exists, skip this migration

### RLS Policy Conflicts

**Problem:** `ERROR: policy "policy_name" for table "table_name" already exists`

**Solution:**
```sql
-- Drop existing policy first
DROP POLICY IF EXISTS "policy_name" ON table_name;
-- Then re-run migration
```

### Fountain Count Mismatch

**Problem:** `SELECT COUNT(*) FROM fountains` returns wrong number.

**Solution:**
1. Check for duplicate imports:
   ```sql
   SELECT codi, COUNT(*) FROM fountains GROUP BY codi HAVING COUNT(*) > 1;
   ```
2. If duplicates found:
   ```sql
   -- Keep first, delete duplicates (admin only)
   DELETE FROM fountains a USING fountains b
   WHERE a.codi = b.codi AND a.created_at > b.created_at;
   ```
3. Re-import if count still wrong

---

## 📚 Related Documentation

- **Core Schema:** `04-SETUP_GUIDES/SUPABASE_SETUP.md` - Initial profiles, reviews, triggers
- **Edge Functions:** `04-SETUP_GUIDES/EDGE_FUNCTION_SETUP.md` - Account deletion function
- **App Status:** `02-CURRENT_STATE/APP_STATUS.md` - Current implementation state
- **Security:** `03-PRODUCTION/SECURITY_CHECKLIST.md` - RLS policy review

---

## ✅ Migration Checklist

Use this checklist when setting up a new Supabase project:

- [ ] Create Supabase project (West EU region)
- [ ] Save project credentials (URL + Anon Key)
- [ ] Run core schema (profiles, reviews, triggers)
- [ ] Run Migration 1: Fountains
- [ ] Run Migration 2: User Roles
- [ ] Run Migration 3: Admin Management
- [ ] Run Migration 4: Account Deletion
- [ ] Import 1,745 fountain records from CSV
- [ ] Create first user via app
- [ ] Set user as admin: `UPDATE profiles SET role = 'admin' WHERE nickname = '...'`
- [ ] Verify all tables exist
- [ ] Verify RLS enabled
- [ ] Verify views return data
- [ ] Test app connection
- [ ] Test admin features in app

---

**Migrations Complete!** 🎉

Your database is now production-ready with all features enabled.
