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
import com.serviall.saputils.ui.theme.ServialRed

@Composable
fun WarehouseSelectionDialog(
    currentWarehouse: String,
    onWarehouseSelected: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val warehouses = listOf("Bodega Central", "Bodega Norte", "Bodega Sur", "Bodega Este")
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
