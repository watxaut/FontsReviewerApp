# Importing Fountains to Supabase

This guide will help you import the 1745 Barcelona fountains into your Supabase database.

## ⚠️ Important Note About Supabase Limits

**Supabase PostgREST has a default max-rows limit of 1000.** This means queries will only return 1000 rows by default, even if you request more.

**Solution Implemented:** The app now uses **pagination** to fetch all fountains in batches of 1000, automatically combining them into the complete dataset. This works regardless of the Supabase server configuration.

## Step 1: Run the SQL Migration Script

1. Go to your Supabase Dashboard
2. Navigate to **SQL Editor**
3. Click **"New query"**
4. Copy and paste the contents of `FOUNTAINS_MIGRATION.sql`
5. Click **"Run"** to execute
6. ✅ This creates the `fountains` table, indexes, RLS policies, and views

## Step 2: Import CSV Data

There are **two methods** to import the fountain data:

### Method A: Using Supabase Dashboard (Recommended - Easiest)

1. Go to **Table Editor** → Select **`fountains`** table
2. Click **"Insert"** dropdown → **"Import data from CSV"**
3. Upload `app/src/main/assets/fonts-ciutat-de-barcelona.csv`
4. **Map the columns:**
   - CSV `codi` → Table `codi`
   - CSV `nom` → Table `nom`
   - CSV `carrer` → Table `carrer`
   - CSV `numero_carrer` → Table `numero_carrer`
   - CSV `latitude` → Table `latitude`
   - CSV `longitude` → Table `longitude`
5. Click **"Import"**
6. Wait for import to complete (~30 seconds for 1745 rows)
7. ✅ Done!

### Method B: Using SQL Script (Alternative)

If the CSV import doesn't work, you can generate SQL INSERT statements:

1. Use the provided Python script (see below) to convert CSV to SQL
2. Run the generated SQL in the SQL Editor

#### Python Script to Generate SQL

```python
import csv

# Read the CSV file
csv_file_path = 'app/src/main/assets/fonts-ciutat-de-barcelona.csv'

print("BEGIN;")
print()

with open(csv_file_path, 'r', encoding='utf-8') as file:
    csv_reader = csv.DictReader(file)
    
    for row in csv_reader:
        codi = row['codi'].replace("'", "''")
        nom = row['nom'].replace("'", "''")
        carrer = row['carrer'].replace("'", "''")
        numero_carrer = row['numero_carrer'].replace("'", "''")
        latitude = row['latitude']
        longitude = row['longitude']
        
        sql = f"INSERT INTO fountains (codi, nom, carrer, numero_carrer, latitude, longitude) VALUES ('{codi}', '{nom}', '{carrer}', '{numero_carrer}', {latitude}, {longitude});"
        print(sql)

print()
print("COMMIT;")
```

**Usage:**
```bash
python3 generate_fountain_sql.py > import_fountains.sql
```

Then copy the contents of `import_fountains.sql` and run it in Supabase SQL Editor.

## Step 3: Verify the Import

Run this query in SQL Editor to verify:

```sql
-- Check total count
SELECT COUNT(*) as total_fountains FROM fountains;
-- Should return: 1745

-- Check first few fountains
SELECT * FROM fountains LIMIT 5;

-- Check fountain stats view
SELECT * FROM fountain_stats_detailed LIMIT 5;

-- Check if views work correctly
SELECT COUNT(*) FROM fountain_stats_detailed;
-- Should return: 1745
```

## Step 4: Test the App

1. **Build and run the app**
2. **Open the Map screen**
3. **Check the logs:**
   ```
   I/FountainRepository: Fetching all fountains from Supabase...
   I/FountainRepository: Fetched 1745 fountains from Supabase
   I/FountainRepository: Fountains with reviews: 2
   I/MapViewModel: === MAP DATA ===
   I/MapViewModel: Total fountains: 1745
   I/MapViewModel: Fountains with reviews: 2
   I/MapViewModel: Best fountain globally: <fountain_codi>
   I/MapViewModel: Current user: <your_nickname>
   I/MapViewModel: User best fountain: <fountain_codi>
   ```

4. **You should now see:**
   - ✅ All 1745 fountains on the map (blue circles)
   - ✅ Best rated fountain highlighted (gold circle)
   - ✅ Your best fountain highlighted (green circle)
   - ✅ Correct review counts and ratings

## Troubleshooting

### Import fails with "too many rows"
- Split the SQL into batches of 500 rows
- Run each batch separately

### "fountain_stats_detailed view not found"
- Make sure you ran the FOUNTAINS_MIGRATION.sql completely
- Check that all views were created successfully

### App still shows "Fountains with reviews: 0"
- Clear app data and restart
- Check Supabase logs for any errors
- Verify RLS policies are set correctly

### No fountains showing on map
- Check network connection
- Look for errors in logcat
- Verify Supabase credentials in `local.properties`
- App will fallback to local CSV if Supabase fails

## Benefits After Migration ✨

✅ **Single Query**: One request gets all fountain data + stats
✅ **Always Up-to-Date**: Reviews instantly update fountain ratings
✅ **Better Performance**: No client-side data merging
✅ **User Best Fountain Works**: Automatically highlighted on map
✅ **Server-Side Filtering**: Can add search/filter features easily
✅ **Scalable**: Ready for future features like geospatial queries

## Next Steps

After successful import:
- ✅ Fountains load from Supabase
- ✅ Review statistics show correctly
- ✅ Best fountain markers work
- ✅ Map refreshes after rating

You can now safely keep the local CSV as a fallback, or remove it entirely in a future update.
