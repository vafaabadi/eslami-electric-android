package com.eslamielectric.android.core.data

import androidx.test.core.app.ApplicationProvider
import com.eslamielectric.android.core.network.BasketDraftItemDto
import com.eslamielectric.android.core.network.ProductDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BasketRepositoryTest {

    private lateinit var repository: BasketRepository

    private val sampleProduct = ProductDto(
        id = "prod-1",
        name = "LED Bulb",
        nameFa = "لامپ",
        price = 9.99,
        imageUrl = "/images/bulb.jpg",
        categoryId = "cat-1"
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository = BasketRepository(context)
        repository.clear()
    }

    @Test
    fun addProduct_createsNewLineItem() = runTest {
        repository.addProduct(sampleProduct, categoryId = "cat-1", quantity = 2)
        val items = repository.getItemsOnce()
        assertEquals(1, items.size)
        assertEquals("prod-1", items[0].id)
        assertEquals(2, items[0].quantity)
        assertEquals(9.99, items[0].price, 0.001)
    }

    @Test
    fun addProduct_mergesQuantityForSameProduct() = runTest {
        repository.addProduct(sampleProduct, categoryId = null, quantity = 1)
        repository.addProduct(sampleProduct, categoryId = null, quantity = 3)
        val items = repository.getItemsOnce()
        assertEquals(1, items.size)
        assertEquals(4, items[0].quantity)
    }

    @Test
    fun updateQuantity_changesQtyAndRemovesWhenZero() = runTest {
        repository.addProduct(sampleProduct, categoryId = null)
        repository.updateQuantity("prod-1", 5)
        assertEquals(5, repository.getItemsOnce().single().quantity)
        repository.updateQuantity("prod-1", 0)
        assertTrue(repository.getItemsOnce().isEmpty())
    }

    @Test
    fun removeItem_dropsMatchingProduct() = runTest {
        repository.addProduct(sampleProduct, categoryId = null)
        repository.removeItem("prod-1")
        assertTrue(repository.getItemsOnce().isEmpty())
    }

    @Test
    fun loadFromDraft_mapsDraftItemsAndCoercesQuantity() = runTest {
        repository.loadFromDraft(
            listOf(
                BasketDraftItemDto(
                    id = "draft-id",
                    name = "Cable",
                    nameFa = "کابل",
                    price = 4.5,
                    quantity = 0,
                    imageUrl = "/img/cable.jpg",
                    categoryId = "cat-2"
                ),
                BasketDraftItemDto(
                    id = null,
                    name = "Switch",
                    price = 2.0,
                    quantity = 2
                )
            )
        )
        val items = repository.getItemsOnce()
        assertEquals(2, items.size)
        assertEquals("draft-id", items[0].id)
        assertEquals(1, items[0].quantity)
        assertTrue(items[1].id.startsWith("draft-"))
        assertEquals(2, items[1].quantity)
    }

    @Test
    fun itemCountAndSubtotal_aggregateLineTotals() = runTest {
        repository.setItems(
            listOf(
                BasketItem(id = "a", name = "A", price = 10.0, quantity = 2),
                BasketItem(id = "b", name = "B", price = 3.5, quantity = 1)
            )
        )
        val items = repository.getItemsOnce()
        assertEquals(3, repository.itemCount(items))
        assertEquals(23.5, repository.subtotal(items), 0.001)
    }

    @Test
    fun toCheckoutLineItems_usesFaNameWhenLocaleFa() = runTest {
        repository.setItems(
            listOf(
                BasketItem(id = "a", name = "Bulb", nameFa = "لامپ", price = 5.0, quantity = 1)
            )
        )
        val en = repository.toCheckoutLineItems("en")
        val fa = repository.toCheckoutLineItems("fa")
        assertEquals("Bulb", en.single().name)
        assertEquals("لامپ", fa.single().name)
    }

    @Test
    fun persistsItemsAcrossRepositoryInstances() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository.addProduct(sampleProduct, categoryId = null, quantity = 2)
        val reloaded = BasketRepository(context)
        assertEquals(2, reloaded.getItemsOnce().single().quantity)
    }

    @Test
    fun toCheckoutLineItems_fallsBackWhenFaNameBlank() = runTest {
        repository.setItems(
            listOf(
                BasketItem(id = "a", name = "Bulb", nameFa = "  ", price = 5.0, quantity = 1)
            )
        )
        val fa = repository.toCheckoutLineItems("fa")
        assertEquals("Bulb", fa.single().name)
    }

    @Test
    fun toCheckoutLineItems_usesItemPlaceholderForBlankName() = runTest {
        repository.setItems(
            listOf(
                BasketItem(id = "a", name = "  ", price = 5.0, quantity = 1)
            )
        )
        assertEquals("Item", repository.toCheckoutLineItems("en").single().name)
    }

    @Test
    fun clear_removesAllItems() = runTest {
        repository.addProduct(sampleProduct, categoryId = null)
        repository.clear()
        assertTrue(repository.getItemsOnce().isEmpty())
    }
}
