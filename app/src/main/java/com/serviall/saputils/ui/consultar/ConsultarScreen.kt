package com.serviall.saputils.ui.consultar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import com.serviall.saputils.ui.recibir.BarcodeScanSimulationDialog
import com.serviall.saputils.ui.scanner.CameraScanDialog
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.serviall.saputils.data.ItemStatus
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.ui.theme.ServialRed

@Suppress("DEPRECATION")
@Composable
fun ConsultarScreen(
    items: List<ServialItem>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ItemStatus?>(null) }
    var selectedItemDetail by remember { mutableStateOf<ServialItem?>(null) }
    var showCameraSearchScan by remember { mutableStateOf(false) }
    var showSimulationSearchScan by remember { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val filteredItems = items.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.number.orEmpty().contains(searchQuery, ignoreCase = true) ||
                item.reference.orEmpty().contains(searchQuery, ignoreCase = true) ||
                item.client.orEmpty().contains(searchQuery, ignoreCase = true) ||
                item.notes.orEmpty().contains(searchQuery, ignoreCase = true) ||
                item.products.orEmpty().any { p -> p.code.orEmpty().contains(searchQuery, ignoreCase = true) || p.name.orEmpty().contains(searchQuery, ignoreCase = true) }

        val matchesStatus = selectedStatusFilter == null || item.status == selectedStatusFilter

        matchesSearch && matchesStatus
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consultar Órdenes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Consulta universal por número, referencia, cliente o producto",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ServialRed.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = ServialRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Universal Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar número, referencia, cliente, producto...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search Icon",
                        tint = ServialRed
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showCameraSearchScan = true }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Escanear código para buscar",
                            tint = ServialRed
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ServialRed,
                    cursorColor = ServialRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("Todos (${items.size})") }
                    )
                }
                items(ItemStatus.entries) { status ->
                    val count = items.count { it.status == status }
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text("${status.label} ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items List / Grid
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron resultados de consulta",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                if (isLargeScreen) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ConsultItemCard(
                                item = item,
                                onDetailClick = { selectedItemDetail = item }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ConsultItemCard(
                                item = item,
                                onDetailClick = { selectedItemDetail = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedItemDetail != null) {
        ItemDetailDialog(
            item = selectedItemDetail!!,
            onDismiss = { selectedItemDetail = null }
        )
    }

    if (showCameraSearchScan) {
        CameraScanDialog(
            title = "Escanear Código para Búsqueda",
            onDismiss = { showCameraSearchScan = false },
            onCodeScanned = { code ->
                showCameraSearchScan = false
                searchQuery = code
            },
            onFallbackToSimulation = {
                showCameraSearchScan = false
                showSimulationSearchScan = true
            }
        )
    }

    if (showSimulationSearchScan) {
        BarcodeScanSimulationDialog(
            itemNumber = "Búsqueda",
            onDismiss = { showSimulationSearchScan = false },
            onScanCode = { code ->
                searchQuery = code
            }
        )
    }
}

@Composable
fun ConsultItemCard(
    item: ServialItem,
    onDetailClick: () -> Unit
) {
    val statusColor = when (item.status) {
        ItemStatus.RECEIVED -> ServialRed
        ItemStatus.DELIVERING -> Color(0xFFD97706)
        ItemStatus.COMPLETED -> Color(0xFF10B981)
        ItemStatus.PENDING -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = item.status.label,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = item.reference,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${item.number} • ${item.client}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.products.size} productos en orden • ${item.notes.ifBlank { "Sin observaciones" }}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingreso: ${item.date}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                TextButton(
                    onClick = onDetailClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ServialRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detalles", color = ServialRed, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ItemDetailDialog(
    item: ServialItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle de Ítem #${item.number}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Referencia:", item.reference)
                DetailRow("Cliente:", item.client)
                DetailRow("Estado Actual:", item.status.label)
                DetailRow("Fecha Ingreso:", item.date)
                DetailRow("Peso:", "${item.weightKg} kg")
                DetailRow("Notas:", item.notes)
                if (item.recipientName != null) {
                    DetailRow("Receptor:", item.recipientName)
                }
                if (item.deliveryTimestamp != null) {
                    DetailRow("Hora de Entrega:", item.deliveryTimestamp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Productos en la Orden:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ServialRed)
                if (item.products.isEmpty()) {
                    Text("Sin productos detallados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    item.products.forEach { prod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "${prod.code} - ${prod.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(
                                    text = "Esp: ${prod.expectedQty} | Act: ${prod.actualQty}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ServialRed
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
            ) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
