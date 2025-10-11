package com.watxaut.fontsreviewer.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh profile when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    ProfileScreenContent(
        uiState = uiState,
        onLoginClick = onNavigateToLogin,
        onRegisterClick = onNavigateToRegister,
        onLogoutClick = viewModel::onLogoutClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is ProfileUiState.NotAuthenticated -> {
                    // Anonymous user - show login/register options
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "Welcome!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Sign in to rate fountains and view your statistics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onRegisterClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Account")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                is ProfileUiState.Success -> {
                    // Authenticated user - show profile info
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nickname: ${uiState.user.nickname}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Total Ratings: ${uiState.user.totalRatings}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Average Score: ${"%.1f".format(uiState.user.averageScore)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Logout")
                    }
                }
                is ProfileUiState.Error -> {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "Error loading profile",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
