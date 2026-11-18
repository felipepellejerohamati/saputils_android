package com.serviall.saputils.ui.atencionmeson

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serviall.saputils.apicalls.ServicioAPI
import java.text.SimpleDateFormat
import java.util.Date
import com.serviall.saputils.data.ItemStatus
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.ui.scanner.CameraScanDialog
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.utils.get64BaseString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

@Composable
fun AtencionMesonScreen(
    activeWarehouse: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var openDocuments by remember { mutableStateOf<List<ServialItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var editingReservation by remember { mutableStateOf<ServialItem?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ItemStatus?>(null) }
    var selectedItemForAction by remember { mutableStateOf<ServialItem?>(null) }
    var selectedItemForDelivery by remember { mutableStateOf<ServialItem?>(null) }
    var selectedItemForViewOnly by remember { mutableStateOf<ServialItem?>(null) }

    val loadOpenDocuments = {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val servicioAPI = ServicioAPI.getInstance()
                val base64String = "Basic " + get64BaseString("serviall:8400Abn.,")
                val list = servicioAPI.OpenDocuments(base64String, activeWarehouse).map { item ->
                    item.copy(status = item.effectiveStatus)
                }
                openDocuments = list
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error al cargar OpenDocuments"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeWarehouse) {
        loadOpenDocuments()
    }

    val photoFile = remember {
        File.createTempFile("sap_meson_add_res_", ".jpg", context.cacheDir)
    }
    val photoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            isTakingPhoto = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    val byteArray = stream.toByteArray()
                    val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

                    val api = ServicioAPI.getInstance()
                    val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")

                    val item = api.dataissuesobj(base64Auth, activeWarehouse, requestBody)
                    withContext(Dispatchers.Main) {
                        isTakingPhoto = false
                        if (item.reference.isBlank() && item.number.isBlank()) {
                            Toast.makeText(context, "No se detecto reserva en imagen", Toast.LENGTH_LONG).show()
                        } else {
                            editingReservation = item
                            Toast.makeText(context, "¡Reserva detectada con éxito!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        isTakingPhoto = false
                        // Fallback de prueba si hay error de red
                        editingReservation = ServialItem(
                            number = searchQuery.ifBlank { "5181234" },
                            client = "Cliente General",
                            products = listOf(
                                ProductItem(code = "1226728", name = "Material 1", expectedQty = 6),
                                ProductItem(code = "SKU cliente", name = "Material 2", expectedQty = 7),
                                ProductItem(code = "SKU cliente", name = "Material 3", expectedQty = 2)
                            )
                        )
                        Toast.makeText(context, "No se detecto reserva en imagen (Mostrando borrador)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            isTakingPhoto = false
            Toast.makeText(context, "No se capturó la foto", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerAddReservationCamera = {
        isTakingPhoto = true
        try {
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            isTakingPhoto = false
            Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val filteredItems = remember(openDocuments, searchQuery, selectedStatusFilter) {
        openDocuments.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.reference.contains(searchQuery, ignoreCase = true) ||
                    item.number.contains(searchQuery, ignoreCase = true) ||
                    item.id.contains(searchQuery, ignoreCase = true) ||
                    item.client.contains(searchQuery, ignoreCase = true) ||
                    item.products.any { p -> p.code.contains(searchQuery, ignoreCase = true) || p.substitute.contains(searchQuery, ignoreCase = true) }

            val statusToMatch = item.effectiveStatus
            val matchesStatus = selectedStatusFilter == null || statusToMatch == selectedStatusFilter

            matchesSearch && matchesStatus
        }
    }

    if (editingReservation != null) {
        CargarReservaEditScreen(
            item = editingReservation!!,
            activeWarehouse = activeWarehouse,
            onDismiss = { editingReservation = null },
            onSave = { savedItem ->
                openDocuments = listOf(savedItem) + openDocuments
                editingReservation = null
                loadOpenDocuments()
            }
        )
        return
    }

    if (selectedItemForViewOnly != null) {
        SoloVerPedidoScreen(
            item = selectedItemForViewOnly!!,
            onDismiss = { selectedItemForViewOnly = null },
            onComenzarAtencionClick = {
                selectedItemForAction = selectedItemForViewOnly
                selectedItemForViewOnly = null
            }
        )
        return
    }

    if (selectedItemForDelivery != null) {
        AtencionMesonDetailScreen(
            item = selectedItemForDelivery!!,
            activeWarehouse = activeWarehouse,
            onDismiss = { selectedItemForDelivery = null },
            onDeliverConfirmed = { updatedItem, _, _ ->
                openDocuments = openDocuments.map { if (it.id == updatedItem.id || (it.reference.isNotBlank() && it.reference == updatedItem.reference)) updatedItem else it }
                selectedItemForDelivery = null
                Toast.makeText(context, "Entrega registrada para ${updatedItem.reference.ifBlank { updatedItem.number }}", Toast.LENGTH_LONG).show()
                loadOpenDocuments()
            },
            onItemUpdated = { updatedItem ->
                openDocuments = openDocuments.map { if (it.id == updatedItem.id || (it.reference.isNotBlank() && it.reference == updatedItem.reference)) updatedItem else it }
                selectedItemForDelivery = updatedItem
                loadOpenDocuments()
            }
        )
        return
    }

    if (selectedItemForAction != null) {
        OpcionesAtencionDialog(
            item = selectedItemForAction!!,
            onDismiss = { selectedItemForAction = null },
            onComenzarAtencion = { updatedItem ->
                selectedItemForAction = null
                selectedItemForDelivery = updatedItem
            },
            onSoloVerPedido = {
                selectedItemForViewOnly = selectedItemForAction
                selectedItemForAction = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Atención de Mesón",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bodega: $activeWarehouse",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isTakingPhoto) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ServialRed)
            Text(text = "Tomando foto y enviando a dataissuesobj...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Filters and Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filtrar por N° reserva, SKU, código, ID...") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )



        // Status Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                label = { Text("Todos Estados", fontSize = 11.sp) }
            )
            listOf(ItemStatus.PARCIAL, ItemStatus.PENDIENTE, ItemStatus.ENTREGADO).forEach { status ->
                FilterChip(
                    selected = selectedStatusFilter == status,
                    onClick = { selectedStatusFilter = status },
                    label = { Text(status.label, fontSize = 11.sp) }
                )
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadOpenDocuments() },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (isLoading && openDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ServialRed)
                }
            } else if (errorMessage != null && openDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { loadOpenDocuments() }, colors = ButtonDefaults.buttonColors(containerColor = ServialRed)) {
                            Text("Reintentar")
                        }
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "No estaba precargada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ServialRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "¿Cargar la reserva ${searchQuery.ifBlank { "666666" }}?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Se crea como reserva (borrador en SAP) con lo leído en la foto. Revisas, agregas o corriges las líneas con códigos del cliente.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { triggerAddReservationCamera() },
                                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Cargar reserva", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isOV = item.tipo.contains("OV", ignoreCase = true) || item.tipo.contains("Orden", ignoreCase = true) || item.tipo.contains("Venta", ignoreCase = true)
                                    val docPrefix = if (isOV) "OV" else "Reserva"
                                    Text(
                                        text = "$docPrefix ${item.reference.ifBlank { item.number }}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ServialRed
                                    )
                                    val statusColor = when (item.status) {
                                        ItemStatus.PENDIENTE, ItemStatus.PENDING -> Color(0xFFEF4444) // Rojo
                                        ItemStatus.PARCIAL -> Color(0xFF3B82F6) // Azul
                                        ItemStatus.ENTREGADO, ItemStatus.COMPLETED -> Color(0xFF10B981) // Verde
                                        else -> ServialRed
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${item.tipo} • ${(item.status ?: ItemStatus.PENDING).label}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }

                                if (item.statustext.isNotBlank()) {
                                    Text(
                                        text = item.statustext,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Progress bar for completion percentage
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Avance de completado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = String.format(Locale.getDefault(), "%.2f%%", item.completado), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (item.completado / 100.0).toFloat() },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = ServialRed,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                // Button: Entrega
                                Button(
                                    onClick = { selectedItemForAction = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Entregar ahora", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ProductGroup(
    val codeKey: String,
    val name: String,
    val expectedQty: Int,
    val options: List<ProductItem>
)

@Composable
fun CargarReservaEditScreen(
    item: ServialItem,
    activeWarehouse: String,
    onDismiss: () -> Unit,
    onSave: (ServialItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    var isSaving by remember { mutableStateOf(false) }

    // Preloaded requested quantities per group
    val groupTargetQuantities = remember(item.products) {
        item.products.groupBy { it.code.ifBlank { it.substitute } }.mapValues { (_, options) ->
            options.maxOfOrNull { it.expectedQty } ?: 1
        }
    }

    // Initialize all substitute alternatives with selected quantity = 0
    val initialProductsWithZeroQty = remember(item) {
        item.products.map { p -> p.copy(expectedQty = 0) }
    }

    var editableItem by remember(item) { mutableStateOf(item.copy(products = initialProductsWithZeroQty)) }
    var pickupUser by remember { mutableStateOf("") }
    var newSkuInput by remember { mutableStateOf("") }

    fun itemKey(p: ProductItem): String = "${p.code.trim()}_${p.substitute.trim()}_${p.linenum}"

    val groupedEditProducts = remember(editableItem.products) {
        editableItem.products.groupBy { it.code.ifBlank { it.substitute } }.map { (cKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().substitute.ifBlank { cKey }
            val targetQty = groupTargetQuantities[cKey] ?: (options.maxOfOrNull { it.expectedQty } ?: 1)
            ProductGroup(
                codeKey = cKey,
                name = groupName,
                expectedQty = targetQty,
                options = options
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cargar reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = onDismiss) {
                    Text("Volver", color = ServialRed)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Se guarda como", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ServialRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⊙ Reserva - borrador SAP",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ServialRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "N° reserva del cliente", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editableItem.reference.ifBlank { editableItem.number },
                        onValueChange = { newRef -> editableItem = editableItem.copy(reference = newRef, number = newRef) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Usuario que viene a retirar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pickupUser,
                        onValueChange = { pickupUser = it },
                        placeholder = { Text("Nombre o credencial", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = if (isDarkTheme) Color.White else Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = ServialRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contrato [Cliente - Faena] • Bodega $activeWarehouse",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CÓDIGOS PADRE Y ALTERNATIVAS (${groupedEditProducts.size} grupos)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(groupedEditProducts, key = { it.codeKey }) { group ->
                    val substituteOptions = group.options.filter { it.substitute.isNotBlank() }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Parent Code Header Row with Requested Quantity & Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SKU Cliente (Línea: ${group.options.firstOrNull()?.linenum ?: "-"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Cantidad solicitada: ${group.expectedQty}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = group.codeKey,
                                onValueChange = { newCode ->
                                    val targetKeys = group.options.map { itemKey(it) }.toSet()
                                    val updatedProducts = editableItem.products.map { p ->
                                        if (itemKey(p) in targetKeys) {
                                            p.copy(code = newCode)
                                        } else {
                                            p
                                        }
                                    }
                                    editableItem = editableItem.copy(products = updatedProducts)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (group.name.isNotBlank()) {
                                Text(
                                    text = group.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (substituteOptions.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                val groupSum = group.options.sumOf { it.expectedQty }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Alternativas disponibles Serviall y stock:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Asignado $groupSum de ${group.expectedQty}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (groupSum == group.expectedQty) Color(0xFF16A34A) else ServialRed
                                    )
                                }

                                // Render each substitute option in group
                                substituteOptions.forEach { prod ->
                                    val prodKey = itemKey(prod)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Substitute SKU & Stock display
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.substitute.ifBlank { prod.code },
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (prod.name.isNotBlank()) {
                                                        Text(
                                                            text = prod.name,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // Display Stock
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (prod.actualQty > 0) {
                                                        if (isDarkTheme) Color(0xFF143820) else Color(0xFFE8F5E9)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFF3B1212) else Color(0xFFFFEBEE)
                                                    },
                                                    border = BorderStroke(1.dp, if (prod.actualQty > 0) {
                                                        if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF81C784)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFFEF4444) else Color(0xFFEF9A9A)
                                                    })
                                                ) {
                                                    Text(
                                                        text = "Stock: ${prod.actualQty} unid.",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (prod.actualQty > 0) {
                                                            if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF2E7D32)
                                                        } else {
                                                            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFC62828)
                                                        }
                                                    )
                                                }
                                            }

                                            // Quantity Allocation (- qty +) and Delete option
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text("Cantidad:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    IconButton(
                                                        onClick = {
                                                            if (prod.expectedQty > 0) {
                                                                val updatedProducts = editableItem.products.map { p ->
                                                                    if (itemKey(p) == prodKey) p.copy(expectedQty = p.expectedQty - 1) else p
                                                                }
                                                                editableItem = editableItem.copy(products = updatedProducts)
                                                            }
                                                        },
                                                        enabled = prod.expectedQty > 0,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (prod.expectedQty > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (prod.expectedQty > 0) ServialRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(1.dp, if (prod.expectedQty > 0) ServialRed else MaterialTheme.colorScheme.outlineVariant),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(text = "${prod.expectedQty}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (prod.expectedQty > 0) ServialRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val currentSum = group.options.sumOf { it.expectedQty }
                                                            if (currentSum < group.expectedQty) {
                                                                val updatedProducts = editableItem.products.map { p ->
                                                                    if (itemKey(p) == prodKey) p.copy(expectedQty = p.expectedQty + 1) else p
                                                                }
                                                                editableItem = editableItem.copy(products = updatedProducts)
                                                            } else {
                                                                Toast.makeText(context, "La suma de alternativas ($currentSum) no puede superar la cantidad pedida (${group.expectedQty})", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                }

                                                TextButton(
                                                    onClick = {
                                                        val updatedProducts = editableItem.products.filter { itemKey(it) != prodKey }
                                                        editableItem = editableItem.copy(products = updatedProducts)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Quitar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSkuInput,
                            onValueChange = { newSkuInput = it },
                            placeholder = { Text("+ Escanea o escribe SKU cliente (substitute)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newSkuInput.isNotBlank()) {
                                    val newProd = ProductItem(code = newSkuInput, substitute = newSkuInput, name = "Nuevo Producto", expectedQty = 1)
                                    editableItem = editableItem.copy(products = editableItem.products + newProd)
                                    newSkuInput = ""
                                    Toast.makeText(context, "Línea agregada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Agregar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Los productos están agrupados por código padre Serviall (code). Puedes cambiar el código padre y repartir las cantidades pedidas entre las alternativas del cliente (substitute) revisando el stock disponible (actualQty) de cada una.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        val itemToSave = if (pickupUser.isNotBlank()) {
                            editableItem.copy(recipientName = pickupUser)
                        } else {
                            editableItem
                        }
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val api = ServicioAPI.getInstance()
                                val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")
                                val response = api.postReserva(base64Auth, activeWarehouse, itemToSave)
                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                    if (response.isNotBlank()) {
                                        Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "¡Reserva guardada con éxito!", Toast.LENGTH_LONG).show()
                                    }
                                    onSave(itemToSave)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                    Toast.makeText(context, "Error al guardar reserva: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun CorregirCantidadPedidaDialog(
    group: ProductGroup,
    item: ServialItem,
    activeWarehouse: String,
    onDismiss: () -> Unit,
    onSaveSuccess: (updatedProducts: List<ProductItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val deliveredQty = group.options.maxOfOrNull { it.delivrdqty } ?: 0
    val minQty = maxOf(1, deliveredQty)
    val initialQty = group.expectedQty
    var currentNewQty by remember { mutableIntStateOf(maxOf(minQty, initialQty)) }
    var isSaving by remember { mutableStateOf(false) }
    var changeResponseMessage by remember { mutableStateOf<String?>(null) }

    if (changeResponseMessage != null) {
        AlertDialog(
            onDismissRequest = { changeResponseMessage = null },
            title = { Text("Respuesta de cambio", fontWeight = FontWeight.Bold) },
            text = { Text(changeResponseMessage!!) },
            confirmButton = {
                Button(
                    onClick = { changeResponseMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top handle indicator
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.width(36.dp).height(4.dp)
                    ) {}
                }

                Text(
                    text = "Corregir cantidad pedida",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SKU cliente ",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = group.options.firstOrNull()?.substitute?.ifBlank { group.codeKey } ?: group.codeKey,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " · ${group.name}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Precargada: $initialQty unidades · Entregadas: $deliveredQty",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity Box (matching screenshot)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pide",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { if (currentNewQty > minQty) currentNewQty-- },
                                    enabled = currentNewQty > minQty && !isSaving
                                ) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (currentNewQty > minQty && !isSaving) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }

                            Text(
                                text = "$currentNewQty",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { currentNewQty++ },
                                    enabled = !isSaving
                                ) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (!isSaving) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }

                // Info banner box (matching screenshot)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "La línea queda marcada \"editada en mesón\" para revisión de planificación.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (!isSaving) {
                                if (currentNewQty < minQty) {
                                    Toast.makeText(context, "La cantidad pedida no puede ser menor a $minQty", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                isSaving = true
                                val targetKeys = group.options.map { "${it.code.trim()}_${it.substitute.trim()}_${it.linenum}" }.toSet()
                                val updatedProducts = item.products.map { p ->
                                    val key = "${p.code.trim()}_${p.substitute.trim()}_${p.linenum}"
                                    if (key in targetKeys) {
                                        p.copy(expectedQty = currentNewQty)
                                    } else {
                                        p
                                    }
                                }
                                val updatedItem = item.copy(products = updatedProducts)

                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val api = ServicioAPI.getInstance()
                                        val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")
                                        val response = api.postChange(base64Auth, activeWarehouse, updatedItem)
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            if (response.isBlank()) {
                                                Toast.makeText(context, "¡Cantidad pedida actualizada con éxito!", Toast.LENGTH_SHORT).show()
                                                onSaveSuccess(updatedProducts)
                                            } else {
                                                changeResponseMessage = response
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            changeResponseMessage = "Error al procesar cambio: ${e.message}"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtencionMesonDetailScreen(
    item: ServialItem,
    activeWarehouse: String,
    onDismiss: () -> Unit,
    onDeliverConfirmed: (ServialItem, String, String) -> Unit,
    onItemUpdated: ((ServialItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDelivering by remember { mutableStateOf(false) }
    var deliveryResponseMessage by remember { mutableStateOf<String?>(null) }
    var scanCodeInput by remember { mutableStateOf("") }
    var showCameraScan by remember { mutableStateOf(false) }
    var currentProducts by remember(item) { mutableStateOf(item.products) }
    var groupToEditQty by remember { mutableStateOf<ProductGroup?>(null) }
    var showConfirmarEntregaScreen by remember { mutableStateOf(false) }

    fun itemKey(p: ProductItem): String = "${p.code.trim()}_${p.substitute.trim()}_${p.linenum}"

    val productQuantities = remember {
        mutableStateMapOf<String, Int>().apply {
            currentProducts.forEach { p ->
                put(itemKey(p), 0)
            }
        }
    }

    if (showConfirmarEntregaScreen) {
        ConfirmarEntregaScreen(
            item = item.copy(products = currentProducts),
            activeWarehouse = activeWarehouse,
            productQuantities = productQuantities,
            onDismiss = { showConfirmarEntregaScreen = false },
            onDeliverConfirmed = onDeliverConfirmed
        )
        return
    }

    val groupedProducts = remember(currentProducts) {
        currentProducts.groupBy { it.substitute.ifBlank { it.code } }.map { (subKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().code
            val maxExpected = options.maxOfOrNull { it.expectedQty } ?: 1
            ProductGroup(
                codeKey = subKey,
                name = groupName,
                expectedQty = maxExpected,
                options = options
            )
        }
    }

    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    if (showCameraScan) {
        CameraScanDialog(
            title = "Escanear QR o Código de Barras",
            onDismiss = { showCameraScan = false },
            onCodeScanned = { scannedCode ->
                showCameraScan = false
                val match = item.products.firstOrNull { it.code.equals(scannedCode, true) || it.substitute.equals(scannedCode, true) }
                if (match != null) {
                    val matchKey = itemKey(match)
                    val groupKey = match.code.ifBlank { match.substitute }
                    val group = groupedProducts.firstOrNull { it.codeKey == groupKey }
                    val totalSelected = group?.options?.sumOf { productQuantities[itemKey(it)] ?: 0 } ?: 0
                    val groupMax = group?.expectedQty ?: match.expectedQty
                    val groupDelivered = group?.options?.sumOf { it.delivrdqty } ?: 0
                    val groupNeeded = (groupMax - groupDelivered).coerceAtLeast(0)
                    val prodNeeded = (match.expectedQty - match.delivrdqty).coerceAtLeast(0)
                    val matchMax = if (match.actualQty > 0) minOf(prodNeeded, match.actualQty) else prodNeeded
                    val current = productQuantities[matchKey] ?: 0

                    if (totalSelected < groupNeeded && current < matchMax) {
                        productQuantities[matchKey] = current + 1
                        Toast.makeText(context, "Escaneado: ${match.substitute.ifBlank { match.code }} (+1)", Toast.LENGTH_SHORT).show()
                    } else if (current >= matchMax) {
                        Toast.makeText(context, "Stock / Máximo alcanzado para ${match.substitute.ifBlank { match.code }} ($matchMax)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Línea completa para esta entrega ($totalSelected de $groupNeeded)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Código no encontrado: $scannedCode", Toast.LENGTH_SHORT).show()
                }
            },
            onFallbackToSimulation = {
                showCameraScan = false
            }
        )
    }

    if (deliveryResponseMessage != null) {
        AlertDialog(
            onDismissRequest = { deliveryResponseMessage = null },
            title = { Text("Respuesta de entrega", fontWeight = FontWeight.Bold) },
            text = { Text(deliveryResponseMessage!!) },
            confirmButton = {
                Button(
                    onClick = { deliveryResponseMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (groupToEditQty != null) {
        CorregirCantidadPedidaDialog(
            group = groupToEditQty!!,
            item = item.copy(products = currentProducts),
            activeWarehouse = activeWarehouse,
            onDismiss = { groupToEditQty = null },
            onSaveSuccess = { updatedProducts ->
                currentProducts = updatedProducts
                groupToEditQty = null
                val updatedItem = item.copy(products = updatedProducts)
                onItemUpdated?.invoke(updatedItem)
            }
        )
    }

    val isDarkTheme = isSystemInDarkTheme()

    val isOV = item.tipo.contains("OV", ignoreCase = true) || item.tipo.contains("Orden", ignoreCase = true) || item.tipo.contains("Venta", ignoreCase = true)
    val tipoLabel = if (isOV) "OV cliente" else "Reserva cliente"
    val tipoBadgeText = if (isOV) "⊙ OV" else "⊙ Reserva"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = tipoLabel,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkTheme) Color(0xFF3A2010) else Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFF97316) else Color(0xFFFFB74D))
                ) {
                    Text(
                        text = tipoBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFFFEDD5) else Color(0xFFE65100)
                    )
                }
            }

            // Title & Client
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.reference.ifBlank { item.number }.ifBlank { "SIN NÚMERO" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = item.client.ifBlank { "CLIENTE NO ESPECIFICADO" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status & Recipient Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ServialRed
                ) {
                    Text(
                        text = "● En atención",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "Retira ${item.recipientName ?: "RECEPTOR AUTORIZADO"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${item.products.size} líneas de ${if (isOV) "OV" else "reserva"} en mesón Serviall · listas para retiro",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section Header
            Text(
                text = "LÍNEAS SERVIALL · ORDEN DE PICKING (${groupedProducts.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Scanning Bar / Quick Code Input
            OutlinedTextField(
                value = scanCodeInput,
                onValueChange = { code ->
                    scanCodeInput = code
                    val match = item.products.firstOrNull { it.code.equals(code, true) || it.substitute.equals(code, true) }
                    if (match != null) {
                        val matchKey = itemKey(match)
                        val groupKey = match.code.ifBlank { match.substitute }
                        val group = groupedProducts.firstOrNull { it.codeKey == groupKey }
                        val totalSelected = group?.options?.sumOf { productQuantities[itemKey(it)] ?: 0 } ?: 0
                        val groupMax = group?.expectedQty ?: match.expectedQty
                        val groupDelivered = group?.options?.sumOf { it.delivrdqty } ?: 0
                        val groupNeeded = (groupMax - groupDelivered).coerceAtLeast(0)
                        val prodNeeded = (match.expectedQty - match.delivrdqty).coerceAtLeast(0)
                        val matchMax = if (match.actualQty > 0) minOf(prodNeeded, match.actualQty) else prodNeeded
                        val current = productQuantities[matchKey] ?: 0

                        if (totalSelected < groupNeeded && current < matchMax) {
                            productQuantities[matchKey] = current + 1
                            Toast.makeText(context, "Escaneado: ${match.substitute.ifBlank { match.code }} (+1)", Toast.LENGTH_SHORT).show()
                            scanCodeInput = ""
                        } else if (current >= matchMax) {
                            Toast.makeText(context, "Stock / Máximo alcanzado para ${match.substitute.ifBlank { match.code }} ($matchMax)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Línea completa para esta entrega ($totalSelected de $groupNeeded)", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                placeholder = { Text("Digitar o escanear código / homólogo...") },
                leadingIcon = { Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showCameraScan = true }) {
                        Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = "Cámara", tint = ServialRed)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Products Grouped Cards List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedProducts, key = { it.codeKey }) { group ->
                    val totalSelectedInGroup = group.options.sumOf { productQuantities[itemKey(it)] ?: 0 }
                    val activeOptionsCount = group.options.count { (productQuantities[itemKey(it)] ?: 0) > 0 }
                    val isExpanded = expandedGroups[group.codeKey] ?: (group.options.size <= 1)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: SKU Cliente & Available Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SKU Cliente ${group.codeKey}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0xFF143820) else Color(0xFFE8F5E9),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF81C784))
                                ) {
                                    Text(
                                        text = "● Disponible",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF2E7D32)
                                    )
                                }
                            }

                            // Group Name & Expected Qty
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Pide ${group.expectedQty}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = ServialRed,
                                    modifier = Modifier.clickable { groupToEditQty = group }
                                )
                            }

                            // Subtitle: variants count & default Serviall code
                            Text(
                                text = "${group.options.size} variantes Serviall · por defecto ${group.options.first().code}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Delivery Summary Row
                            val groupDelivered = group.options.sumOf { it.delivrdqty }
                            val groupNeeded = (group.expectedQty - groupDelivered).coerceAtLeast(0)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Se entrega con ${if (activeOptionsCount > 0) "$activeOptionsCount SKU Serviall" else "0 SKU Serviall"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$totalSelectedInGroup de $groupNeeded",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (totalSelectedInGroup == groupNeeded) Color(0xFF16A34A) else ServialRed
                                )
                            }

                            // Options List (Colapsado / Expandido)
                            val visibleOptions = if (isExpanded) {
                                group.options
                            } else {
                                val selected = group.options.filter { (productQuantities[itemKey(it)] ?: 0) > 0 }
                                if (selected.isNotEmpty()) selected else listOf(group.options.first())
                            }

                            visibleOptions.forEach { prod ->
                                val prodKey = itemKey(prod)
                                val currentQty = productQuantities[prodKey] ?: 0
                                val isSelected = currentQty > 0

                                val optionBg = if (isSelected) {
                                    if (isDarkTheme) Color(0xFF143820) else Color(0xFFF0FDF4)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                val optionBorder = if (isSelected) {
                                    if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF86EFAC)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                                val optionTextColor = if (isSelected) {
                                    if (isDarkTheme) Color(0xFFF0FDF4) else Color(0xFF14532D)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                val optionSubtextColor = if (isSelected) {
                                    if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = optionBg,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, optionBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${prod.code} × $currentQty",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = optionTextColor
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Seleccionado",
                                                        tint = if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${if (isSelected) "escaneado / asignado" else "por escanear"}",
                                                fontSize = 11.sp,
                                                color = optionSubtextColor
                                            )
                                        }

                                        val prodNeeded = (prod.expectedQty - prod.delivrdqty).coerceAtLeast(0)
                                        val prodMax = if (prod.actualQty > 0) minOf(prodNeeded, prod.actualQty) else prodNeeded
                                        val canDecrease = currentQty > 0
                                        val canIncrease = totalSelectedInGroup < groupNeeded && currentQty < prodMax

                                        // Plus / Minus Controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (canDecrease) {
                                                            productQuantities[prodKey] = currentQty - 1
                                                        }
                                                    },
                                                    enabled = canDecrease
                                                ) {
                                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (canDecrease) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                }
                                            }

                                            Text(
                                                text = "$currentQty",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = optionTextColor,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (canIncrease) ServialRed else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (totalSelectedInGroup >= groupNeeded) {
                                                            Toast.makeText(context, "Línea completa para esta entrega ($groupNeeded pend.)", Toast.LENGTH_SHORT).show()
                                                        } else if (currentQty >= prodMax) {
                                                            Toast.makeText(context, "Stock / Máximo alcanzado para ${prod.substitute.ifBlank { prod.code }} ($prodMax)", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            productQuantities[prodKey] = currentQty + 1
                                                        }
                                                    },
                                                    enabled = canIncrease
                                                ) {
                                                    Text(
                                                        "+",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = if (canIncrease) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Repartir entre N variantes Toggle Button (if group has multiple options)
                            if (group.options.size > 1) {
                                TextButton(
                                    onClick = { expandedGroups[group.codeKey] = !isExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = null,
                                        tint = ServialRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isExpanded) "Ocultar variantes" else "⇆ Repartir entre ${group.options.size} variantes Serviall",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ServialRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Confirm Button -> opens ConfirmarEntregaScreen
            Button(
                onClick = {
                    val totalUnitsThisSession = currentProducts.sumOf { productQuantities[itemKey(it)] ?: 0 }
                    if (totalUnitsThisSession <= 0) {
                        Toast.makeText(context, "Seleccione al menos 1 unidad para entregar", Toast.LENGTH_SHORT).show()
                    } else {
                        showConfirmarEntregaScreen = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Atención / Entrega", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SignatureCanvas(
    paths: SnapshotStateList<Path>,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (paths.isEmpty() && currentPath == null) {
            Text(
                text = "Firme aquí con el dedo",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val p = Path().apply { moveTo(offset.x, offset.y) }
                            currentPath = p
                            paths.add(p)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                            val lastIdx = paths.lastIndex
                            if (lastIdx >= 0) {
                                val p = paths[lastIdx]
                                paths[lastIdx] = Path().apply { addPath(p) }
                            }
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = ServialRed,
                    style = Stroke(width = 6f)
                )
            }
        }
    }
}

private fun generateSignatureBase64(paths: List<Path>, widthPx: Int, heightPx: Int): String? {
    if (paths.isEmpty()) return null
    val w = if (widthPx > 0) widthPx else 600
    val h = if (heightPx > 0) heightPx else 300
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(220, 38, 38)
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
    }
    paths.forEach { path ->
        canvas.drawPath(path.asAndroidPath(), paint)
    }
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun ConfirmarEntregaScreen(
    item: ServialItem,
    activeWarehouse: String,
    productQuantities: Map<String, Int>,
    onDismiss: () -> Unit,
    onDeliverConfirmed: (ServialItem, String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    fun itemKey(p: ProductItem): String = "${p.code.trim()}_${p.substitute.trim()}_${p.linenum}"

    var recipientNameInput by remember { mutableStateOf(item.recipientName ?: "") }
    var folioRebajaInput by remember { mutableStateOf(item.folioRebaja ?: "") }
    var attachedPhotoBase64 by remember { mutableStateOf<String?>(item.photoBase64) }
    var deliveryResponseMessage by remember { mutableStateOf<String?>(null) }
    var isDelivering by remember { mutableStateOf(false) }
    var isDetailExpanded by remember { mutableStateOf(false) }

    val signaturePaths = remember { mutableStateListOf<Path>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val photoFile = remember { File.createTempFile("delivery_proof_", ".jpg", context.cacheDir) }
    val photoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                val bytes = stream.toByteArray()
                attachedPhotoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Toast.makeText(context, "Foto adjuntada exitosamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al procesar la foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val groupedProducts = remember(item.products) {
        item.products.groupBy { it.substitute.ifBlank { it.code } }.map { (subKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().code
            val maxExpected = options.maxOfOrNull { it.expectedQty } ?: 1
            ProductGroup(
                codeKey = subKey,
                name = groupName,
                expectedQty = maxExpected,
                options = options
            )
        }
    }

    val totalUnitsThisSession = item.products.sumOf { productQuantities[itemKey(it)] ?: 0 }
    val totalSkuServiallDelivered = item.products.count { (productQuantities[itemKey(it)] ?: 0) > 0 }

    val completosCount = groupedProducts.count { g ->
        val deliveredPrev = g.options.sumOf { it.delivrdqty }
        val deliveredNow = g.options.sumOf { productQuantities[itemKey(it)] ?: 0 }
        (deliveredPrev + deliveredNow) >= g.expectedQty
    }
    val parcialesCount = groupedProducts.count { g ->
        val deliveredPrev = g.options.sumOf { it.delivrdqty }
        val deliveredNow = g.options.sumOf { productQuantities[itemKey(it)] ?: 0 }
        val total = deliveredPrev + deliveredNow
        total > 0 && total < g.expectedQty
    }
    val noEntregadosCount = groupedProducts.count { g ->
        val deliveredPrev = g.options.sumOf { it.delivrdqty }
        val deliveredNow = g.options.sumOf { productQuantities[itemKey(it)] ?: 0 }
        (deliveredPrev + deliveredNow) == 0
    }

    if (deliveryResponseMessage != null) {
        AlertDialog(
            onDismissRequest = { deliveryResponseMessage = null },
            title = { Text("Respuesta de entrega", fontWeight = FontWeight.Bold) },
            text = { Text(deliveryResponseMessage!!) },
            confirmButton = {
                Button(
                    onClick = { deliveryResponseMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = "Confirmar entrega",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val isOV = item.tipo.contains("OV", ignoreCase = true) || item.tipo.contains("Orden", ignoreCase = true) || item.tipo.contains("Venta", ignoreCase = true)
                Text(
                    text = "${if (isOV) "OV" else "Reserva"} ${item.reference.ifBlank { item.number }} · Entrega: [$activeWarehouse]",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }

            HorizontalDivider()

            // Scrollable Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen Box Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "RESUMEN · ${groupedProducts.size} SKU CLIENTE PEDIDOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        // 3 Stat cards row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Green Card: Completos
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0xFF143820) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF86EFAC)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("$completosCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D))
                                    Text("Completos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D))
                                    Text("se cierran", fontSize = 10.sp, color = if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF166534))
                                }
                            }

                            // Purple Card: Parciales
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0xFF2E1065) else Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFA855F7) else Color(0xFFE9D5FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("$parcialesCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
                                    Text("Parciales", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
                                    Text("con saldo", fontSize = 10.sp, color = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF6B21A8))
                                }
                            }

                            // Grey Card: No entregados
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFF3F4F6),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4B5563) else Color(0xFFE5E7EB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("$noEntregadosCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("No entregados", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("quedan abiertos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Summary pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Se entregan $totalUnitsThisSession unid. con $totalSkuServiallDelivered SKU Serviall",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Abiertos: $parcialesCount a OV pendiente (vuelve a retirar) · $noEntregadosCount sigue en la reserva (no confirma)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Notice text
                Text(
                    text = "Al firmar se crean en SAP la OV y la entrega, se rebaja el stock y queda la lista para rebajar en el sistema del cliente",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input 1: Usuario final que recibe
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Usuario final que recibe",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = recipientNameInput,
                        onValueChange = { recipientNameInput = it },
                        placeholder = { Text("[NOMBRE SOLICITANTE]") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Input 2: Folio de rebaja (sistema del cliente)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Folio de rebaja (sistema del cliente)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lo genera el sistema SAP del cliente al rebajar el pedido. No es el N° de reserva.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = folioRebajaInput,
                        onValueChange = { folioRebajaInput = it },
                        placeholder = { Text("N° folio de rebaja") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Row: Adjuntar foto
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Solo si el contrato exige comprobante impreso firmado",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                try {
                                    photoLauncher.launch(photoUri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al abrir cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (attachedPhotoBase64 != null) Icons.Rounded.Check else Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (attachedPhotoBase64 != null) Color(0xFF16A34A) else ServialRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (attachedPhotoBase64 != null) "Foto adjunta" else "Adjuntar foto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (attachedPhotoBase64 != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Section: Firma de quien recibe
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Firma de quien recibe",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { signaturePaths.clear() }
                        ) {
                            Text("Borrar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ServialRed)
                        }
                    }

                    // Signature Canvas Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .onGloballyPositioned { layoutCoordinates ->
                                canvasSize = layoutCoordinates.size
                            }
                    ) {
                        SignatureCanvas(
                            paths = signaturePaths,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Collapsible section: Ver detalle de la entrega
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailExpanded = !isDetailExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ver detalle de la entrega",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isDetailExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isDetailExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            item.products.forEach { prod ->
                                val qty = productQuantities[itemKey(prod)] ?: 0
                                if (qty > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text("SKU cliente: ${prod.substitute.ifBlank { prod.code }} · Serviall: ${prod.code}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("$qty unid.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ServialRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Confirm Button
            Button(
                onClick = {
                    if (!isDelivering) {
                        if (recipientNameInput.isBlank()) {
                            Toast.makeText(context, "Por favor ingrese el nombre de quien recibe", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (signaturePaths.isEmpty()) {
                            Toast.makeText(context, "Por favor realice la firma en el recuadro", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isDelivering = true
                        val width = if (canvasSize.width > 0) canvasSize.width else 600
                        val height = if (canvasSize.height > 0) canvasSize.height else 300
                        val signatureBase64Str = generateSignatureBase64(signaturePaths, width, height)

                        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        val updatedProducts = item.products.map { p ->
                            val qty = productQuantities[itemKey(p)] ?: 0
                            p.copy(qttytrans = qty, delivrdqty = p.delivrdqty + qty)
                        }
                        val updatedItem = item.copy(
                            recipientName = recipientNameInput,
                            folioRebaja = folioRebajaInput.ifBlank { null },
                            photoBase64 = attachedPhotoBase64,
                            signatureBase64 = signatureBase64Str,
                            status = ItemStatus.ENTREGADO,
                            completado = 100.0,
                            products = updatedProducts,
                            deliveryTimestamp = nowStr,
                            endtime = nowStr
                        )

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val api = ServicioAPI.getInstance()
                                val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")
                                val response = api.postEntrega(base64Auth, activeWarehouse, updatedItem)
                                withContext(Dispatchers.Main) {
                                    isDelivering = false
                                    if (response.isBlank()) {
                                        Toast.makeText(context, "¡Entrega registrada exitosamente!", Toast.LENGTH_LONG).show()
                                        onDeliverConfirmed(updatedItem, "", "")
                                    } else {
                                        deliveryResponseMessage = response
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isDelivering = false
                                    deliveryResponseMessage = "Error al procesar entrega: ${e.message}"
                                }
                            }
                        }
                    }
                },
                enabled = !isDelivering,
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isDelivering) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Confirmar entrega ($totalUnitsThisSession unidades)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun OpcionesAtencionDialog(
    item: ServialItem,
    onDismiss: () -> Unit,
    onComenzarAtencion: (updatedItem: ServialItem) -> Unit,
    onSoloVerPedido: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    var recipientInput by remember { mutableStateOf(item.recipientName ?: "") }

    val card1Bg = if (isDarkTheme) ServialRed.copy(alpha = 0.15f) else Color(0xFFFFF5F5)
    val inputBg = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
    val inputText = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                val isOV = item.tipo.contains("OV", ignoreCase = true) || item.tipo.contains("Orden", ignoreCase = true) || item.tipo.contains("Venta", ignoreCase = true)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isOV) "OV cliente" else "Reserva cliente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.reference.ifBlank { item.number }.ifBlank { "SIN NÚMERO" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.client} · ${item.products.size} líneas Serviall",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "¿Qué quieres hacer?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Option 1: Comenzar atención
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = card1Bg,
                    border = BorderStroke(1.5.dp, ServialRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Comenzar atención",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ServialRed
                        )
                        Text(
                            text = "El tiempo de atención corre desde ahora hasta la firma. Puedes escanear y entregar.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Quién viene a retirar (obligatorio)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = { recipientInput = it },
                            placeholder = { Text("Nombre o credencial", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = inputText, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = inputText,
                                unfocusedTextColor = inputText,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = ServialRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (recipientInput.isBlank()) {
                                    Toast.makeText(context, "Ingrese el nombre o credencial de quien retira", Toast.LENGTH_SHORT).show()
                                } else {
                                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                    val updated = item.copy(starttime = nowStr, recipientName = recipientInput)
                                    onComenzarAtencion(updated)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Comenzar atención", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                // Option 2: Solo ver el pedido
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onSoloVerPedido,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Solo ver el pedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SoloVerPedidoScreen(
    item: ServialItem,
    onDismiss: () -> Unit,
    onComenzarAtencionClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val isOV = item.tipo.contains("OV", ignoreCase = true) || item.tipo.contains("Orden", ignoreCase = true) || item.tipo.contains("Venta", ignoreCase = true)
    val groupedProducts = remember(item.products) {
        item.products.groupBy { it.substitute.ifBlank { it.code } }.map { (subKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().code
            val maxExpected = options.maxOfOrNull { it.expectedQty } ?: 1
            ProductGroup(
                codeKey = subKey,
                name = groupName,
                expectedQty = maxExpected,
                options = options
            )
        }
    }

    val disponiblesCount = groupedProducts.count { g ->
        val actual = g.options.maxOfOrNull { it.actualQty } ?: 0
        actual >= g.expectedQty
    }
    val quiebresCount = groupedProducts.count { g ->
        val actual = g.options.maxOfOrNull { it.actualQty } ?: 0
        actual == 0
    }
    val parcialesCount = groupedProducts.count { g ->
        val actual = g.options.maxOfOrNull { it.actualQty } ?: 0
        actual > 0 && actual < g.expectedQty
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = if (isOV) "OV cliente" else "Reserva cliente",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Solo consulta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Title & Client
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.reference.ifBlank { item.number }.ifBlank { "SIN NÚMERO" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = item.client.ifBlank { "CLIENTE NO ESPECIFICADO" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Reserva · ${groupedProducts.size} líneas Serviall · $disponiblesCount disponibles · $parcialesCount stock parcial · $quiebresCount quiebre",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Estás viendo el pedido. Para escanear y entregar, comienza la atención.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Product Cards List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupedProducts, key = { it.codeKey }) { group ->
                    val actualStock = group.options.maxOfOrNull { it.actualQty } ?: 0
                    val expectedQty = group.expectedQty
                    val mainSub = group.options.first().substitute.ifBlank { group.codeKey }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Row: Ubicación / SKU Serviall & Availability Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SKU Serviall ${group.codeKey}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Availability Badge
                                when {
                                    actualStock >= expectedQty -> {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkTheme) Color(0xFF143820) else Color(0xFFE8F5E9),
                                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF81C784))
                                        ) {
                                            Text(
                                                text = "● Disponible",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    actualStock == 0 -> {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkTheme) Color(0xFF3B1212) else Color(0xFFFFEBEE),
                                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFEF4444) else Color(0xFFEF9A9A))
                                        ) {
                                            Text(
                                                text = "Quiebre",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                    else -> { // actualStock > 0 && actualStock < expectedQty
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkTheme) Color(0xFF382D14) else Color(0xFFFFF8E1),
                                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFF59E0B) else Color(0xFFFFE082))
                                        ) {
                                            Text(
                                                text = "◐ Stock parcial",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFF57F17)
                                            )
                                        }
                                    }
                                }
                            }

                            // Group Name & Expected Qty
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Pide $expectedQty",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Subtitle: SKU cliente, Stock, and homólogos
                            val subtitleText = when {
                                actualStock >= expectedQty -> {
                                    "SKU cliente $mainSub · Stock $actualStock ${if (group.options.size > 1) "· ${group.options.size} homólogos" else ""}"
                                }
                                actualStock == 0 -> {
                                    "SKU cliente $mainSub · Stock 0"
                                }
                                else -> {
                                    "SKU cliente $mainSub · Stock $actualStock · alcanza para $actualStock de $expectedQty"
                                }
                            }

                            Text(
                                text = subtitleText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Button: Comenzar atención
            Button(
                onClick = onComenzarAtencionClick,
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Comenzar atención", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
