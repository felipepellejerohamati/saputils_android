package com.serviall.saputils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.serviall.saputils.data.ItemStatus
import com.serviall.saputils.data.ItemStatusAdapter
import com.serviall.saputils.data.ProductItem
import com.serviall.saputils.data.ProductItemDeserializer
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.data.ServialItemDeserializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ServialItemSerializationTest {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ItemStatus::class.java, ItemStatusAdapter())
        .registerTypeAdapter(ProductItem::class.java, ProductItemDeserializer())
        .registerTypeAdapter(ServialItem::class.java, ServialItemDeserializer())
        .create()

    @Test
    fun testCopyWhenStatusIsMissingInJson() {
        val json = """
            {
                "number": "1001",
                "client": "Cliente Test",
                "statustext": "Pendiente"
            }
        """.trimIndent()

        val item = gson.fromJson(json, ServialItem::class.java)
        assertNotNull(item)
        assertEquals("1001", item.number)

        // Calling .copy(...) should not throw NullPointerException
        val copiedItem = item.copy(number = "1002")
        assertEquals("1002", copiedItem.number)
        assertEquals(ItemStatus.PENDING, copiedItem.status ?: ItemStatus.PENDING)
    }

    @Test
    fun testCopyWhenStatusIsNullInJson() {
        val json = """
            {
                "number": "2001",
                "status": null,
                "client": "Empresa S.A."
            }
        """.trimIndent()

        val item = gson.fromJson(json, ServialItem::class.java)
        assertNotNull(item)

        // Calling .copy(...) should not throw NullPointerException
        val copiedItem = item.copy(number = "2002")
        assertEquals("2002", copiedItem.number)
    }

    @Test
    fun testCopyWhenStatusIsCustomStringInJson() {
        val json = """
            {
                "number": "3001",
                "status": "Entregado",
                "products": [
                    { "code": "PRD-1", "expectedQty": 5 }
                ]
            }
        """.trimIndent()

        val item = gson.fromJson(json, ServialItem::class.java)
        assertNotNull(item)
        assertEquals("Entregado", item.status?.label)

        val updatedProducts = item.products.map { it.copy(delivrdqty = 5) }
        val copiedItem = item.copy(products = updatedProducts)
        assertEquals(5, copiedItem.products.first().delivrdqty)
    }
}
