-- =====================================================
-- ADMIN FOUNTAIN MANAGEMENT MIGRATION
-- =====================================================
-- This script adds admin functionality for creating and soft-deleting fountains
-- Run this SQL in Supabase SQL Editor

-- =====================================================
-- 1. Add is_deleted column to fountains table
-- =====================================================
ALTER TABLE fountains 
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;

-- Add index for performance (filtering deleted fountains)
CREATE INDEX IF NOT EXISTS idx_fountains_is_deleted ON fountains(is_deleted);

-- =====================================================
-- 2. Update fountain_stats_detailed view to exclude deleted fountains
-- =====================================================
CREATE OR REPLACE VIEW fountain_stats_detailed AS
SELECT
    f.codi,
    f.nom,
    f.carrer,
    f.numero_carrer,
    f.latitude,
    f.longitude,
    f.is_deleted,
    COALESCE(COUNT(r.id), 0)::INTEGER as total_reviews,
    COALESCE(ROUND(AVG(r.overall)::numeric, 2), 0.0) as average_rating
FROM fountains f
LEFT JOIN reviews r ON f.codi = r.fountain_id
WHERE f.is_deleted = FALSE  -- Only include non-deleted fountains
GROUP BY f.codi, f.nom, f.carrer, f.numero_carrer, f.latitude, f.longitude, f.is_deleted;

-- Grant permissions
GRANT SELECT ON fountain_stats_detailed TO anon, authenticated;

-- =====================================================
-- 3. Update RLS policies for fountains table
-- =====================================================

-- Enable RLS if not already enabled
ALTER TABLE fountains ENABLE ROW LEVEL SECURITY;

-- Policy: Everyone can view non-deleted fountains
DROP POLICY IF EXISTS "Anyone can view non-deleted fountains" ON fountains;
CREATE POLICY "Anyone can view non-deleted fountains"
ON fountains
FOR SELECT
USING (is_deleted = FALSE);

-- Policy: Admins can view all fountains (including deleted)
DROP POLICY IF EXISTS "Admins can view all fountains" ON fountains;
CREATE POLICY "Admins can view all fountains"
ON fountains
FOR SELECT
USING (
  is_deleted = FALSE OR
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
);

-- Policy: Only admins can insert fountains
DROP POLICY IF EXISTS "Admins can insert fountains" ON fountains;
CREATE POLICY "Admins can insert fountains"
ON fountains
FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
);

-- Policy: Only admins can update fountains (including soft delete)
DROP POLICY IF EXISTS "Admins can update fountains" ON fountains;
CREATE POLICY "Admins can update fountains"
ON fountains
FOR UPDATE
TO authenticated
USING (
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
)
WITH CHECK (
  EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
);

-- Policy: Prevent physical deletion (soft delete only)
DROP POLICY IF EXISTS "No one can delete fountains" ON fountains;
CREATE POLICY "No one can delete fountains"
ON fountains
FOR DELETE
USING (FALSE);

-- =====================================================
-- 4. Function to soft delete a fountain
-- =====================================================
CREATE OR REPLACE FUNCTION soft_delete_fountain(fountain_codi TEXT)
RETURNS BOOLEAN AS $$
DECLARE
  user_role TEXT;
BEGIN
  -- Check if user is admin
  SELECT role INTO user_role FROM profiles WHERE id = auth.uid();
  
  IF user_role != 'admin' THEN
    RAISE EXCEPTION 'Only admins can delete fountains';
  END IF;
  
  -- Soft delete the fountain
  UPDATE fountains
  SET is_deleted = TRUE
  WHERE codi = fountain_codi;
  
  RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execution permission
GRANT EXECUTE ON FUNCTION soft_delete_fountain(TEXT) TO authenticated;

-- =====================================================
-- 5. Function to restore a deleted fountain
-- =====================================================
CREATE OR REPLACE FUNCTION restore_fountain(fountain_codi TEXT)
RETURNS BOOLEAN AS $$
DECLARE
  user_role TEXT;
BEGIN
  -- Check if user is admin
  SELECT role INTO user_role FROM profiles WHERE id = auth.uid();
  
  IF user_role != 'admin' THEN
    RAISE EXCEPTION 'Only admins can restore fountains';
  END IF;
  
  -- Restore the fountain
  UPDATE fountains
  SET is_deleted = FALSE
  WHERE codi = fountain_codi;
  
  RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execution permission
GRANT EXECUTE ON FUNCTION restore_fountain(TEXT) TO authenticated;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Run these to verify the migration worked:

-- Check if is_deleted column exists
-- SELECT column_name, data_type, column_default 
-- FROM information_schema.columns 
-- WHERE table_name = 'fountains' AND column_name = 'is_deleted';

-- Check RLS policies
-- SELECT schemaname, tablename, policyname, cmd, roles, qual
-- FROM pg_policies
-- WHERE tablename = 'fountains';

-- Test view excludes deleted fountains
-- INSERT INTO fountains (codi, nom, carrer, numero_carrer, latitude, longitude, is_deleted)
-- VALUES ('TEST_DEL', 'Test Deleted', 'Test St', '123', 41.3851, 2.1734, TRUE);
-- 
-- SELECT COUNT(*) FROM fountain_stats_detailed WHERE codi = 'TEST_DEL';
-- Should return 0 (deleted fountain not in view)
-- 
-- DELETE FROM fountains WHERE codi = 'TEST_DEL';

-- =====================================================
-- NOTES
-- =====================================================
-- 1. Soft delete means fountains are marked as deleted but not physically removed
-- 2. Only admins can create, update, or soft-delete fountains
-- 3. The fountain_stats_detailed view automatically filters out deleted fountains
-- 4. Reviews for deleted fountains are preserved (not affected)
-- 5. To restore a fountain, an admin can use: SELECT restore_fountain('fountain_codi');
