package com.serviall.saputils.ui.recibir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
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
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.ui.scanner.CameraScanDialog
import com.serviall.saputils.ui.theme.ServialRed

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun RecibirScreen(
    items: List<ServialItem>,
    onAddNewItem: (String, String, String, String, Double) -> Unit,
    onUpdateStatus: (String, ItemStatus) -> Unit,
    onUpdateProductQty: (String, String, Int) -> Unit,
    onScanBarcode: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var activeCameraScanItem by remember { mutableStateOf<ServialItem?>(null) }
    var activeSimulationScanItem by remember { mutableStateOf<ServialItem?>(null) }
    var showCameraSearchScan by remember { mutableStateOf(false) }
    var showSimulationSearchScan by remember { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val filteredItems = items.filter { item ->
        val query = searchQuery.trim().lowercase()
        query.isEmpty() ||
                item.number.lowercase().contains(query) ||
                item.reference.lowercase().contains(query) ||
                item.client.lowercase().contains(query) ||
                item.notes.lowercase().contains(query) ||
                item.products.any { p -> p.code.lowercase().contains(query) || p.name.lowercase().contains(query) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ServialRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Registrar nuevo ítem"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recibir Mercancía",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Gestión de ingresos, productos y escaneo de códigos",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ServialRed.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = null,
                        tint = ServialRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por número, referencia, cliente o producto...") },
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

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron ítems para recibir",
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
                            ReceivingItemCard(
                                item = item,
                                onUpdateStatus = { newStatus -> onUpdateStatus(item.id, newStatus) },
                                onUpdateProductQty = { code, qty -> onUpdateProductQty(item.id, code, qty) },
                                onScanClick = { activeCameraScanItem = item }
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
                            ReceivingItemCard(
                                item = item,
                                onUpdateStatus = { newStatus -> onUpdateStatus(item.id, newStatus) },
                                onUpdateProductQty = { code, qty -> onUpdateProductQty(item.id, code, qty) },
                                onScanClick = { activeCameraScanItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReceivedItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { num, ref, client, notes, weight ->
                onAddNewItem(num, ref, client, notes, weight)
                showAddDialog = false
            }
        )
    }

    if (activeCameraScanItem != null) {
        CameraScanDialog(
            title = "Escanear - Guía #${activeCameraScanItem!!.number}",
            onDismiss = { activeCameraScanItem = null },
            onCodeScanned = { code ->
                val item = activeCameraScanItem
                activeCameraScanItem = null
                if (item != null) {
                    onScanBarcode(item.id, code)
                }
            },
            onFallbackToSimulation = {
                val item = activeCameraScanItem
                activeCameraScanItem = null
                activeSimulationScanItem = item
            }
        )
    }

    if (activeSimulationScanItem != null) {
        BarcodeScanSimulationDialog(
            itemNumber = activeSimulationScanItem!!.number,
            onDismiss = { activeSimulationScanItem = null },
            onScanCode = { code ->
                onScanBarcode(activeSimulationScanItem!!.id, code)
            }
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
fun ReceivingItemCard(
    item: ServialItem,
    onUpdateStatus: (ItemStatus) -> Unit,
    onUpdateProductQty: (String, Int) -> Unit,
    onScanClick: () -> Unit
) {
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
                    color = ServialRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "№ ${item.number}",
                        color = ServialRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Button(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed.copy(alpha = 0.9f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Escanear", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.client,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.notes.ifBlank { "Ref: ${item.reference}" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Products section
            Text(
                text = "Productos / Ítems (${item.products.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (item.products.isEmpty()) {
                Text("Sin productos detallados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                item.products.forEach { prod ->
                    ProductLineRow(
                        product = prod,
                        onQuantityChanged = { newQty -> onUpdateProductQty(prod.code, newQty) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peso: ${item.weightKg} kg",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                when (item.status) {
                    ItemStatus.RECEIVED -> {
                        OutlinedButton(
                            onClick = { onUpdateStatus(ItemStatus.DELIVERING) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("A Reparto", fontSize = 12.sp)
                        }
                    }
                    ItemStatus.DELIVERING -> {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("En Reparto", modifier = Modifier.padding(6.dp))
                        }
                    }
                    ItemStatus.COMPLETED -> {
                        Badge(containerColor = Color(0xFF10B981)) {
                            Text("Entregado", color = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun ProductLineRow(
    product: ProductItem,
    onQuantityChanged: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = product.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ServialRed)
                    Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = product.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Esperado: ${product.expectedQty} | Recibido: ${product.actualQty}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChanged(product.actualQty - 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${product.actualQty}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onQuantityChanged(product.actualQty + 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun BarcodeScanSimulationDialog(
    itemNumber: String,
    onDismiss: () -> Unit,
    onScanCode: (String) -> Unit
) {
    var manualCode by remember { mutableStateOf("") }
    val sampleBarcodes = listOf(
        "PRD-001" to "Servidor Blade Pro",
        "PRD-002" to "Módulo Memoria 32GB",
        "PRD-003" to "Licencia SAP S/4HANA",
        "PRD-004" to "Switch Industrial 24p",
        "PRD-005" to "Sensor IoT Temperatura",
        "PRD-006" to "Terminal Portátil Barcode"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null, tint = ServialRed)
                Text("Simulador de Escáner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (itemNumber == "Búsqueda") "Simule escaneo o seleccione producto para filtrar la búsqueda:" else "Simule escaneo o seleccione producto para la orden #$itemNumber:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Código SKU (ej. PRD-001)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (manualCode.isNotBlank()) {
                            onScanCode(manualCode.trim())
                            onDismiss()
                        }
                    },
                    enabled = manualCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear / Agregar Código")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "O seleccione de la lista de códigos de barra:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                sampleBarcodes.forEach { (code, name) ->
                    Surface(
                        onClick = {
                            onScanCode(code)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = code, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ServialRed)
                                Text(text = name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddReceivedItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("2.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nueva Recepción", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Número de Guía / Ítem (ej. 1006)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia (ej. REF-5900)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Cliente / Proveedor") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Observaciones") },
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 1.5
                    onConfirm(number, reference, client, notes, w)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
