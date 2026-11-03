package com.serviall.saputils.ui.reservas

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.utils.get64BaseString
import kotlinx.coroutines.launch

@Composable
fun ReservasScreen(
    activeWarehouse: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var reservasItems by remember { mutableStateOf<List<ServialItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemDetail by remember { mutableStateOf<ServialItem?>(null) }

    val loadReservas = {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val servicioAPI = ServicioAPI.getInstance()
                val base64String = "Basic " + get64BaseString("serviall:8400Abn.,")
                val list = servicioAPI.reservasabiertas(base64String, activeWarehouse)
                reservasItems = list
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error al cargar reservas abiertas"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeWarehouse) {
        loadReservas()
    }

    val filteredItems = reservasItems.filter { item ->
        searchQuery.isBlank() ||
                item.number.contains(searchQuery, ignoreCase = true) ||
                item.reference.contains(searchQuery, ignoreCase = true) ||
                item.client.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true)
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
                    text = "Reservas Abiertas",
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ServialRed.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = ServialRed,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar reserva, cliente, referencia...") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ServialRed)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { loadReservas() }, colors = ButtonDefaults.buttonColors(containerColor = ServialRed)) {
                        Text("Reintentar")
                    }
                }
            }
        } else if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No hay reservas abiertas disponibles.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    text = "Reserva #${item.number}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ServialRed
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ServialRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.client,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ServialRed
                                    )
                                }
                            }

                            Text(
                                text = "Ref: ${item.reference} • Fecha: ${item.date}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "Notas: ${item.notes}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Productos: ${item.products.size} ítem(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider()

                            // Action buttons: Confirmar a Orden, Descartar, Ver Detalle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { selectedItemDetail = item },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Detalle", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        reservasItems = reservasItems.filter { it.id != item.id }
                                        Toast.makeText(context, "Reserva #${item.number} confirmada a orden exitosamente", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirmar a Orden", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = {
                                        reservasItems = reservasItems.filter { it.id != item.id }
                                        Toast.makeText(context, "Reserva #${item.number} descartada", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Descartar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedItemDetail != null) {
        val item = selectedItemDetail!!
        AlertDialog(
            onDismissRequest = { selectedItemDetail = null },
            title = { Text("Detalle de Reserva #${item.number}", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Cliente: ${item.client}", fontWeight = FontWeight.SemiBold)
                        Text(text = "Referencia: ${item.reference}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Fecha: ${item.date}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Productos (${item.products.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(item.products) { prod ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = prod.name.ifBlank { prod.code }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Código: ${prod.code} | Esperado: ${prod.expectedQty}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemToConfirm = selectedItemDetail
                        selectedItemDetail = null
                        if (itemToConfirm != null) {
                            reservasItems = reservasItems.filter { it.id != itemToConfirm.id }
                            Toast.makeText(context, "Reserva #${itemToConfirm.number} confirmada a orden exitosamente", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Text("Confirmar a Orden")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemDetail = null }) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
