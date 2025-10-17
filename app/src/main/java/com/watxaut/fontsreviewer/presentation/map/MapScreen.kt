package com.watxaut.fontsreviewer.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
import com.watxaut.fontsreviewer.util.LocationUtil
import kotlinx.coroutines.launch
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.watxaut.fontsreviewer.BuildConfig
import com.watxaut.fontsreviewer.R
import com.watxaut.fontsreviewer.domain.model.Fountain

@Composable
fun MapScreen(
    onFountainClick: (String) -> Unit = {},
    onAddFountain: () -> Unit = {},
    savedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val showDeletedFountains by viewModel.showDeletedFountains.collectAsState()
    var selectedFountainId by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(LocationUtil.hasLocationPermission(context))
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                               permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        
        if (hasLocationPermission) {
            // Get location after permission granted
            scope.launch {
                val location = LocationUtil.getCurrentLocation(context)
                viewModel.updateUserLocation(location)
            }
        }
    }
    
    // Request location on first composition if not granted
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            // Get current location
            val location = LocationUtil.getCurrentLocation(context)
            viewModel.updateUserLocation(location)
        }
    }
    
    // Listen for review submission or fountain addition to refresh map data
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<Boolean>("reviewSubmitted")?.observeForever { submitted ->
            if (submitted == true) {
                viewModel.refresh()
                savedStateHandle.set("reviewSubmitted", false)
            }
        }
        
        savedStateHandle?.getLiveData<Boolean>("fountainAdded")?.observeForever { added ->
            if (added == true) {
                viewModel.refresh()
                savedStateHandle.set("fountainAdded", false)
            }
        }
    }

    MapScreenContent(
        uiState = uiState,
        userLocation = userLocation,
        selectedFountainId = selectedFountainId,
        showDeletedFountains = showDeletedFountains,
        onFountainClick = { fountainId ->
            selectedFountainId = fountainId
        },
        onDismissSheet = { selectedFountainId = null },
        onViewDetails = { fountainId ->
            selectedFountainId = null
            onFountainClick(fountainId)
        },
        onRefreshLocation = {
            scope.launch {
                val location = LocationUtil.getCurrentLocation(context)
                viewModel.updateUserLocation(location)
            }
        },
        onToggleDeletedFountains = {
            viewModel.toggleShowDeletedFountains()
        },
        onAddFountain = onAddFountain
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    uiState: MapUiState,
    userLocation: android.location.Location?,
    selectedFountainId: String?,
    showDeletedFountains: Boolean,
    onFountainClick: (String) -> Unit,
    onDismissSheet: () -> Unit,
    onViewDetails: (String) -> Unit,
    onRefreshLocation: () -> Unit,
    onToggleDeletedFountains: () -> Unit,
    onAddFountain: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is MapUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is MapUiState.Empty -> {
                Text(
                    text = stringResource(R.string.error_loading_fountains),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            is MapUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.error),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is MapUiState.Success -> {
                MapboxMapView(
                    fountains = uiState.fountains,
                    bestFountainId = uiState.bestFountainId,
                    userBestFountainId = uiState.userBestFountainId,
                    userReviewedFountainIds = uiState.userReviewedFountainIds,
                    userLocation = userLocation,
                    currentUser = uiState.currentUser,
                    showDeletedFountains = showDeletedFountains,
                    onFountainClick = onFountainClick,
                    onToggleDeletedFountains = onToggleDeletedFountains,
                    onAddFountain = onAddFountain,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Bottom sheet for quick actions when fountain is selected
        if (selectedFountainId != null && uiState is MapUiState.Success) {
            val selectedFountain = uiState.fountains.find { it.codi == selectedFountainId }
            selectedFountain?.let { fountain ->
                ModalBottomSheet(
                    onDismissRequest = onDismissSheet,
                    sheetState = sheetState
                ) {
                    FountainQuickActionSheet(
                        fountain = fountain,
                        isAuthenticated = uiState.currentUser != null,
                        onViewDetails = { onViewDetails(fountain.codi) },
                        onDismiss = onDismissSheet
                    )
                }
            }
        }
    }
}

/**
 * Helper function to load drawable as bitmap for Mapbox
 */
@Composable
private fun rememberDrawableBitmap(drawableId: Int): Bitmap? {
    val context = LocalContext.current
    return remember(drawableId) {
        try {
            val drawable = ContextCompat.getDrawable(context, drawableId)
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else null
        } catch (e: Exception) {
            com.watxaut.fontsreviewer.util.SecureLog.e("MapScreen", "Failed to load drawable $drawableId", e)
            null
        }
    }
}

@OptIn(MapboxExperimental::class)
@Composable
fun MapboxMapView(
    fountains: List<Fountain>,
    bestFountainId: String?,
    userBestFountainId: String?,
    userReviewedFountainIds: Set<String>,
    userLocation: android.location.Location?,
    currentUser: com.watxaut.fontsreviewer.domain.model.User?,
    showDeletedFountains: Boolean,
    onFountainClick: (String) -> Unit,
    onToggleDeletedFountains: () -> Unit,
    onAddFountain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Barcelona center coordinates
    val barcelonaCenterLat = 41.3874
    val barcelonaCenterLon = 2.1686
    
    // Load fountain marker images
    val regularIcon = rememberDrawableBitmap(R.drawable.ic_fountain_regular)
    val reviewedIcon = rememberDrawableBitmap(R.drawable.ic_fountain_reviewed)
    val bestIcon = rememberDrawableBitmap(R.drawable.ic_fountain_best)
    val userBestIcon = rememberDrawableBitmap(R.drawable.ic_fountain_user_best)
    val deletedIcon = rememberDrawableBitmap(R.drawable.ic_fountain_deleted)
    
    // Create viewport state centered on Barcelona
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(barcelonaCenterLon, barcelonaCenterLat))
            zoom(13.0)
            pitch(0.0)
            bearing(0.0)
        }
    }
    
    // Track current zoom level for hybrid rendering
    var currentZoom by remember { mutableStateOf(13.0) }
    
    // Update zoom level when camera changes
    LaunchedEffect(mapViewportState.cameraState) {
        currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
    }
    
    // Function to center map on user location
    val centerOnUserLocation: () -> Unit = {
        userLocation?.let { location ->
            mapViewportState.transitionToFollowPuckState()
        }
    }
    
    // Images are loaded by Mapbox automatically from drawable resources
    // No need to manually add them to style when using PointAnnotation with iconImage
    
    // Separate fountains by type OUTSIDE MapboxMap to prevent recomposition issues
    // Using remember to maintain stable references
    val categorizedFountains = remember(fountains, bestFountainId, userBestFountainId, userReviewedFountainIds, showDeletedFountains) {
        val regularFountains = mutableListOf<Pair<Fountain, Int>>()
        val userReviewedFountains = mutableListOf<Pair<Fountain, Int>>()
        val bestFountain = mutableListOf<Pair<Fountain, Int>>()
        val userBestFountain = mutableListOf<Pair<Fountain, Int>>()
        val deletedFountains = mutableListOf<Pair<Fountain, Int>>()
        
        fountains.forEachIndexed { index, fountain ->
            when {
                fountain.isDeleted -> {
                    // Deleted fountains (red/gray - only visible to admins)
                    if (showDeletedFountains) {
                        deletedFountains.add(fountain to index)
                    }
                }
                fountain.codi == bestFountainId -> {
                    // Best rated fountain globally (gold)
                    bestFountain.add(fountain to index)
                }
                fountain.codi == userBestFountainId && fountain.codi != bestFountainId -> {
                    // User's personal best fountain (special green with star effect)
                    userBestFountain.add(fountain to index)
                }
                fountain.codi in userReviewedFountainIds -> {
                    // Fountains user has reviewed (green)
                    userReviewedFountains.add(fountain to index)
                }
                else -> {
                    // Regular fountains (blue)
                    regularFountains.add(fountain to index)
                }
            }
        }
        
        mapOf(
            "regular" to regularFountains,
            "reviewed" to userReviewedFountains,
            "best" to bestFountain,
            "userBest" to userBestFountain,
            "deleted" to deletedFountains
        )
    }
    
    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
        // Extract categorized fountains from stable map
        val regularFountains = categorizedFountains["regular"] ?: emptyList()
        val userReviewedFountains = categorizedFountains["reviewed"] ?: emptyList()
        val bestFountain = categorizedFountains["best"] ?: emptyList()
        val userBestFountain = categorizedFountains["userBest"] ?: emptyList()
        val deletedFountains = categorizedFountains["deleted"] ?: emptyList()
        
        // Add regular fountain markers - Always use circles for performance (too many to render as images)
        if (regularFountains.isNotEmpty()) {
            // Create stable annotation list with remember to prevent recreation
            val regularAnnotations = remember(regularFountains) {
                regularFountains.map { (fountain, index) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0)
                        .withCircleColor("#2196F3") // Blue
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(2.0)
                        .withCircleSortKey(1.0) // Lower sort key = rendered first (behind other layers)
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                }
            }
            
            CircleAnnotationGroup(
                annotations = regularAnnotations,
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add user-reviewed fountain markers - Always use circles for performance
        if (userReviewedFountains.isNotEmpty()) {
            val reviewedAnnotations = remember(userReviewedFountains) {
                userReviewedFountains.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0)
                        .withCircleColor("#4CAF50") // Green
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(2.0)
                        .withCircleSortKey(2.0) // Higher than regular = rendered on top
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                }
            }
            
            CircleAnnotationGroup(
                annotations = reviewedAnnotations,
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add best fountain marker - Show image only when very zoomed in (1 marker = no performance impact)
        if (bestFountain.isNotEmpty()) {
            if (currentZoom >= 16.0 && bestIcon != null) {
                // Very zoomed in - show gold image with crown
                val bestImageAnnotations = remember(bestFountain, bestIcon) {
                    bestFountain.map { (fountain, _) ->
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                            .withIconImage(bestIcon)
                            .withIconSize(1.3)
                            .withData(com.google.gson.JsonPrimitive(fountain.codi))
                    }
                }
                PointAnnotationGroup(
                    annotations = bestImageAnnotations,
                    onClick = { annotation ->
                        val codi = annotation.getData()?.asString ?: return@PointAnnotationGroup false
                        onFountainClick(codi)
                        true
                    }
                )
            } else {
                // Default - show larger gold circle
                val bestCircleAnnotations = remember(bestFountain) {
                    bestFountain.map { (fountain, _) ->
                        CircleAnnotationOptions()
                            .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                            .withCircleRadius(12.0)
                            .withCircleColor("#FFD700")
                            .withCircleStrokeColor("#FF8C00")
                            .withCircleStrokeWidth(3.0)
                            .withCircleSortKey(4.0) // Highest priority = always on top
                            .withData(com.google.gson.JsonPrimitive(fountain.codi))
                    }
                }
                CircleAnnotationGroup(
                    annotations = bestCircleAnnotations,
                    onClick = { annotation ->
                        val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                        onFountainClick(codi)
                        true
                    }
                )
            }
        }
        
        // Add user's best fountain marker - Show image only when very zoomed in (1 marker = no performance impact)
        if (userBestFountain.isNotEmpty()) {
            if (currentZoom >= 16.0 && userBestIcon != null) {
                // Very zoomed in - show green image with star
                val userBestImageAnnotations = remember(userBestFountain, userBestIcon) {
                    userBestFountain.map { (fountain, _) ->
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                            .withIconImage(userBestIcon)
                            .withIconSize(1.3)
                            .withData(com.google.gson.JsonPrimitive(fountain.codi))
                    }
                }
                PointAnnotationGroup(
                    annotations = userBestImageAnnotations,
                    onClick = { annotation ->
                        val codi = annotation.getData()?.asString ?: return@PointAnnotationGroup false
                        onFountainClick(codi)
                        true
                    }
                )
            } else {
                // Default - show larger green circle
                val userBestCircleAnnotations = remember(userBestFountain) {
                    userBestFountain.map { (fountain, _) ->
                        CircleAnnotationOptions()
                            .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                            .withCircleRadius(12.0)
                            .withCircleColor("#66BB6A")
                            .withCircleStrokeColor("#2E7D32")
                            .withCircleStrokeWidth(3.0)
                            .withCircleSortKey(3.0) // High priority = on top of regular markers
                            .withData(com.google.gson.JsonPrimitive(fountain.codi))
                    }
                }
                CircleAnnotationGroup(
                    annotations = userBestCircleAnnotations,
                    onClick = { annotation ->
                        val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                        onFountainClick(codi)
                        true
                    }
                )
            }
        }
        
        // Add deleted fountain markers - Always use circles (admin only, already filtered in categorization)
        if (deletedFountains.isNotEmpty()) {
            val deletedAnnotations = remember(deletedFountains) {
                deletedFountains.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0)
                        .withCircleColor("#757575")
                        .withCircleStrokeColor("#D32F2F")
                        .withCircleStrokeWidth(2.5)
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                }
            }
            CircleAnnotationGroup(
                annotations = deletedAnnotations,
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add user location marker (red circle with white border)
        if (userLocation != null) {
            CircleAnnotationGroup(
                annotations = listOf(
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                        .withCircleRadius(8.0)
                        .withCircleColor("#FF0000") // Red
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(3.0)
                )
            )
        }
        }
        
        // Admin controls column (bottom-right, above bottom nav)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp), // Extra padding to avoid bottom nav
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Admin buttons - only show if user is admin
            if (currentUser?.role == com.watxaut.fontsreviewer.domain.model.UserRole.ADMIN) {
                // Add fountain button
                FloatingActionButton(
                    onClick = onAddFountain,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new fountain",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Toggle deleted fountains visibility button
                FloatingActionButton(
                    onClick = onToggleDeletedFountains,
                    containerColor = if (showDeletedFountains) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (showDeletedFountains) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                ) {
                    Icon(
                        imageVector = if (showDeletedFountains) {
                            androidx.compose.material.icons.Icons.Default.VisibilityOff
                        } else {
                            androidx.compose.material.icons.Icons.Default.Visibility
                        },
                        contentDescription = if (showDeletedFountains) {
                            "Hide deleted fountains"
                        } else {
                            "Show deleted fountains"
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Location button - floating action button to center on user
            if (userLocation != null) {
                FloatingActionButton(
                    onClick = {
                        // Center the map on user location
                        mapViewportState.flyTo(
                            cameraOptions = com.mapbox.maps.CameraOptions.Builder()
                                .center(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                                .zoom(16.0) // Closer zoom when centering on user
                                .build()
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.center_on_location),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FountainQuickActionSheet(
    fountain: Fountain,
    isAuthenticated: Boolean,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fountain info
        Text(
            text = fountain.nom,
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = "${fountain.carrer}, ${fountain.numeroCarrer}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (fountain.totalReviews > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐ ${"%.1f".format(fountain.averageRating)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "(${fountain.totalReviews} ${if (fountain.totalReviews == 1) "review" else "reviews"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No reviews yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons
        Button(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isAuthenticated) "View Details & Rate" else "View Details")
        }
        
        if (!isAuthenticated) {
            Text(
                text = "Sign in to rate this fountain",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
