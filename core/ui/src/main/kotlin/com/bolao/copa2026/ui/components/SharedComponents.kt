package com.bolao.copa2026.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ErrorSnackbar(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onDismiss()
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun StaleDataWarning(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Dados podem estar desatualizados",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOffline = false }
            override fun onLost(network: Network) { isOffline = true }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        isOffline = cm.activeNetwork == null
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    if (isOffline) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Text(
                "Sem conexão — modo offline",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
