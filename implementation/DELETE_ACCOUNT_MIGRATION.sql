-- =====================================================
-- DELETE ACCOUNT FEATURE - DATABASE SETUP
-- =====================================================
-- This script sets up cascade deletion for user accounts
-- so that when a profile is deleted, all related data is also deleted

-- =====================================================
-- 1. Update Foreign Key Constraints with CASCADE DELETE
-- =====================================================

-- Drop existing foreign key constraint on reviews table
ALTER TABLE reviews 
DROP CONSTRAINT IF EXISTS reviews_user_id_fkey;

-- Re-add with CASCADE DELETE
ALTER TABLE reviews
ADD CONSTRAINT reviews_user_id_fkey 
FOREIGN KEY (user_id) 
REFERENCES profiles(id) 
ON DELETE CASCADE;

-- =====================================================
-- 2. Create Function to Handle Account Deletion
-- =====================================================

CREATE OR REPLACE FUNCTION delete_user_account(user_uuid UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Delete profile (reviews will cascade automatically)
    DELETE FROM profiles WHERE id = user_uuid;
    
    -- Optionally: Delete from auth.users if you have permission
    -- This typically requires admin privileges
    -- DELETE FROM auth.users WHERE id = user_uuid;
    
    RETURN TRUE;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Failed to delete user account: %', SQLERRM;
        RETURN FALSE;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION delete_user_account(UUID) TO authenticated;

-- =====================================================
-- 3. Verify Cascade Delete Setup
-- =====================================================

-- Check that the foreign key constraint has CASCADE DELETE
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
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

-- Expected output:
-- table_name | column_name | foreign_table_name | foreign_column_name | delete_rule
-- -----------|-------------|--------------------|--------------------|-------------
-- reviews    | user_id     | profiles           | id                 | CASCADE

-- =====================================================
-- 4. Test Cascade Delete (Optional - Run in Test Environment)
-- =====================================================

/*
-- Create a test user
INSERT INTO profiles (id, nickname, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'test_user_delete', NOW(), NOW());

-- Create a test review
INSERT INTO reviews (fountain_id, user_id, user_nickname, taste, freshness, location_rating, aesthetics, splash, jet, overall, created_at, updated_at)
VALUES ('test-fountain', '00000000-0000-0000-0000-000000000001'::uuid, 'test_user_delete', 5, 5, 5, 5, 5, 5, 5.0, NOW(), NOW());

-- Verify review exists
SELECT * FROM reviews WHERE user_id = '00000000-0000-0000-0000-000000000001'::uuid;

-- Delete the profile
DELETE FROM profiles WHERE id = '00000000-0000-0000-0000-000000000001'::uuid;

-- Verify review was automatically deleted (should return 0 rows)
SELECT * FROM reviews WHERE user_id = '00000000-0000-0000-0000-000000000001'::uuid;
*/

-- =====================================================
-- 5. Row Level Security (RLS) Policies for Deletion
-- =====================================================

-- Allow users to delete their own profile
CREATE POLICY "Users can delete their own profile"
ON profiles
FOR DELETE
USING (auth.uid() = id);

-- Note: Reviews don't need explicit delete policy since they cascade

-- =====================================================
-- IMPORTANT NOTES
-- =====================================================

/*
IMPORTANT: Supabase Auth User Deletion

The Supabase Kotlin SDK does not provide a direct method to delete auth.users.
When a user deletes their account via the app:

1. The app deletes the profile (which cascades to delete reviews)
2. The app signs out the user
3. The auth.users entry remains in the database

TO FULLY DELETE auth.users, you have 3 options:

OPTION A: Manual Admin Deletion
  - Admin manually deletes from Supabase Dashboard
  - Database → Authentication → Users → Select user → Delete

OPTION B: Supabase Edge Function (Recommended for Production)
  - Create a Supabase Edge Function with admin privileges
  - Call: supabase.auth.admin.deleteUser(userId)
  - The app calls this Edge Function instead of direct deletion

OPTION C: Database Trigger (Advanced)
  - Create a trigger that calls auth.admin_delete_user() when profile is deleted
  - Requires careful setup and testing

For MVP/Barcelona launch, Option A (manual) is acceptable.
For full production, implement Option B.
*/

-- =====================================================
-- 6. Edge Function Example (For Reference)
-- =====================================================

/*
// File: supabase/functions/delete-user-account/index.ts

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  try {
    // Get user from JWT
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )
    
    const { data: { user } } = await supabaseClient.auth.getUser(token)
    
    if (!user) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 })
    }
    
    // Create admin client
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )
    
    // Delete profile (cascades to reviews)
    await supabaseAdmin
      .from('profiles')
      .delete()
      .eq('id', user.id)
    
    // Delete auth user
    await supabaseAdmin.auth.admin.deleteUser(user.id)
    
    return new Response(
      JSON.stringify({ success: true, message: 'Account deleted' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
*/

-- =====================================================
-- 7. Monitoring Query
-- =====================================================

-- Check orphaned reviews (reviews without a profile)
-- Should return 0 rows if cascade delete is working
SELECT r.*
FROM reviews r
LEFT JOIN profiles p ON r.user_id = p.id
WHERE p.id IS NULL;

-- =====================================================
-- END OF MIGRATION SCRIPT
-- =====================================================
