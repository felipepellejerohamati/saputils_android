package com.serviall.saputils.ui.entregar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.data.ItemStatus
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.globals.UserSession
import com.serviall.saputils.ui.recibir.BarcodeScanSimulationDialog
import com.serviall.saputils.ui.recibir.ProductLineRow
import com.serviall.saputils.ui.scanner.CameraScanDialog
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.utils.get64BaseString
import com.serviall.saputils.ui.viewmodel.ServialViewModel

@Suppress("DEPRECATION")
@Composable
fun EntregarScreen(
    items: List<ServialItem>,
    onRecordDelivery: (String, String, String, String?, String?) -> Unit,
    onUpdateProductQty: (String, String, Int, String, Int) -> Unit,
    onScanBarcode: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDelivery by remember { mutableStateOf<ServialItem?>(null) }
    var activeCameraScanItem by remember { mutableStateOf<ServialItem?>(null) }
    var activeSimulationScanItem by remember { mutableStateOf<ServialItem?>(null) }
    var showCameraSearchScan by remember { mutableStateOf(false) }
    var showSimulationSearchScan by remember { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val deliverableItems = items.filter { item ->
        val query = searchQuery.trim().lowercase()
        val isNotCompleted = item.status != ItemStatus.COMPLETED
        val matchesSearch = query.isEmpty() ||
                item.number.orEmpty().lowercase().contains(query) ||
                item.reference.orEmpty().lowercase().contains(query) ||
                item.client.orEmpty().lowercase().contains(query) ||
                item.notes.orEmpty().lowercase().contains(query) ||
                item.products.orEmpty().any { p ->
                    p.code.orEmpty().lowercase().contains(query) ||
                            p.name.orEmpty().lowercase().contains(query) ||
                            p.substitute.orEmpty().lowercase().contains(query)
                }
        isNotCompleted && matchesSearch
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Entregar Mercancía",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Despacho, verificación de productos y registro de entrega",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ServialRed.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalShipping,
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
                placeholder = { Text("Buscar por número, referencia o producto para entrega...") },
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

            if (deliverableItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay ítems pendientes de entrega",
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
                        items(deliverableItems, key = { it.id }) { item ->
                            DeliveryItemCard(
                                item = item,
                                onDeliverClick = { selectedItemForDelivery = item },
                                onUpdateProductQty = { code, linenum, substitute, qty -> onUpdateProductQty(item.id, code, linenum, substitute, qty) },
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
                        items(deliverableItems, key = { it.id }) { item ->
                            DeliveryItemCard(
                                item = item,
                                onDeliverClick = { selectedItemForDelivery = item },
                                onUpdateProductQty = { code, linenum, substitute, qty -> onUpdateProductQty(item.id, code, linenum, substitute, qty) },
                                onScanClick = { activeCameraScanItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedItemForDelivery != null) {
        RecordDeliveryDialog(
            item = selectedItemForDelivery!!,
            onDismiss = { selectedItemForDelivery = null },
            onConfirm = { recipient, notes, signature, deliveredBy ->
                onRecordDelivery(selectedItemForDelivery!!.id, recipient, notes, signature, deliveredBy)
                selectedItemForDelivery = null
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
fun DeliveryItemCard(
    item: ServialItem,
    onDeliverClick: () -> Unit,
    onUpdateProductQty: (String, Int, String, Int) -> Unit,
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
                text = "Ref: " + item.reference.ifBlank { " ${item.notes}" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Products section
            Text(
                text = "Productos a Despachar (${item.products.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (item.products.isEmpty()) {
                Text("Sin productos detallados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                item.products.forEach { prod ->
                    ProductLineRowEnt(
                        product = prod,
                        onQuantityChanged = { newQty -> onUpdateProductQty(prod.code, prod.linenum, prod.substitute, newQty) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado: ${item.status.label}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ServialRed
                )

                Button(
                    onClick = onDeliverClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar Entrega", fontSize = 12.sp)
                }
            }
        }
    }
}
@Composable
fun ProductLineRowEnt(
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
                if (product.substitute.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Código Cliente: ${product.substitute}",
                        fontSize = 11.sp,
                        color = ServialRed
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Solicitado: ${product.expectedQty} | Entregado: ${product.delivrdqty} | Inventario: ${product.actualQty}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChanged(product.qttytrans - 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${ product.qttytrans }",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onQuantityChanged( product.qttytrans + 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
@Composable
fun RecordDeliveryDialog(
    item: ServialItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var deliveryNotes by remember { mutableStateOf("") }
    var deliveredBy by remember { mutableStateOf(UserSession.username.ifBlank { "Operario" }) }
    var signatureBase64 by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Entrega #${item.number}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cliente: ${item.client}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = deliveredBy,
                    onValueChange = { deliveredBy = it },
                    label = { Text("Nombre de quien entrega (Operario / Conductor)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Nombre de quien recibe (Firma/Receptor)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deliveryNotes,
                    onValueChange = { deliveryNotes = it },
                    label = { Text("Notas de entrega / Novedades") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Firma del Receptor:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SignatureCanvas(
                    onSignatureChanged = { base64 ->
                        signatureBase64 = base64
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(recipientName, deliveryNotes, signatureBase64, deliveredBy) },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
            ) {
                Text("Confirmar Entrega")
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

@Composable
fun SignatureCanvas(
    onSignatureChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<List<androidx.compose.ui.geometry.Offset>>() }
    var currentPath by remember { mutableStateOf<MutableList<androidx.compose.ui.geometry.Offset>?>(null) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = mutableListOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.let { path ->
                                path.add(change.position)
                            }
                        },
                        onDragEnd = {
                            currentPath?.let { path ->
                                if (path.isNotEmpty()) {
                                    paths.add(path.toList())
                                    currentPath = null
                                    onSignatureChanged(generateSignatureBase64(paths))
                                }
                            }
                        },
                        onDragCancel = {
                            currentPath = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
                paths.forEach { pathPoints ->
                    if (pathPoints.size > 1) {
                        val composePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                lineTo(pathPoints[i].x, pathPoints[i].y)
                            }
                        }
                        drawPath(path = composePath, color = Color.Black, style = stroke)
                    }
                }
                currentPath?.let { pathPoints ->
                    if (pathPoints.size > 1) {
                        val composePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                lineTo(pathPoints[i].x, pathPoints[i].y)
                            }
                        }
                        drawPath(path = composePath, color = Color.Black, style = stroke)
                    }
                }
            }

            if (paths.isEmpty() && currentPath == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Firme aquí con el dedo o lápiz",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    onSignatureChanged(null)
                }
            ) {
                Text("Borrar Firma", fontSize = 12.sp, color = ServialRed)
            }
        }
    }
}

fun generateSignatureBase64(paths: List<List<androidx.compose.ui.geometry.Offset>>): String {
    val width = 600
    val height = 300
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    paths.forEach { points ->
        if (points.size > 1) {
            val path = android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
    val bytes = outputStream.toByteArray()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}
