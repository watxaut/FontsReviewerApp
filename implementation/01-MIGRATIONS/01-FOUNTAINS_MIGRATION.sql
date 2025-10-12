-- =====================================================
-- FOUNTAINS TABLE MIGRATION
-- =====================================================
-- This script creates the fountains table in Supabase
-- and sets up necessary indexes and permissions

-- Create fountains table
CREATE TABLE IF NOT EXISTS fountains (
  codi TEXT PRIMARY KEY,
  nom TEXT NOT NULL,
  carrer TEXT NOT NULL,
  numero_carrer TEXT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_fountains_location ON fountains(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_fountains_nom ON fountains(nom);

-- Enable RLS (Row Level Security)
ALTER TABLE fountains ENABLE ROW LEVEL SECURITY;

-- Allow everyone to read fountains (public data)
CREATE POLICY "Fountains are viewable by everyone"
ON fountains
FOR SELECT
USING (true);

-- Grant permissions to anon and authenticated roles
GRANT SELECT ON fountains TO anon, authenticated;

-- Create updated_at trigger
CREATE TRIGGER trigger_fountains_updated_at
BEFORE UPDATE ON fountains
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ENHANCED FOUNTAIN STATS VIEW
-- =====================================================
-- Drop old view and recreate with fountain info
DROP VIEW IF EXISTS fountain_stats CASCADE;

CREATE OR REPLACE VIEW fountain_stats_detailed AS
SELECT
  f.codi,
  f.nom,
  f.carrer,
  f.numero_carrer,
  f.latitude,
  f.longitude,
  COALESCE(COUNT(r.id), 0)::INTEGER as total_reviews,
  COALESCE(ROUND(AVG(r.overall)::numeric, 2), 0.0) as average_rating,
  COALESCE(ROUND(AVG(r.taste)::numeric, 2), 0.0) as avg_taste,
  COALESCE(ROUND(AVG(r.freshness)::numeric, 2), 0.0) as avg_freshness,
  COALESCE(ROUND(AVG(r.location_rating)::numeric, 2), 0.0) as avg_location,
  COALESCE(ROUND(AVG(r.aesthetics)::numeric, 2), 0.0) as avg_aesthetics,
  MAX(r.created_at) as last_reviewed_at
FROM fountains f
LEFT JOIN reviews r ON f.codi = r.fountain_id
GROUP BY f.codi, f.nom, f.carrer, f.numero_carrer, f.latitude, f.longitude;

-- Keep old view for backward compatibility with existing queries
CREATE OR REPLACE VIEW fountain_stats AS
SELECT
  codi as fountain_id,
  total_reviews,
  average_rating,
  avg_taste,
  avg_freshness,
  avg_location,
  avg_aesthetics,
  last_reviewed_at
FROM fountain_stats_detailed;

-- Grant permissions on views
GRANT SELECT ON fountain_stats_detailed TO anon, authenticated;
GRANT SELECT ON fountain_stats TO anon, authenticated;

-- =====================================================
-- NOTES FOR DATA IMPORT
-- =====================================================
-- After running this script:
-- 1. Go to Supabase Dashboard → Table Editor → fountains
-- 2. Click "Insert" → "Import data from CSV"
-- 3. Upload the fonts-ciutat-de-barcelona.csv file
-- 4. Map columns: codi, nom, carrer, numero_carrer, latitude, longitude
-- 5. Click "Import"
--
-- OR use the SQL import script that will be generated
