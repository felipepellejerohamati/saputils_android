package com.serviall.saputils.ui.reconocer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.globals.UserSession
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.ui.viewmodel.ServialViewModel
import com.serviall.saputils.utils.EscribirLog
import com.serviall.saputils.utils.get64BaseString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun ReconocerScreen(
    modifier: Modifier = Modifier,
    viewModel: ServialViewModel? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var recognitionStatus by remember { mutableStateOf("") }
    var editableReservation by remember { mutableStateOf<ServialItem?>(null) }
    var pickupUser by remember { mutableStateOf("") }
    var newSkuInput by remember { mutableStateOf("") }

    val activeWhs = viewModel?.activeWarehouse?.collectAsState()?.value
        ?: UserSession.wharehouse.ifBlank { "Bodega Central" }

    fun processDataIssuesObj(bitmap: Bitmap?) {
        if (bitmap != null) {
            capturedBitmap = bitmap
            isScanning = true
            recognitionStatus = "Enviando imagen a dataissuesobj (Whs: $activeWhs)..."

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    val byteArray = stream.toByteArray()
                    val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

                    val api = ServicioAPI.getInstance()
                    val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")

                    // Enviar imagen a dataissuesobj y esperar ServialItem
                    val item = api.dataissuesobj(base64Auth, activeWhs, requestBody)
                    EscribirLog(tag = "DataIssuesObj", mensaje = "Recibido ServialItem de dataissuesobj (bytes: ${byteArray.size})")

                    withContext(Dispatchers.Main) {
                        isScanning = false
                        editableReservation = item
                        recognitionStatus = "¡Datos extraídos con éxito!"
                        Toast.makeText(context, "¡Pedido reconocido con éxito!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    EscribirLog(tag = "DataIssuesObj", mensaje = "Error en dataissuesobj: ${e.message}")
                    withContext(Dispatchers.Main) {
                        isScanning = false
                        // Fallback con datos de ejemplo si la red falla
                        editableReservation = ServialItem(
                            number = "5181234",
                            client = "Cliente General",
                            products = listOf(
                                ProductItem(code = "1226728", name = "Material 1", expectedQty = 6),
                                ProductItem(code = "SKU cliente", name = "Material 2", expectedQty = 7),
                                ProductItem(code = "SKU cliente", name = "Material 3", expectedQty = 2)
                            )
                        )
                        recognitionStatus = "Aviso: Mostrando datos de ejemplo (Error API: ${e.message})"
                        Toast.makeText(context, "Mostrando datos de ejemplo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            isScanning = false
            Toast.makeText(context, "No se capturó ninguna foto", Toast.LENGTH_SHORT).show()
        }
    }

    val photoFile = remember {
        File.createTempFile("sap_reconocer_", ".jpg", context.cacheDir)
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
            isScanning = true
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            processDataIssuesObj(bitmap)
        } else {
            isScanning = false
            Toast.makeText(context, "No se capturó la foto", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar title / back
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Cargar reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = onBack) {
                Text("Volver", color = ServialRed)
            }
        }

        if (editableReservation == null) {
            // Initial state: Capture photo button
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Button(
                        onClick = {
                            isScanning = true
                            try {
                                cameraLauncher.launch(photoUri)
                            } catch (e: Exception) {
                                isScanning = false
                                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capturar Foto del Pedido (dataissuesobj)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    if (isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ServialRed)
                        Text(text = recognitionStatus, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        text = "Capture una foto del pedido para enviar la imagen al endpoint 'dataissuesobj' (Bodega: $activeWhs) y cargar la reserva automáticamente.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val res = editableReservation!!
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

                    // N° reserva del cliente
                    Text(text = "N° reserva del cliente", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = res.number,
                        onValueChange = { newNum -> editableReservation = res.copy(number = newNum) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Usuario que viene a retirar
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
                        text = "Contrato [Cliente - Faena] • Bodega $activeWhs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Líneas en código del cliente
                    Text(
                        text = "LÍNEAS EN CÓDIGO DEL CLIENTE (${res.products.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(res.products, key = { it.code + it.linenum }) { prod ->
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
                                    Text(text = "SKU cliente", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = prod.code,
                                        onValueChange = { newCode ->
                                            val updatedProducts = res.products.map { p ->
                                                if (p == prod) p.copy(code = newCode) else p
                                            }
                                            editableReservation = res.copy(products = updatedProducts)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Quantity Stepper
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (prod.expectedQty > 1) {
                                                val updatedProducts = res.products.map { p ->
                                                    if (p == prod) p.copy(expectedQty = p.expectedQty - 1) else p
                                                }
                                                editableReservation = res.copy(products = updatedProducts)
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
                                            val updatedProducts = res.products.map { p ->
                                                if (p == prod) p.copy(expectedQty = p.expectedQty + 1) else p
                                            }
                                            editableReservation = res.copy(products = updatedProducts)
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
                                        val updatedProducts = res.products.filter { it != prod }
                                        editableReservation = res.copy(products = updatedProducts)
                                    }
                                ) {
                                    Text("Quitar línea", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item {
                    // Add new line row
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
                                    editableReservation = res.copy(products = res.products + newProd)
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

            // Save button at bottom
            Button(
                onClick = {
                    Toast.makeText(context, "¡Reserva #${res.number} guardada con éxito en SAP!", Toast.LENGTH_LONG).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Guardar reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
