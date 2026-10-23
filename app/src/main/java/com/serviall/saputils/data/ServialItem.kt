package com.serviall.saputils.data

import java.util.UUID

enum class ItemStatus(val label: String) {
    PENDING("Pendiente"),
    RECEIVED("Recibido"),
    DELIVERING("En Reparto"),
    COMPLETED("Entregado")
}

data class ProductItem(
    val code: String = "",
    val name: String = "",
    val substitute: String = "",
    val expectedQty: Int = 1,
    val actualQty: Int = 0,
    val delivrdqty: Int=0,
    val linenum: Int = 0,
    val qttytrans : Int =0
)

data class ServialItem(
    val id: String = UUID.randomUUID().toString(),
    val number: String = "",
    val reference: String = "",
    val client: String = "",
    val status: ItemStatus = ItemStatus.PENDING,
    val date: String = "",
    val notes: String = "",
    val recipientName: String? = null,
    val deliveryTimestamp: String? = null,
    val weightKg: Double = 1.5,
    val products: List<ProductItem> = emptyList(),
    val signatureBase64: String? = null,
    val deliveredBy: String? = null
)
