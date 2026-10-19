package com.serviall.saputils.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.serviall.saputils.R
import com.serviall.saputils.ui.consultar.ConsultarScreen
import com.serviall.saputils.ui.entregar.EntregarScreen
import com.serviall.saputils.ui.recibir.RecibirScreen
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.ui.viewmodel.ServialViewModel
import kotlinx.coroutines.launch

enum class ServialTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    RECIBIR("Recibir", Icons.Rounded.Inbox),
    ENTREGAR("Entregar", Icons.Rounded.LocalShipping),
    CONSULTAR("Consultar", Icons.Rounded.Search)
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun MainScreen(
    username: String,
    viewModel: ServialViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ServialTab.RECIBIR) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val items by viewModel.items.collectAsState()
    val activeWarehouse by viewModel.activeWarehouse.collectAsState()
    var showWarehouseDialog by remember { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val drawerContent: @Composable () -> Unit = {
        ModalDrawerSheet(
            modifier = Modifier.width(300.dp),
            drawerContainerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // App Branding Header in Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ServialRed
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.serviall_isotipo_rojo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "SERVIALL",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Usuario: $username",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Warehouse Card in Drawer
                    Surface(
                        onClick = { showWarehouseDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ServialRed.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Store, contentDescription = null, tint = ServialRed, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Bodega Activa", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = activeWarehouse, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ServialRed)
                            }
                            Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ServialRed)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Navigation Items
                    ServialTab.entries.forEach { tab ->
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontWeight = FontWeight.Medium) },
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                if (!isLargeScreen) {
                                    coroutineScope.launch { drawerState.close() }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = ServialRed.copy(alpha = 0.12f),
                                selectedTextColor = ServialRed,
                                selectedIconColor = ServialRed
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Logout Option
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = "Salir") },
                        label = { Text("Salir / Logout", fontWeight = FontWeight.Medium, color = ServialRed) },
                        selected = false,
                        onClick = { onLogout() },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = ServialRed,
                            unselectedIconColor = ServialRed
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    val mainContent: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = selectedTab.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        if (!isLargeScreen) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        }
                    },
                    actions = {
                        // Warehouse Badge / Clickable Chip
                        Surface(
                            onClick = { showWarehouseDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = ServialRed.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Store,
                                    contentDescription = null,
                                    tint = ServialRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = activeWarehouse,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ServialRed
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = username,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    ServialTab.RECIBIR -> RecibirScreen(
                        items = items,
                        onAddNewItem = { num, ref, client, notes, w ->
                            viewModel.addNewReceivedItem(num, ref, client, notes, w)
                        },
                        onUpdateStatus = { id, status ->
                            viewModel.updateItemStatus(id, status)
                        },
                        onUpdateProductQty = { id, code, qty ->
                            viewModel.updateProductQuantity(id, code, qty)
                        },
                        onScanBarcode = { id, code ->
                            viewModel.scanBarcodeAndIncrement(id, code)
                        }
                    )
                    ServialTab.ENTREGAR -> EntregarScreen(
                        items = items,
                        onRecordDelivery = { id, recipient, notes ->
                            viewModel.recordDelivery(id, recipient, notes)
                        },
                        onUpdateProductQty = { id, code, qty ->
                            viewModel.updateProductQuantity(id, code, qty)
                        },
                        onScanBarcode = { id, code ->
                            viewModel.scanBarcodeAndIncrement(id, code)
                        }
                    )
                    ServialTab.CONSULTAR -> ConsultarScreen(
                        items = items
                    )
                }
            }
        }
    }

    if (isLargeScreen) {
        PermanentNavigationDrawer(
            drawerContent = drawerContent,
            modifier = modifier.fillMaxSize()
        ) {
            mainContent()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent,
            modifier = modifier.fillMaxSize()
        ) {
            mainContent()
        }
    }

    if (showWarehouseDialog) {
        WarehouseSelectionDialog(
            currentWarehouse = activeWarehouse,
            onWarehouseSelected = { warehouse ->
                viewModel.setWarehouse(warehouse)
                showWarehouseDialog = false
            },
            onDismiss = { showWarehouseDialog = false }
        )
    }
}
