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
    
    // Handle account deletion success
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.AccountDeleted) {
            onLogout() // Navigate away after account deletion
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        onLoginClick = onNavigateToLogin,
        onRegisterClick = onNavigateToRegister,
        onLogoutClick = viewModel::onLogoutClick,
        onDeleteAccountClick = viewModel::onDeleteAccountClick,
        onDismissDeleteError = viewModel::onDismissDeleteError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDismissDeleteError: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account") },
            text = { 
                Text(
                    "Are you sure you want to delete your account?\n\n" +
                    "This action cannot be undone. All your reviews and data will be permanently deleted."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccountClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete error dialog
    if (uiState is ProfileUiState.DeleteAccountError) {
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title = { Text("Error") },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                Button(onClick = onDismissDeleteError) {
                    Text("OK")
                }
            }
        )
    }
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Account")
                    }
                }
                is ProfileUiState.DeletingAccount -> {
                    // Show loading while deleting
                    Spacer(modifier = Modifier.weight(1f))
                    
                    CircularProgressIndicator()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Deleting account...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                is ProfileUiState.AccountDeleted -> {
                    // Show success message briefly before navigation
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "Account deleted successfully",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                is ProfileUiState.DeleteAccountError -> {
                    // Show profile with error dialog (handled above)
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Account")
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
