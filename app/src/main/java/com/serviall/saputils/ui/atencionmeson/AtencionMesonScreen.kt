package com.serviall.saputils.ui.atencionmeson

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                openDocuments = openDocuments.map { if (it.id == updatedItem.id) updatedItem else it }
                selectedItemForDelivery = null
                Toast.makeText(context, "Entrega registrada para ${updatedItem.reference.ifBlank { updatedItem.number }}", Toast.LENGTH_LONG).show()
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ServialRed)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { loadOpenDocuments() }, colors = ButtonDefaults.buttonColors(containerColor = ServialRed)) {
                        Text("Reintentar")
                    }
                }
            }
        } else if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                                Text(
                                    text = "Reserva ${item.reference.ifBlank { item.number }}",
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

@Composable
fun CargarReservaEditScreen(
    item: ServialItem,
    activeWarehouse: String,
    onDismiss: () -> Unit,
    onSave: (ServialItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var editableItem by remember { mutableStateOf(item) }
    var pickupUser by remember { mutableStateOf("") }
    var newSkuInput by remember { mutableStateOf("") }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
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
                        text = "LÍNEAS EN CÓDIGO DEL CLIENTE (${editableItem.products.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(editableItem.products, key = { "${it.code}_${it.linenum}" }) { prod ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "SKU cliente (Línea: ${prod.linenum})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = prod.code,
                                        onValueChange = { newCode ->
                                            val updatedProducts = editableItem.products.map { p ->
                                                if (p.code == prod.code && p.linenum == prod.linenum) p.copy(code = newCode) else p
                                            }
                                            editableItem = editableItem.copy(products = updatedProducts)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (prod.expectedQty > 1) {
                                                val updatedProducts = editableItem.products.map { p ->
                                                    if (p.code == prod.code && p.linenum == prod.linenum) p.copy(expectedQty = p.expectedQty - 1) else p
                                                }
                                                editableItem = editableItem.copy(products = updatedProducts)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "${prod.expectedQty}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val updatedProducts = editableItem.products.map { p ->
                                                if (p.code == prod.code && p.linenum == prod.linenum) p.copy(expectedQty = p.expectedQty + 1) else p
                                            }
                                            editableItem = editableItem.copy(products = updatedProducts)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ServialRed.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Leído de la foto",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ServialRed
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val updatedProducts = editableItem.products.filter { !(it.code == prod.code && it.linenum == prod.linenum) }
                                        editableItem = editableItem.copy(products = updatedProducts)
                                    }
                                ) {
                                    Text("Quitar línea", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
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
                            placeholder = { Text("+ Escanea o escribe SKU cliente") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newSkuInput.isNotBlank()) {
                                    val newProd = ProductItem(code = newSkuInput, name = "Nuevo Producto", expectedQty = 1)
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
                        text = "Revisa cada línea contra el documento: el SKU y la cantidad leídos de la foto se pueden corregir. Al guardar, SAP asigna a cada línea el SKU Serviall a entregar según la homologación. La reserva queda primera en el mesón para entregarla.",
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

private data class ProductGroup(
    val codeKey: String,
    val name: String,
    val expectedQty: Int,
    val options: List<ProductItem>
)

@Composable
fun AtencionMesonDetailScreen(
    item: ServialItem,
    activeWarehouse: String,
    onDismiss: () -> Unit,
    onDeliverConfirmed: (ServialItem, String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDelivering by remember { mutableStateOf(false) }
    var deliveryResponseMessage by remember { mutableStateOf<String?>(null) }
    var scanCodeInput by remember { mutableStateOf("") }
    var showCameraScan by remember { mutableStateOf(false) }

    fun itemKey(p: ProductItem): String = "${p.code.trim()}_${p.substitute.trim()}_${p.linenum}"

    val productQuantities = remember {
        mutableStateMapOf<String, Int>().apply {
            item.products.forEach { p ->
                put(itemKey(p), 0)
            }
        }
    }

    val groupedProducts = remember(item.products) {
        item.products.groupBy { it.code.ifBlank { it.substitute } }.map { (cKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().substitute.ifBlank { cKey }
            val maxExpected = options.maxOfOrNull { it.expectedQty } ?: 1
            ProductGroup(
                codeKey = cKey,
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

    val isDarkTheme = isSystemInDarkTheme()

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
                        text = "Reserva cliente",
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
                        text = "⊙ Reserva",
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
                    text = "${item.products.size} líneas de reserva en mesón Serviall · listas para retiro",
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
                            // Top Row: Ubicación / SKU Serviall & Available Badge
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Subtitle: options count & default substitute
                            Text(
                                text = "${group.options.size} homólogos cliente · por defecto ${group.options.first().substitute.ifBlank { group.codeKey }}",
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
                                    text = "Se entrega con ${if (activeOptionsCount > 0) "$activeOptionsCount SKU cliente" else "0 SKU cliente"}",
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
                                                    text = "${prod.substitute.ifBlank { prod.code }} × $currentQty",
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

                            // Repartir entre N homólogos Toggle Button (if group has multiple options)
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
                                        text = if (isExpanded) "Ocultar homólogos" else "⇆ Repartir entre ${group.options.size} homólogos",
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

            // Bottom Confirm Button
            Button(
                onClick = {
                    if (!isDelivering) {
                        isDelivering = true
                        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        val updatedProducts = item.products.map { p ->
                            val qty = productQuantities[itemKey(p)] ?: 0
                            p.copy(qttytrans = qty, delivrdqty = p.delivrdqty + qty)
                        }
                        val updatedItem = item.copy(
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
                                        Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isDelivering = false
                                    Toast.makeText(context, "Error al procesar entrega: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                },
                enabled = !isDelivering,
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isDelivering) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Atención / Entrega", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Reserva cliente",
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
    val groupedProducts = remember(item.products) {
        item.products.groupBy { it.code.ifBlank { it.substitute } }.map { (cKey, options) ->
            val groupName = options.firstOrNull { it.name.isNotBlank() }?.name ?: options.first().substitute.ifBlank { cKey }
            val maxExpected = options.maxOfOrNull { it.expectedQty } ?: 1
            ProductGroup(
                codeKey = cKey,
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
                        text = "Reserva cliente",
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
