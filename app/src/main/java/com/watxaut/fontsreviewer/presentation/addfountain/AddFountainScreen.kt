package com.watxaut.fontsreviewer.presentation.addfountain

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.watxaut.fontsreviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFountainScreen(
    onNavigateBack: () -> Unit,
    onFountainAdded: () -> Unit,
    viewModel: AddFountainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var streetNumber by remember { mutableStateOf("") }
    var selectedLatitude by remember { mutableStateOf<Double?>(null) }
    var selectedLongitude by remember { mutableStateOf<Double?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    
    // Show success dialog and navigate back
    LaunchedEffect(uiState) {
        if (uiState is AddFountainUiState.Success) {
            onFountainAdded()
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_fountain_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showMapPicker) {
                // Map picker mode
                MapPickerView(
                    initialLatitude = selectedLatitude ?: 41.3874, // Barcelona center
                    initialLongitude = selectedLongitude ?: 2.1686,
                    onLocationSelected = { lat, lng ->
                        selectedLatitude = lat
                        selectedLongitude = lng
                        showMapPicker = false
                    },
                    onCancel = { showMapPicker = false }
                )
            } else {
                // Form mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Instructions
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.add_fountain_instructions),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.fountain_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Street field
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text(stringResource(R.string.street)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Street number field
                    OutlinedTextField(
                        value = streetNumber,
                        onValueChange = { streetNumber = it },
                        label = { Text(stringResource(R.string.street_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Location picker button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLatitude != null) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.location),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (selectedLatitude != null && selectedLongitude != null) {
                                        Text(
                                            text = stringResource(
                                                R.string.coordinates_format,
                                                selectedLatitude!!,
                                                selectedLongitude!!
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.location_not_set),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                IconButton(onClick = { showMapPicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = stringResource(R.string.pick_location),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Error message
                    if (uiState is AddFountainUiState.Error) {
                        Text(
                            text = (uiState as AddFountainUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Submit button
                    Button(
                        onClick = {
                            if (selectedLatitude != null && selectedLongitude != null) {
                                viewModel.createFountain(
                                    name = name,
                                    street = street,
                                    streetNumber = streetNumber,
                                    latitude = selectedLatitude!!,
                                    longitude = selectedLongitude!!
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is AddFountainUiState.Loading &&
                                name.isNotBlank() &&
                                street.isNotBlank() &&
                                streetNumber.isNotBlank() &&
                                selectedLatitude != null &&
                                selectedLongitude != null
                    ) {
                        if (uiState is AddFountainUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.add_fountain))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class)
@Composable
fun MapPickerView(
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    var selectedLat by remember { mutableStateOf(initialLatitude) }
    var selectedLng by remember { mutableStateOf(initialLongitude) }
    
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(initialLongitude, initialLatitude))
            zoom(15.0)
            pitch(0.0)
            bearing(0.0)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pick_location_on_map)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                onMapClickListener = { point ->
                    selectedLat = point.latitude()
                    selectedLng = point.longitude()
                    true
                }
            ) {
                // Show selected location marker
                CircleAnnotationGroup(
                    annotations = listOf(
                        CircleAnnotationOptions()
                            .withPoint(Point.fromLngLat(selectedLng, selectedLat))
                            .withCircleRadius(10.0)
                            .withCircleColor("#FF0000") // Red
                            .withCircleStrokeColor("#FFFFFF") // White border
                            .withCircleStrokeWidth(3.0)
                    )
                )
            }
            
            // Instructions overlay
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.tap_map_to_select_location),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Coordinates display
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 80.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.selected_location),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.coordinates_format,
                            selectedLat,
                            selectedLng
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // Confirm button
            FloatingActionButton(
                onClick = { onLocationSelected(selectedLat, selectedLng) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.confirm)
                )
            }
        }
    }
}
