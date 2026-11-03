package com.serviall.saputils.data

import java.util.UUID

data class ReconocerItem(
    val id: String = UUID.randomUUID().toString(),
    val docNum: String = "4904500131",
    val docItem: String = "0005",
    val materialCode: String,
    val description: String,
    val quantity: String,
    val reserva: String = "", // Campo Reserva añadido para el segundo formato
    val sku: String = "" // Atributo en blanco solicitado
)
