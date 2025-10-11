package com.watxaut.fontsreviewer.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watxaut.fontsreviewer.R
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LeaderboardScreenContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreenContent(
    uiState: LeaderboardUiState,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.leaderboard)) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is LeaderboardUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is LeaderboardUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_leaderboard_data),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Be the first to rate fountains!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LeaderboardUiState.Error -> {
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

                is LeaderboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.rank),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.nickname),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        text = stringResource(R.string.fountains),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Text(
                                        text = stringResource(R.string.average),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Leaderboard entries
                        items(uiState.entries) { entry ->
                            LeaderboardEntryCard(
                                entry = entry,
                                isCurrentUser = entry.nickname == uiState.currentUserNickname
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank with trophy icon for top 3
            Box(
                modifier = Modifier.weight(0.8f),
                contentAlignment = Alignment.Center
            ) {
                when (entry.rank) {
                    1 -> {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "1st place",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    2 -> {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "2nd place",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    3 -> {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "3rd place",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = entry.rank.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Nickname
            Text(
                text = entry.nickname,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(2f)
            )

            // Total ratings
            Text(
                text = entry.totalRatings.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1.2f)
            )

            // Average score
            Text(
                text = "%.2f".format(entry.averageScore),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}
