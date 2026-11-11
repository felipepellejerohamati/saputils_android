package com.serviall.saputils.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type
import java.util.Locale
import java.util.UUID

@JsonAdapter(ItemStatusAdapter::class)
enum class ItemStatus(val label: String) {
    PENDING("Pendiente"),
    RECEIVED("Recibido"),
    DELIVERING("En Reparto"),
    COMPLETED("Entregado"),
    PARCIAL("Parcial"),
    PENDIENTE("Pendiente"),
    ENTREGADO("Entregado")
}

class ItemStatusAdapter : TypeAdapter<ItemStatus>() {
    override fun write(out: JsonWriter, value: ItemStatus?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.name)
        }
    }

    override fun read(reader: JsonReader): ItemStatus {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return ItemStatus.PENDING
        }
        val str = reader.nextString()
        return parse(str)
    }

    companion object {
        fun parse(str: String?): ItemStatus {
            if (str.isNullOrBlank()) return ItemStatus.PENDING
            val trimmed = str.trim()
            return ItemStatus.entries.firstOrNull {
                it.name.equals(trimmed, ignoreCase = true) ||
                it.label.equals(trimmed, ignoreCase = true)
            } ?: when (trimmed.lowercase(Locale.ROOT)) {
                "pendiente", "pending", "abierto" -> ItemStatus.PENDIENTE
                "entregado", "completed", "cerrado" -> ItemStatus.ENTREGADO
                "parcial", "partial" -> ItemStatus.PARCIAL
                "recibido", "received" -> ItemStatus.RECEIVED
                "en reparto", "delivering" -> ItemStatus.DELIVERING
                else -> ItemStatus.PENDING
            }
        }
    }
}

@JsonAdapter(ProductItemDeserializer::class)
data class ProductItem(
    val code: String = "",
    val name: String = "",
    val substitute: String = "",
    val expectedQty: Int = 1,
    val actualQty: Int = 0,
    val delivrdqty: Int = 0,
    val linenum: Int = 0,
    val qttytrans: Int = 0
)

class ProductItemDeserializer : JsonDeserializer<ProductItem> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ProductItem {
        if (json == null || !json.isJsonObject) {
            return ProductItem()
        }
        val obj = json.asJsonObject

        fun getString(vararg keys: String): String {
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    return obj.get(key).asString ?: ""
                }
            }
            return ""
        }

        fun getInt(default: Int, vararg keys: String): Int {
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    return try { obj.get(key).asInt } catch (_: Exception) { default }
                }
            }
            return default
        }

        return ProductItem(
            code = getString("code", "Code", "ItemCode", "itemcode"),
            name = getString("name", "Name", "ItemName", "itemname", "Dscription", "dscription"),
            substitute = getString("substitute", "Substitute", "subcode"),
            expectedQty = getInt(1, "expectedQty", "ExpectedQty", "Quantity", "quantity"),
            actualQty = getInt(0, "actualQty", "ActualQty", "OnHand", "onhand", "Stock", "stock"),
            delivrdqty = getInt(0, "delivrdqty", "DelivrdQty", "DelivQty", "delivqty"),
            linenum = getInt(0, "linenum", "LineNum", "lineNum"),
            qttytrans = getInt(0, "qttytrans", "QttyTrans", "qttyTrans")
        )
    }
}

@JsonAdapter(ServialItemDeserializer::class)
data class ServialItem(
    val id: String = UUID.randomUUID().toString(),
    val number: String = "",
    val reference: String = "",
    val client: String = "",
    val status: ItemStatus? = ItemStatus.PENDING,
    val date: String = "",
    val notes: String = "",
    val recipientName: String? = null,
    val deliveryTimestamp: String? = null,
    val weightKg: Double = 1.5,
    val products: List<ProductItem> = emptyList(),
    val signatureBase64: String? = null,
    val deliveredBy: String? = null,
    val tipo: String = "Reserva",
    val completado: Double = 0.0, // Double for percentage with 2 decimal precision
    val statustext: String = "",
    val starttime: String = "", // Fecha y hora de inicio
    val endtime: String = ""    // Fecha y hora de fin
) {
    val effectiveStatus: ItemStatus
        get() = status ?: ItemStatusAdapter.parse(statustext)
}

class ServialItemDeserializer : JsonDeserializer<ServialItem> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ServialItem {
        if (json == null || !json.isJsonObject) {
            return ServialItem()
        }
        val obj = json.asJsonObject

        fun getString(vararg keys: String): String {
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    return obj.get(key).asString ?: ""
                }
            }
            return ""
        }

        fun getDouble(default: Double, vararg keys: String): Double {
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    return try { obj.get(key).asDouble } catch (_: Exception) { default }
                }
            }
            return default
        }

        val id = getString("id", "Id").ifBlank { UUID.randomUUID().toString() }
        val number = getString("number", "Number", "DocNum", "docnum")
        val reference = getString("reference", "Reference", "NumAtCard", "numatcard", "reserva", "DocNum").ifBlank { getString("number", "Number") }
        val client = getString("client", "Client", "CardName", "cardname", "CardCode", "cardcode")
        val date = getString("date", "Date", "DocDate", "docdate")
        val notes = getString("notes", "Notes", "Comments", "comments")
        val recipientName = getString("recipientName", "RecipientName").ifBlank { null }
        val deliveryTimestamp = getString("deliveryTimestamp", "DeliveryTimestamp").ifBlank { null }
        val weightKg = getDouble(1.5, "weightKg", "WeightKg", "weight")
        val signatureBase64 = getString("signatureBase64", "SignatureBase64").ifBlank { null }
        val deliveredBy = getString("deliveredBy", "DeliveredBy").ifBlank { null }
        val tipo = getString("tipo", "Tipo", "DocType").ifBlank { "Reserva" }
        val completado = getDouble(0.0, "completado", "Completado")
        val statustext = getString("statustext", "StatusText", "statustxt")
        val starttime = getString("starttime", "StartTime")
        val endtime = getString("endtime", "EndTime")

        // Parse products
        val productsList = mutableListOf<ProductItem>()
        val productsElement = obj.get("products") ?: obj.get("Products") ?: obj.get("items") ?: obj.get("Items") ?: obj.get("DocumentLines")
        if (productsElement != null && productsElement.isJsonArray) {
            val productDeserializer = ProductItemDeserializer()
            for (elem in productsElement.asJsonArray) {
                if (elem.isJsonObject) {
                    val prod = productDeserializer.deserialize(elem, ProductItem::class.java, context)
                    productsList.add(prod)
                }
            }
        }

        // Parse status
        val statusElement = obj.get("status") ?: obj.get("Status") ?: obj.get("DocStatus") ?: obj.get("docstatus")
        val statusStr = if (statusElement != null && !statusElement.isJsonNull) statusElement.asString else null
        val parsedStatus = ItemStatusAdapter.parse(statusStr)

        return ServialItem(
            id = id,
            number = number,
            reference = reference,
            client = client,
            status = parsedStatus,
            date = date,
            notes = notes,
            recipientName = recipientName,
            deliveryTimestamp = deliveryTimestamp,
            weightKg = weightKg,
            products = productsList,
            signatureBase64 = signatureBase64,
            deliveredBy = deliveredBy,
            tipo = tipo,
            completado = completado,
            statustext = statustext,
            starttime = starttime,
            endtime = endtime
        )
    }
}
