package com.serviall.saputils.ui.reconocer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.data.db.RecognitionDatabase
import com.serviall.saputils.data.db.RecognitionHistoryEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReconocerScreen(
    modifier: Modifier = Modifier,
    viewModel: ServialViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var recognitionStatus by remember { mutableStateOf("") }
    var historyList by remember { mutableStateOf<List<RecognitionHistoryEntity>>(emptyList()) }

    val activeWhs = viewModel?.activeWarehouse?.collectAsState()?.value
        ?: UserSession.wharehouse.ifBlank { "Bodega Central" }

    // Cargar historial de SQLite (Room)
    LaunchedEffect(Unit) {
        try {
            val dao = RecognitionDatabase.getDatabase(context).recognitionHistoryDao()
            dao.getAllHistory().collect { list ->
                historyList = list
            }
        } catch (_: Exception) {}
    }

    fun processTesseractOcr(bitmap: Bitmap?) {
        if (bitmap != null) {
            capturedBitmap = bitmap
            isScanning = true
            recognitionStatus = "Enviando imagen HD a tesseractocr (Whs: $activeWhs)..."

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    val byteArray = stream.toByteArray()
                    val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

                    val api = ServicioAPI.getInstance()
                    val base64Auth = "Basic " + get64BaseString("serviall:8400Abn.,")

                    // Llamar al método tesseractocr especificando auth base64 y whscode
                    val responseStr = api.tesseractocr(base64Auth, activeWhs, requestBody)
                    EscribirLog(tag = "TesseractOCR", mensaje = "Respuesta tesseractocr HD: $responseStr")

                    // Almacenar respuesta en la tabla SQLite (Room)
                    val historyDao = RecognitionDatabase.getDatabase(context).recognitionHistoryDao()
                    val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    historyDao.insertHistory(
                        RecognitionHistoryEntity(
                            timestamp = timestampStr,
                            whscode = activeWhs,
                            responseJson = responseStr,
                            summary = "OCR HD - Resp: ${responseStr.take(80)}"
                        )
                    )

                    withContext(Dispatchers.Main) {
                        isScanning = false
                        recognitionStatus = "Envio y respuesta registrada"
                        Toast.makeText(context, "¡Respuesta guardada en historial!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    EscribirLog(tag = "TesseractOCR", mensaje = "Error en tesseractocr HD: ${e.message}")
                    withContext(Dispatchers.Main) {
                        isScanning = false
                        recognitionStatus = "Error en API tesseractocr HD: ${e.message}"
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            isScanning = false
            Toast.makeText(context, "No se capturó ninguna foto", Toast.LENGTH_SHORT).show()
        }
    }

    val photoFile = remember {
        File.createTempFile("sap_high_res_", ".jpg", context.cacheDir)
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
            processTesseractOcr(bitmap)
        } else {
            isScanning = false
            Toast.makeText(context, "No se capturó la foto en alta calidad", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Actions Row: Capture Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isScanning = true
                    try {
                        cameraLauncher.launch(photoUri)
                    } catch (e: Exception) {
                        isScanning = false
                        Toast.makeText(context, "Error al abrir la cámara HD: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Capturar Foto HD para tesseractocr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ServialRed)
            Text(text = recognitionStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Preview thumbnail
        if (capturedBitmap != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().height(90.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Foto capturada HD",
                        modifier = Modifier.size(74.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "Enviado a tesseractocr (Whs: $activeWhs)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ServialRed)
                        Text(text = "Historial: ${historyList.size} registro(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // List Header: Historial SQLite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.History, contentDescription = null, tint = ServialRed, modifier = Modifier.size(18.dp))
                Text(
                    text = "Historial de Respuestas (${historyList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Sin historial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Capture una foto en alta calidad para enviarla a tesseractocr (Whs: $activeWhs). Las respuestas de la API se guardarán y mostrarán aquí en el historial.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Historial List View (SQLite)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList, key = { it.id }) { hist ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Almacén: ${hist.whscode}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ServialRed
                                )
                                Text(
                                    text = hist.timestamp,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = hist.summary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                text = "Respuesta API: ${hist.responseJson}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}
