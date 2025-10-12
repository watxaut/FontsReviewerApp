-- =====================================================
-- USER ROLES MIGRATION
-- =====================================================
-- This script adds user roles (admin/operator) to the app

-- Add role column to profiles table
ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'operator' CHECK (role IN ('admin', 'operator'));

-- Add index for role queries
CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);

-- Update RLS policies to allow admins to bypass location checks
-- (Location checks will be enforced in the app logic)

-- Grant permissions
GRANT SELECT ON profiles TO anon, authenticated;

-- =====================================================
-- HELPER FUNCTION: Check if user is admin
-- =====================================================
CREATE OR REPLACE FUNCTION is_admin(user_uuid UUID)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM profiles
    WHERE id = user_uuid AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- UPDATE: Set initial admin user
-- =====================================================
-- Replace 'your-user-id-here' with your actual user UUID
-- You can find it by running: SELECT id, nickname FROM profiles;
-- Then uncomment and run:

-- UPDATE profiles SET role = 'admin' WHERE id = 'your-user-id-here';

-- Or set by nickname:
-- UPDATE profiles SET role = 'admin' WHERE nickname = 'watxaut';

-- =====================================================
-- NOTES
-- =====================================================
-- Role definitions:
-- - 'admin': Can review any fountain anywhere
-- - 'operator': Can only review fountains within 300m of their location
--
-- Location checks are enforced in the Android app, not in the database
-- This allows for better UX (showing distance, enabling/disabling buttons)
