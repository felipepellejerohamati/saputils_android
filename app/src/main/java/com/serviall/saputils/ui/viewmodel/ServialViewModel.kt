package com.serviall.saputils.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.serviall.saputils.data.ItemStatus
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ServialItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServialViewModel : ViewModel() {

    private val _activeWarehouse = MutableStateFlow("Bodega Central")
    val activeWarehouse: StateFlow<String> = _activeWarehouse.asStateFlow()

    fun setWarehouse(warehouse: String) {
        _activeWarehouse.value = warehouse
    }

    fun setItems(newItems: List<ServialItem>) {
        _items.value = newItems
    }

    private val _items = MutableStateFlow<List<ServialItem>>(
        listOf(
            ServialItem(
                number = "1001",
                reference = "REF-5849",
                client = "Empresa Distribuidora S.A.",
                status = ItemStatus.RECEIVED,
                date = "2025-05-10 09:30",
                notes = "Paquete frágil con equipos SAP industriales",
                weightKg = 4.2,
                products = listOf(
                    ProductItem("PRD-001", "Servidor Blade Pro", "PRD-001-ALT", 3),
                    ProductItem("PRD-002", "Módulo Memoria 32GB", "", 10)
                )
            ),
            ServialItem(
                number = "1002",
                reference = "REF-5850",
                client = "Comercial del Norte Ltda.",
                status = ItemStatus.RECEIVED,
                date = "2025-05-10 10:15",
                notes = "Documentación fiscal y certificados de calibración",
                weightKg = 0.8,
                products = listOf(
                    ProductItem("PRD-003", "Licencia SAP S/4HANA", "", 2)
                )
            ),
            ServialItem(
                number = "1003",
                reference = "REF-5851",
                client = "Servicios Tecnológicos Globales",
                status = ItemStatus.DELIVERING,
                date = "2025-05-09 14:00",
                notes = "Repuestos de servidores blade",
                weightKg = 7.5,
                products = listOf(
                    ProductItem("PRD-001", "Servidor Blade Pro", "", 2),
                    ProductItem("PRD-004", "Switch Industrial 24p", "", 1)
                )
            ),
            ServialItem(
                number = "1004",
                reference = "REF-5852",
                client = "Industrias Metalúrgicas Andinas",
                status = ItemStatus.COMPLETED,
                date = "2025-05-08 11:20",
                notes = "Sensores IoT de temperatura",
                recipientName = "Carlos Mendoza",
                deliveryTimestamp = "2025-05-08 16:45",
                weightKg = 2.1,
                products = listOf(
                    ProductItem("PRD-005", "Sensor IoT Temperatura", "", 4)
                )
            ),
            ServialItem(
                number = "1005",
                reference = "REF-5853",
                client = "Logística y Transporte Austral",
                status = ItemStatus.RECEIVED,
                date = "2025-05-10 11:00",
                notes = "Terminales portátiles de código de barras",
                weightKg = 3.0,
                products = listOf(
                    ProductItem("PRD-006", "Terminal Portátil Barcode", "", 1)
                )
            )
        )
    )
    val items: StateFlow<List<ServialItem>> = _items.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun addNewReceivedItem(number: String, reference: String, client: String, notes: String, weightKg: Double) {
        val newItem = ServialItem(
            number = number.ifBlank { "1${(100..999).random()}" },
            reference = reference.ifBlank { "REF-${(1000..9999).random()}" },
            client = client.ifBlank { "Cliente General" },
            status = ItemStatus.RECEIVED,
            date = dateFormat.format(Date()),
            notes = notes,
            weightKg = weightKg,
            products = listOf(
                ProductItem("PRD-001", "Servidor Blade Pro", "", 0),
                ProductItem("PRD-002", "Módulo Memoria 32GB", "", 0)
            )
        )
        _items.update { current -> listOf(newItem) + current }
    }

    fun updateItemStatus(itemId: String, newStatus: ItemStatus, notes: String? = null) {
        _items.update { current ->
            current.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = newStatus,
                        notes = notes ?: item.notes
                    )
                } else item
            }
        }
    }

    fun recordDelivery(itemId: String, recipientName: String, notes: String) {
        val timestamp = dateFormat.format(Date())
        _items.update { current ->
            current.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = ItemStatus.COMPLETED,
                        recipientName = recipientName.ifBlank { "Receptor Autorizado" },
                        deliveryTimestamp = timestamp,
                        notes = if (notes.isNotBlank()) "${item.notes} | Entrega: $notes" else item.notes
                    )
                } else item
            }
        }
    }

    fun updateProductQuantity(itemId: String, productCode: String, linenum: Int, substitute: String, newActualQty: Int) {
        _items.update { current ->
            current.map { item ->
                if (item.id == itemId) {
                    val updatedProducts = item.products.map { prod ->
                        if (prod.code.equals(productCode, ignoreCase = true) &&
                            prod.linenum == linenum &&
                            prod.substitute.equals(substitute, ignoreCase = true)) {
                            val maxLimit = if (prod.actualQty == 0) prod.actualQty else prod.expectedQty
                            val clamped = newActualQty.coerceIn(0, maxLimit)
                            prod.copy(delivrdqty = clamped)
                        } else prod
                    }
                    item.copy(products = updatedProducts)
                } else item
            }
        }
    }

    fun scanBarcodeAndIncrement(itemId: String, productCode: String, linenum: Int = 0, substitute: String = "") {
        _items.update { current ->
            current.map { item ->
                if (item.id == itemId) {
                    var found = false
                    val updatedProducts = item.products.map { prod ->
                        if (prod.code.equals(productCode, ignoreCase = true) &&
                            (linenum == 0 || prod.linenum == linenum) &&
                            prod.substitute.equals(substitute, ignoreCase = true)) {
                            found = true
                            prod.copy(actualQty = prod.delivrdqty + 1)
                        } else prod
                    }
                    val finalProducts = if (!found) {
                        updatedProducts + ProductItem(code = productCode.uppercase(), name = "Producto Escaneado ($productCode)", expectedQty = 1, actualQty = 1, linenum = linenum, substitute = substitute)
                    } else {
                        updatedProducts
                    }
                    item.copy(products = finalProducts)
                } else item
            }
        }
    }
}
