package com.watxaut.fontsreviewer.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.watxaut.fontsreviewer.BuildConfig
import com.watxaut.fontsreviewer.R
import com.watxaut.fontsreviewer.domain.model.Fountain

@Composable
fun MapScreen(
    onFountainClick: (String) -> Unit = {},
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
    
    // Listen for review submission to refresh map data
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<Boolean>("reviewSubmitted")?.observeForever { submitted ->
            if (submitted == true) {
                viewModel.refresh()
                savedStateHandle.set("reviewSubmitted", false)
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
        }
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
    onToggleDeletedFountains: () -> Unit
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
    modifier: Modifier = Modifier
) {
    // Barcelona center coordinates
    val barcelonaCenterLat = 41.3874
    val barcelonaCenterLon = 2.1686
    
    // Create viewport state centered on Barcelona
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(barcelonaCenterLon, barcelonaCenterLat))
            zoom(13.0)
            pitch(0.0)
            bearing(0.0)
        }
    }
    
    // Function to center map on user location
    val centerOnUserLocation: () -> Unit = {
        userLocation?.let { location ->
            mapViewportState.transitionToFollowPuckState()
        }
    }
    
    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
        // Separate fountains by type (priority order: best global > user best > user reviewed > deleted > regular)
        val regularFountains = mutableListOf<Pair<Fountain, Int>>()
        val userReviewedFountains = mutableListOf<Pair<Fountain, Int>>()
        val bestFountain = mutableListOf<Pair<Fountain, Int>>()
        val userBestFountain = mutableListOf<Pair<Fountain, Int>>()
        val deletedFountains = mutableListOf<Pair<Fountain, Int>>()
        
        fountains.forEachIndexed { index, fountain ->
            when {
                fountain.isDeleted -> {
                    // Deleted fountains (red/gray - only visible to admins)
                    deletedFountains.add(fountain to index)
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
        
        // Add regular fountain markers (blue circles) - LARGER for easier clicking
        if (regularFountains.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = regularFountains.map { (fountain, index) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0) // Increased from 6.0 to 8.0
                        .withCircleColor("#2196F3") // Blue
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(2.0) // Increased from 1.5 to 2.0
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add user-reviewed fountain markers (green circles) - NEW!
        if (userReviewedFountains.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = userReviewedFountains.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0) // Same size as regular for consistency
                        .withCircleColor("#4CAF50") // Green - matches user best fountain
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(2.0)
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add best fountain marker (largest gold circle with prominent border)
        if (bestFountain.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = bestFountain.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(12.0) // Increased from 10.0 to 12.0
                        .withCircleColor("#FFD700") // Gold
                        .withCircleStrokeColor("#FF8C00") // Dark orange border
                        .withCircleStrokeWidth(3.0) // Increased from 2.5 to 3.0
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add user's best fountain marker (large green circle with special border)
        if (userBestFountain.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = userBestFountain.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(12.0) // Increased from 10.0 to 12.0 - matches best fountain
                        .withCircleColor("#66BB6A") // Lighter green to differentiate from regular reviewed
                        .withCircleStrokeColor("#2E7D32") // Dark green border
                        .withCircleStrokeWidth(3.0) // Increased from 2.5 to 3.0
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add deleted fountain markers (gray/red circles - admin only)
        if (deletedFountains.isNotEmpty() && showDeletedFountains) {
            CircleAnnotationGroup(
                annotations = deletedFountains.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(8.0)
                        .withCircleColor("#757575") // Gray
                        .withCircleStrokeColor("#D32F2F") // Red border to indicate deleted
                        .withCircleStrokeWidth(2.5)
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Extra padding to avoid bottom nav
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
