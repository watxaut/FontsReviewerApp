package com.watxaut.fontsreviewer.presentation.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watxaut.fontsreviewer.R

@Composable
fun ReviewScreen(
    onNavigateBack: () -> Unit,
    onReviewSubmitted: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            onReviewSubmitted()
            viewModel.onSubmitSuccess()
        }
    }

    ReviewScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTasteChange = viewModel::onTasteChange,
        onFreshnessChange = viewModel::onFreshnessChange,
        onLocationChange = viewModel::onLocationChange,
        onAestheticsChange = viewModel::onAestheticsChange,
        onSplashChange = viewModel::onSplashChange,
        onJetChange = viewModel::onJetChange,
        onSubmit = viewModel::onSubmit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreenContent(
    uiState: ReviewUiState,
    onNavigateBack: () -> Unit,
    onTasteChange: (Float) -> Unit,
    onFreshnessChange: (Float) -> Unit,
    onLocationChange: (Float) -> Unit,
    onAestheticsChange: (Float) -> Unit,
    onSplashChange: (Float) -> Unit,
    onJetChange: (Float) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.submit_review)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.submit_review),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Taste rating
            RatingSlider(
                label = stringResource(R.string.taste),
                value = uiState.taste,
                onValueChange = onTasteChange
            )

            // Freshness rating
            RatingSlider(
                label = stringResource(R.string.freshness),
                value = uiState.freshness,
                onValueChange = onFreshnessChange
            )

            // Location rating
            RatingSlider(
                label = stringResource(R.string.location),
                value = uiState.locationRating,
                onValueChange = onLocationChange
            )

            // Aesthetics rating
            RatingSlider(
                label = stringResource(R.string.aesthetics),
                value = uiState.aesthetics,
                onValueChange = onAestheticsChange
            )

            // Splash rating
            RatingSlider(
                label = stringResource(R.string.splash),
                value = uiState.splash,
                onValueChange = onSplashChange
            )

            // Jet rating
            RatingSlider(
                label = stringResource(R.string.jet),
                value = uiState.jet,
                onValueChange = onJetChange
            )

            // Overall score (calculated)
            if (uiState.taste > 0 && uiState.freshness > 0 && uiState.locationRating > 0 &&
                uiState.aesthetics > 0 && uiState.splash > 0 && uiState.jet > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.overall),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val overall = (uiState.taste + uiState.freshness + uiState.locationRating +
                                uiState.aesthetics + uiState.splash + uiState.jet) / 6.0
                        Text(
                            text = "%.1f / 5.0".format(overall),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Submit button
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.submit_review))
                }
            }
        }
    }
}

@Composable
fun RatingSlider(
    label: String,
    value: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (value > 0) "$value / 5" else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = 0f..5f,
            steps = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
