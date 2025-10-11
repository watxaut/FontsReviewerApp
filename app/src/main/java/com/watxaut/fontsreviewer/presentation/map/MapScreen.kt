package com.watxaut.fontsreviewer.presentation.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
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
    var selectedFountainId by remember { mutableStateOf<String?>(null) }
    
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
        selectedFountainId = selectedFountainId,
        onFountainClick = { fountainId ->
            selectedFountainId = fountainId
        },
        onDismissSheet = { selectedFountainId = null },
        onViewDetails = { fountainId ->
            selectedFountainId = null
            onFountainClick(fountainId)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    uiState: MapUiState,
    selectedFountainId: String?,
    onFountainClick: (String) -> Unit,
    onDismissSheet: () -> Unit,
    onViewDetails: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.map)) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                        onFountainClick = onFountainClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
    onFountainClick: (String) -> Unit,
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
    
    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState
    ) {
        // Separate fountains by type
        val regularFountains = mutableListOf<Pair<Fountain, Int>>()
        val bestFountain = mutableListOf<Pair<Fountain, Int>>()
        val userBestFountain = mutableListOf<Pair<Fountain, Int>>()
        
        // Debug logging
        android.util.Log.i("MapScreen", "=== RENDERING MARKERS ===")
        android.util.Log.i("MapScreen", "bestFountainId: $bestFountainId")
        android.util.Log.i("MapScreen", "userBestFountainId: $userBestFountainId")
        
        fountains.forEachIndexed { index, fountain ->
            when {
                fountain.codi == userBestFountainId && fountain.codi != bestFountainId -> {
                    android.util.Log.i("MapScreen", "User best fountain: ${fountain.codi} - ${fountain.nom}")
                    userBestFountain.add(fountain to index)
                }
                fountain.codi == bestFountainId -> {
                    android.util.Log.i("MapScreen", "Best fountain: ${fountain.codi} - ${fountain.nom}")
                    bestFountain.add(fountain to index)
                }
                else -> 
                    regularFountains.add(fountain to index)
            }
        }
        
        android.util.Log.i("MapScreen", "Regular fountains: ${regularFountains.size}")
        android.util.Log.i("MapScreen", "Best fountain markers: ${bestFountain.size}")
        android.util.Log.i("MapScreen", "User best fountain markers: ${userBestFountain.size}")
        
        // Add regular fountain markers (simple blue circles)
        if (regularFountains.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = regularFountains.map { (fountain, index) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(6.0)
                        .withCircleColor("#2196F3") // Blue
                        .withCircleStrokeColor("#FFFFFF") // White border
                        .withCircleStrokeWidth(1.5)
                        .withData(com.google.gson.JsonPrimitive(fountain.codi))
                },
                onClick = { annotation ->
                    val codi = annotation.getData()?.asString ?: return@CircleAnnotationGroup false
                    onFountainClick(codi)
                    true
                }
            )
        }
        
        // Add best fountain marker (larger gold circle)
        if (bestFountain.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = bestFountain.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(10.0)
                        .withCircleColor("#FFD700") // Gold
                        .withCircleStrokeColor("#FF8C00") // Dark orange border
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
        
        // Add user's best fountain marker (larger green circle)
        if (userBestFountain.isNotEmpty()) {
            CircleAnnotationGroup(
                annotations = userBestFountain.map { (fountain, _) ->
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(fountain.longitude, fountain.latitude))
                        .withCircleRadius(10.0)
                        .withCircleColor("#4CAF50") // Green
                        .withCircleStrokeColor("#2E7D32") // Dark green border
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
