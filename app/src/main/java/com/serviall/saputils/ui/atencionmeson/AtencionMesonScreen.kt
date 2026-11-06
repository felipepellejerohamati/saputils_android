package com.serviall.saputils.ui.atencionmeson

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
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
    var selectedTypeFilter by remember { mutableStateOf("Todos") } // Todos, Reserva, OV
    var selectedStatusFilter by remember { mutableStateOf<ItemStatus?>(null) }
    var selectedItemForDelivery by remember { mutableStateOf<ServialItem?>(null) }

    val loadOpenDocuments = {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val servicioAPI = ServicioAPI.getInstance()
                val base64String = "Basic " + get64BaseString("serviall:8400Abn.,")
                val list = servicioAPI.OpenDocuments(base64String, activeWarehouse)
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
                        if (item.number.isBlank()) {
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

    val filteredItems = openDocuments.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.number.contains(searchQuery, ignoreCase = true) ||
                item.id.contains(searchQuery, ignoreCase = true) ||
                item.client.contains(searchQuery, ignoreCase = true) ||
                item.products.any { p -> p.code.contains(searchQuery, ignoreCase = true) || p.substitute.contains(searchQuery, ignoreCase = true) }

        val matchesType = selectedTypeFilter == "Todos" || item.tipo.equals(selectedTypeFilter, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || item.status == selectedStatusFilter

        matchesSearch && matchesType && matchesStatus
    }

    if (editingReservation != null) {
        CargarReservaEditScreen(
            item = editingReservation!!,
            activeWarehouse = activeWarehouse,
            onDismiss = { editingReservation = null },
            onSave = { savedItem ->
                openDocuments = listOf(savedItem) + openDocuments
                editingReservation = null
                Toast.makeText(context, "¡Reserva #${savedItem.number} guardada con éxito!", Toast.LENGTH_LONG).show()
            }
        )
        return
    }

    if (selectedItemForDelivery != null) {
        AtencionMesonDetailScreen(
            item = selectedItemForDelivery!!,
            onDismiss = { selectedItemForDelivery = null },
            onDeliverConfirmed = { updatedItem, _, _ ->
                openDocuments = openDocuments.map { if (it.id == updatedItem.id) updatedItem else it }
                selectedItemForDelivery = null
                Toast.makeText(context, "Entrega registrada para ${updatedItem.number}", Toast.LENGTH_LONG).show()
            }
        )
        return
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

        // Type Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Todos", "Reserva", "OV").forEach { type ->
                FilterChip(
                    selected = selectedTypeFilter == type,
                    onClick = { selectedTypeFilter = type },
                    label = { Text(type, fontSize = 11.sp) }
                )
            }
        }

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
                                    text = "Reserva ${item.number}",
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
                                onClick = { selectedItemForDelivery = item },
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
                        value = editableItem.number,
                        onValueChange = { newNum -> editableItem = editableItem.copy(number = newNum) },
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
                        placeholder = { Text("Nombre o credencial") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                onClick = { onSave(editableItem) },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Guardar reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AtencionMesonDetailScreen(
    item: ServialItem,
    onDismiss: () -> Unit,
    onDeliverConfirmed: (ServialItem, String, String) -> Unit
) {
    val context = LocalContext.current
    var scanCodeInput by remember { mutableStateOf("") }
    var showCameraScan by remember { mutableStateOf(false) }
    val productQuantities = remember {
        mutableStateMapOf<String, Int>().apply {
            item.products.forEach { p -> put(p.code, p.qttytrans) }
        }
    }

    if (showCameraScan) {
        CameraScanDialog(
            title = "Escanear QR o Código de Barras",
            onDismiss = { showCameraScan = false },
            onCodeScanned = { scannedCode ->
                showCameraScan = false
                val match = item.products.firstOrNull { it.code.equals(scannedCode, true) || it.substitute.equals(scannedCode, true) }
                if (match != null) {
                    val current = productQuantities[match.code] ?: 0
                    if (current < match.expectedQty) {
                        productQuantities[match.code] = current + 1
                        Toast.makeText(context, "Escaneado por cámara: ${match.code} (+1)", Toast.LENGTH_SHORT).show()
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reserva - ${item.number}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ServialRed
                )
                TextButton(onClick = onDismiss) {
                    Text("Volver")
                }
            }

            Button(
                onClick = { showCameraScan = true },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear con Cámara (QR / Barras)", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = scanCodeInput,
                onValueChange = { code ->
                    scanCodeInput = code
                    val match = item.products.firstOrNull { it.code.equals(code, true) || it.substitute.equals(code, true) }
                    if (match != null) {
                        val current = productQuantities[match.code] ?: 0
                        if (current < match.expectedQty) {
                            productQuantities[match.code] = current + 1
                            Toast.makeText(context, "Escaneado: ${match.code} (+1)", Toast.LENGTH_SHORT).show()
                            scanCodeInput = ""
                        }
                    }
                },
                label = { Text("Digitar o escanear código / substituto...") },
                leadingIcon = { Icon(imageVector = Icons.Rounded.QrCodeScanner, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showCameraScan = true }) {
                        Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = "Escanear con cámara", tint = ServialRed)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ServialRed,
                    unfocusedBorderColor = ServialRed,
                    errorBorderColor = ServialRed
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(item.products) { prod ->
                    val currentQty = productQuantities[prod.code] ?: 0
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = prod.name.ifBlank { prod.code }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "Código: ${prod.code} ${if (prod.substitute.isNotBlank()) "• Sub: ${prod.substitute}" else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Esperado: ${prod.expectedQty} | Stock: ${prod.actualQty} | Entregado: ${prod.delivrdqty}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ServialRed)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    if (currentQty > 0) productQuantities[prod.code] = currentQty - 1
                                }) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Text(text = "${currentQty}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(onClick = {
                                    if (currentQty < (prod.expectedQty-prod.delivrdqty) && currentQty<prod.actualQty)
                                        productQuantities[prod.code] = currentQty + 1
                                }) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val updatedProducts = item.products.map { p ->
                        p.copy(delivrdqty = productQuantities[p.code] ?: p.delivrdqty)
                    }
                    val updatedItem = item.copy(
                        status = ItemStatus.ENTREGADO,
                        completado = 100.0,
                        products = updatedProducts,
                        deliveryTimestamp = "2025-05-10 16:00"
                    )
                    onDeliverConfirmed(updatedItem, "", "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar Atención / Entrega", fontWeight = FontWeight.Bold)
            }
        }
    }
}
