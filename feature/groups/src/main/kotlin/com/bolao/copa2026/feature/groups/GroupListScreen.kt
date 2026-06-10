package com.bolao.copa2026.feature.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bolao.copa2026.domain.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val showPaywall by viewModel.paywallVisible.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Sair da conta") },
            text = { Text("Tem certeza que deseja sair?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Sair", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPaywall) {
        PaywallDialog(onDismiss = { viewModel.dismissPaywall() })
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Atualização disponível") },
            text = { Text(info.releaseNotes) },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                    viewModel.dismissUpdate()
                }) {
                    Text("Baixar agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) { Text("Agora não") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Grupos") },
                actions = {
                    if (state.isVip) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Meu perfil")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair da conta")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onJoinGroupClicked(onNavigateToJoin) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Entrar com código")
                    }
                    Button(
                        onClick = { viewModel.onCreateGroupClicked(onNavigateToCreate) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Criar grupo")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                state.groups.isEmpty() -> EmptyGroupsHint(modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.groups) { group ->
                        GroupItem(group = group, onClick = { onNavigateToGroup(group.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
        title = { Text("Limite de grupos atingido") },
        text = {
            Text(
                "No plano gratuito você pode participar de apenas 1 grupo.\n\n" +
                "Torne-se VIP por R\$10,00 (pagamento único) e participe de grupos ilimitados!"
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Em breve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun EmptyGroupsHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            "Você ainda não participa de nenhum grupo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Crie ou entre em um grupo usando os botões abaixo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun GroupItem(group: Group, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                "${group.memberCount} membro${if (group.memberCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    group.inviteCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
