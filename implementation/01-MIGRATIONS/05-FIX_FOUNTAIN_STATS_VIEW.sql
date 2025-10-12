-- =====================================================
-- Fix fountain_stats_detailed View
-- =====================================================
-- This script fixes the view to include the is_deleted column
-- for proper fountain management (soft delete support)

-- Step 1: Drop the existing view (and cascade to dependent objects if any)
DROP VIEW IF EXISTS fountain_stats_detailed CASCADE;

-- Step 2: Recreate the view with the correct structure
CREATE VIEW fountain_stats_detailed AS
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

-- Step 3: Grant permissions
GRANT SELECT ON fountain_stats_detailed TO anon, authenticated;

-- Step 4: Verify the view works
SELECT * FROM fountain_stats_detailed LIMIT 5;

-- =====================================================
-- Optional: Also recreate the old fountain_stats view
-- (for backward compatibility if needed)
-- =====================================================
DROP VIEW IF EXISTS fountain_stats CASCADE;

CREATE VIEW fountain_stats AS
SELECT
    fountain_id,
    COUNT(*) as total_reviews,
    ROUND(AVG(overall)::numeric, 2) as average_rating,
    ROUND(AVG(taste)::numeric, 2) as avg_taste,
    ROUND(AVG(freshness)::numeric, 2) as avg_freshness,
    ROUND(AVG(location_rating)::numeric, 2) as avg_location,
    ROUND(AVG(aesthetics)::numeric, 2) as avg_aesthetics,
    ROUND(AVG(splash)::numeric, 2) as avg_splash,
    ROUND(AVG(jet)::numeric, 2) as avg_jet,
    MAX(created_at) as last_reviewed_at
FROM reviews
GROUP BY fountain_id;

GRANT SELECT ON fountain_stats TO anon, authenticated;

-- =====================================================
-- Verification Queries
-- =====================================================

-- Check if view exists and has correct columns
SELECT 
    column_name, 
    data_type 
FROM information_schema.columns 
WHERE table_name = 'fountain_stats_detailed' 
ORDER BY ordinal_position;

-- Count total fountains
SELECT COUNT(*) as total_fountains FROM fountain_stats_detailed;

-- Count fountains with reviews
SELECT COUNT(*) as fountains_with_reviews 
FROM fountain_stats_detailed 
WHERE total_reviews > 0;

-- Show top 5 highest rated fountains
SELECT 
    nom,
    carrer,
    total_reviews,
    average_rating
FROM fountain_stats_detailed
WHERE total_reviews > 0
ORDER BY average_rating DESC, total_reviews DESC
LIMIT 5;
