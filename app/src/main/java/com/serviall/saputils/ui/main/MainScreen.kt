package com.serviall.saputils.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.serviall.saputils.R
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.globals.UserSession
import com.serviall.saputils.ui.consultar.ConsultarScreen
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Storefront
import com.serviall.saputils.ui.atencionmeson.AtencionMesonScreen
import com.serviall.saputils.ui.entregar.EntregarScreen
import com.serviall.saputils.ui.reconocer.ReconocerScreen
import com.serviall.saputils.ui.reservas.ReservasScreen
import com.serviall.saputils.ui.recibir.RecibirScreen
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.ui.viewmodel.ServialViewModel
import com.serviall.saputils.utils.get64BaseString
import kotlinx.coroutines.launch

enum class ServialTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ATENCION_MESON("Atención de Mesón", Icons.Rounded.Storefront)
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
    var selectedTab by rememberSaveable { mutableStateOf(ServialTab.ATENCION_MESON) }
    var isMenuCollapsed by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val activeWarehouse by viewModel.activeWarehouse.collectAsState()
    var showWarehouseDialog by remember { mutableStateOf(false) }



    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    LaunchedEffect(isLargeScreen, isPortrait) {
        if (isLargeScreen && isPortrait) {
            isMenuCollapsed = true
        }
    }

    val drawerContent: @Composable () -> Unit = {
        ModalDrawerSheet(
            modifier = Modifier.width(if (isLargeScreen && isMenuCollapsed) 80.dp else 300.dp),
            drawerContainerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(if (isLargeScreen && isMenuCollapsed) 8.dp else 16.dp),
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
                        horizontalArrangement = if (isLargeScreen && isMenuCollapsed) Arrangement.Center else Arrangement.SpaceBetween
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            //color = ServialRed
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
                        if (!isLargeScreen || !isMenuCollapsed) {
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
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
                    }

                    if (!isLargeScreen || !isMenuCollapsed) {
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
                    } else {
                        IconButton(
                            onClick = { showWarehouseDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Store, contentDescription = "Bodega: $activeWarehouse", tint = ServialRed)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Navigation Items
                    ServialTab.entries.forEach { tab ->
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = if (isLargeScreen && isMenuCollapsed) { { } } else { { Text(tab.title, fontWeight = FontWeight.Medium) } },
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
                        label = if (isLargeScreen && isMenuCollapsed) { { } } else { { Text("Salir / Logout", fontWeight = FontWeight.Medium, color = ServialRed) } },
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
                        } else {
                            IconButton(onClick = {
                                isMenuCollapsed = !isMenuCollapsed
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Colapsar/Expandir menú"
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
                    ServialTab.ATENCION_MESON -> AtencionMesonScreen(
                        activeWarehouse = activeWarehouse
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
                UserSession.wharehouse = warehouse
                showWarehouseDialog = false
            },
            onDismiss = { showWarehouseDialog = false }
        )
    }
}
