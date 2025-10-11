# Mapbox Setup Instructions

## Step 1: Get Mapbox Tokens

1. Go to [mapbox.com](https://mapbox.com) and sign up (free tier: 50k map loads/month)
2. After signing up, go to **Account → Access Tokens**
3. You need **TWO** tokens:

### Public Token (for displaying the map)
- Find your **Default public token** (starts with `pk.`)
- OR create a new public token:
  - Click "Create a token"
  - Name: "FontsReviewer Public"
  - Public scopes: (all enabled by default)
  - Click "Create token"
- Copy the token (starts with `pk.`)

### Secret Token (for downloading the SDK)
- Create a new **secret token**:
  - Click "Create a token"
  - Name: "FontsReviewer Downloads"
  - **Important**: Check the `downloads:read` scope
  - Click "Create token"
  - **IMPORTANT**: Copy the token immediately (starts with `sk.`) - you won't see it again!

## Step 2: Add Tokens to local.properties

Open `local.properties` in your project root and update these lines:

```properties
# Mapbox Configuration
MAPBOX_PUBLIC_TOKEN=pk.YOUR_PUBLIC_TOKEN_HERE
MAPBOX_DOWNLOADS_TOKEN=sk.YOUR_SECRET_TOKEN_HERE
```

Replace:
- `pk.YOUR_PUBLIC_TOKEN_HERE` with your actual public token
- `sk.YOUR_SECRET_TOKEN_HERE` with your actual secret token

## Step 3: Sync Gradle

1. In Android Studio, click **File → Sync Project with Gradle Files**
2. Wait for the sync to complete
3. Gradle will download the Mapbox SDK (this may take a few minutes the first time)

## Step 4: Build and Run

1. Click **Build → Clean Project**
2. Click **Build → Rebuild Project**
3. Run the app

## What the Map Will Show

- **Center**: Barcelona (41.3874, 2.1686)
- **Zoom level**: 13 (city view)
- **Markers**: All 1745 fountains from `2025_fonts_bcn.csv`
- **Interaction**: Tap any fountain marker to view details

## Troubleshooting

### Error: "Could not resolve com.mapbox.maps:android"
- **Cause**: Secret token is missing or invalid
- **Fix**: 
  1. Verify your `MAPBOX_DOWNLOADS_TOKEN` in `local.properties`
  2. Make sure the token has `downloads:read` scope
  3. Sync Gradle again

### Error: "Mapbox access token not set"
- **Cause**: Public token is missing
- **Fix**: 
  1. Verify your `MAPBOX_PUBLIC_TOKEN` in `local.properties`
  2. Rebuild the project

### Map shows but no markers
- **Cause**: Fountains not loaded from CSV
- **Fix**: Check Logcat for errors in CSV parsing

### Map is blank or shows wrong location
- **Cause**: Coordinates might be swapped
- **Fix**: Coordinates are (longitude, latitude) not (latitude, longitude)

## Current Implementation

The map is configured to:
- Load all fountains from the local Room database
- Display them as markers with the built-in "water-15" icon
- Show fountain names as labels
- Handle clicks to navigate to fountain details

## Future Enhancements

Consider adding:
- User location tracking
- Clustering for better performance with many markers
- Custom fountain icons
- Different colors for rated vs unrated fountains
- Heatmap showing fountain ratings
