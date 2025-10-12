package com.watxaut.fontsreviewer.presentation.details

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watxaut.fontsreviewer.R
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.model.UserRole
import com.watxaut.fontsreviewer.util.LocationUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FountainDetailsScreen(
    onNavigateBack: () -> Unit,
    onAddReview: () -> Unit,
    savedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
    viewModel: FountainDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var locationDialogMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                     permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        
        if (granted) {
            scope.launch {
                userLocation = LocationUtil.getCurrentLocation(context)
                checkLocationAndNavigate(
                    uiState = uiState,
                    userLocation = userLocation,
                    onAddReview = onAddReview,
                    onShowDialog = { message ->
                        locationDialogMessage = message
                        showLocationDialog = true
                    }
                )
            }
        } else {
            locationDialogMessage = "Location permission is required for operators to review fountains within 300m"
            showLocationDialog = true
        }
    }
    
    // Listen for review submission to refresh fountain details
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<Boolean>("reviewSubmitted")?.observeForever { submitted ->
            if (submitted == true) {
                viewModel.onRefresh()
                savedStateHandle.set("reviewSubmitted", false)
            }
        }
    }

    FountainDetailsContent(
        uiState = uiState,
        currentUser = viewModel.getCurrentUser(),
        onNavigateBack = onNavigateBack,
        onDeleteFountain = { showDeleteDialog = true },
        onAddReview = {
            // Check user role and location before allowing review
            val successState = uiState as? FountainDetailsUiState.Success
            if (successState != null) {
                // Check if user has already reviewed
                if (successState.userHasReviewed) {
                    locationDialogMessage = "You have already reviewed this fountain.\n\nYou can only review each fountain once."
                    showLocationDialog = true
                    return@FountainDetailsContent
                }
                
                val currentUser = viewModel.getCurrentUser()
                
                when {
                    currentUser == null -> {
                        locationDialogMessage = "You must be logged in to review fountains"
                        showLocationDialog = true
                    }
                    currentUser.role == UserRole.ADMIN -> {
                        // Admins can review any fountain
                        onAddReview()
                    }
                    currentUser.role == UserRole.OPERATOR -> {
                        // Operators need location check
                        if (!LocationUtil.hasLocationPermission(context)) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        } else {
                            scope.launch {
                                userLocation = LocationUtil.getCurrentLocation(context)
                                checkLocationAndNavigate(
                                    uiState = successState,
                                    userLocation = userLocation,
                                    onAddReview = onAddReview,
                                    onShowDialog = { message ->
                                        locationDialogMessage = message
                                        showLocationDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        onRefresh = viewModel::onRefresh
    )
    
    // Location restriction dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location Required") },
            text = { Text(locationDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        val fountainName = (uiState as? FountainDetailsUiState.Success)?.fountain?.nom ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = { 
                Text(stringResource(R.string.confirm_delete_fountain_message) + "\n\nFountain: $fountainName")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteFountain(
                            onSuccess = {
                                // Navigate back to map
                                onNavigateBack()
                            },
                            onError = { error ->
                                locationDialogMessage = error
                                showLocationDialog = true
                            }
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun checkLocationAndNavigate(
    uiState: FountainDetailsUiState,
    userLocation: android.location.Location?,
    onAddReview: () -> Unit,
    onShowDialog: (String) -> Unit
) {
    if (uiState !is FountainDetailsUiState.Success) return
    
    if (userLocation == null) {
        onShowDialog("Unable to get your location. Please make sure location services are enabled.")
        return
    }
    
    val fountain = uiState.fountain
    val distance = LocationUtil.calculateDistance(
        userLocation.latitude,
        userLocation.longitude,
        fountain.latitude,
        fountain.longitude
    )
    
    if (distance <= 300.0) {
        // Within range, allow review
        onAddReview()
    } else {
        // Too far away
        val distanceKm = distance / 1000.0
        onShowDialog(
            "You must be within 300m of this fountain to review it.\n\n" +
            "Current distance: %.2f km (%.0f meters)".format(distanceKm, distance)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FountainDetailsContent(
    uiState: FountainDetailsUiState,
    currentUser: User?,
    onNavigateBack: () -> Unit,
    onDeleteFountain: () -> Unit,
    onAddReview: () -> Unit,
    onRefresh: () -> Unit
) {
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isDeleting = (uiState as? FountainDetailsUiState.Success)?.isDeleting == true
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.fountain_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Delete button for admins only
                    if (isAdmin) {
                        IconButton(
                            onClick = onDeleteFountain,
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_fountain),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is FountainDetailsUiState.Success && !uiState.userHasReviewed) {
                FloatingActionButton(onClick = onAddReview) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_review)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is FountainDetailsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is FountainDetailsUiState.Error -> {
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
                        Text(text = uiState.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text(text = "Retry")
                        }
                    }
                }

                is FountainDetailsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Fountain info card
                        item {
                            FountainInfoCard(fountain = uiState.fountain)
                        }
                        
                        // Show indicator if user has already reviewed
                        if (uiState.userHasReviewed) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "You have already reviewed this fountain",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Reviews section header
                        item {
                            Text(
                                text = stringResource(R.string.reviews) + " (${uiState.reviews.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Reviews error if any
                        if (uiState.reviewsError != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = uiState.reviewsError,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Reviews list
                        if (uiState.reviews.isEmpty()) {
                            item {
                                Card {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_reviews),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.be_first_review),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.reviews) { review ->
                                ReviewCard(review = review)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FountainInfoCard(fountain: com.watxaut.fontsreviewer.domain.model.Fountain) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = fountain.nom,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${fountain.carrer}, ${fountain.numeroCarrer}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Code: ${fountain.codi}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lat: %.4f, Lon: %.4f".format(fountain.latitude, fountain.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (fountain.totalReviews > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.average_score),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "%.2f / 5.0".format(fountain.averageRating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.ratings),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = fountain.totalReviews.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userNickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(review.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating categories
            RatingRow(label = stringResource(R.string.taste), rating = review.taste)
            Spacer(modifier = Modifier.height(4.dp))
            RatingRow(label = stringResource(R.string.freshness), rating = review.freshness)
            Spacer(modifier = Modifier.height(4.dp))
            RatingRow(label = stringResource(R.string.location), rating = review.locationRating)
            Spacer(modifier = Modifier.height(4.dp))
            RatingRow(label = stringResource(R.string.aesthetics), rating = review.aesthetics)

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Overall score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overall),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.1f / 5.0".format(review.overall),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Comment if exists
            if (!review.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun RatingRow(label: String, rating: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "$rating / 5",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
