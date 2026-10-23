package com.serviall.saputils

import com.serviall.saputils.ui.viewmodel.ServialViewModel
import org.junit.Assert.*
import org.junit.Test

class ServialViewModelTest {

    @Test
    fun testActiveWarehouseSelection() {
        val viewModel = ServialViewModel()
        assertEquals("Bodega Central", viewModel.activeWarehouse.value)

        viewModel.setWarehouse("Bodega Norte")
        assertEquals("Bodega Norte", viewModel.activeWarehouse.value)
    }

    @Test
    fun testProductQuantityUpdateAndBarcodeScanning() {
        val viewModel = ServialViewModel()
        val item = viewModel.items.value.first()
        val initialProduct = item.products.first()
        val initialQty = initialProduct.qttytrans

        // Update product quantity directly
        viewModel.updateProductQuantity(item.id, initialProduct.code, initialProduct.linenum, initialProduct.substitute, initialQty + 2)
        val updatedItem = viewModel.items.value.first { it.id == item.id }
        val updatedProduct = updatedItem.products.first { it.code == initialProduct.code }
        assertEquals(initialQty + 2, updatedProduct.qttytrans)

        // Test barcode scan and increment
        viewModel.scanBarcodeAndIncrement(item.id, initialProduct.code, initialProduct.linenum, initialProduct.substitute)
        val scannedItem = viewModel.items.value.first { it.id == item.id }
        val scannedProduct = scannedItem.products.first { it.code == initialProduct.code }
        assertEquals(initialQty + 3, scannedProduct.qttytrans)

        // Test scanning a new barcode (adds new product item)
        viewModel.scanBarcodeAndIncrement(item.id, "PRD-NEW-999")
        val newProductItem = viewModel.items.value.first { it.id == item.id }.products.firstOrNull { it.code == "PRD-NEW-999" }
        assertNotNull(newProductItem)
        assertEquals(1, newProductItem?.qttytrans)
    }
}
