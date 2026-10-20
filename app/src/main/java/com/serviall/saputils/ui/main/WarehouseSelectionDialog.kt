package com.serviall.saputils.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.globals.UserSession
import com.serviall.saputils.ui.theme.ServialRed
import com.serviall.saputils.utils.get64BaseString

@Composable
fun WarehouseSelectionDialog(
    currentWarehouse: String,
    onWarehouseSelected: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var warehouses by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val servicioAPI = ServicioAPI.getInstance()
            val base64String = "Basic " + get64BaseString("serviall:8400Abn.,")
            warehouses = servicioAPI.getswhs(base64String, UserSession.username)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error al cargar bodegas"
            warehouses = listOf("Bodega Central", "Bodega Norte", "Bodega Sur")
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.Store, contentDescription = null, tint = ServialRed)
                Text("Seleccionar Bodega Activa", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Seleccione la bodega operativa para las transacciones de inventario:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ServialRed)
                    }
                } else {
                    if (errorMessage != null && warehouses.isEmpty()) {
                        Text(
                            text = "Error: $errorMessage",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    warehouses.forEach { warehouse ->
                        val isSelected = warehouse == currentWarehouse
                        Surface(
                            onClick = { onWarehouseSelected(warehouse) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ServialRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, ServialRed) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = warehouse,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ServialRed else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = ServialRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onDismiss != null) {
                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = ServialRed)
                ) {
                    Text("Aceptar")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
