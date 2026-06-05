package com.eslamielectric.android.util

import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleExtensionsTest {

    private val product = ProductDto(
        id = "p1",
        name = "Cable",
        nameFa = "کابل",
        description = "Copper cable",
        descriptionFa = "کابل مس",
        price = 9.99,
        category = "Wiring",
        categoryFa = "سیم‌کشی",
        imageAltEn = "Cable photo",
        imageAltFa = "عکس کابل"
    )

    @Test
    fun displayNameUsesFaWhenLocaleFa() {
        assertEquals("کابل", product.displayName("fa"))
        assertEquals("Cable", product.displayName("en"))
    }

    @Test
    fun displayNameFallsBackToEnWhenFaBlank() {
        val p = product.copy(nameFa = "")
        assertEquals("Cable", p.displayName("fa"))
    }

    @Test
    fun displayCategoryUsesFaWhenAvailable() {
        assertEquals("سیم‌کشی", product.displayCategory("fa"))
        assertEquals("Wiring", product.displayCategory("en"))
    }

    @Test
    fun imageContentDescriptionLocalized() {
        assertEquals("عکس کابل", product.imageContentDescription("fa"))
        assertEquals("Cable photo", product.imageContentDescription("en"))
    }

    @Test
    fun categoryDisplayNameLocalized() {
        val category = CategoryDto(id = "c1", name = "Lighting", nameFa = "روشنایی")
        assertEquals("روشنایی", category.displayName("fa"))
        assertEquals("Lighting", category.displayName("en"))
    }
}
