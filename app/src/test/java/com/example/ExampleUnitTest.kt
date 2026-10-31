package com.example

import com.example.data.model.TransactionEntity
import com.example.util.ExcelExporter
import com.example.util.ShamsiDateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun shamsi_conversion_isCorrect() {
        // March 20, 2024 is Farvardin 1, 1403
        val shamsi = ShamsiDateHelper.gregorianToShamsi(2024, 3, 20)
        assertEquals(1403, shamsi.year)
        assertEquals(1, shamsi.month)
        assertEquals(1, shamsi.day)
    }

    @Test
    fun persian_digits_formatting() {
        val result = ShamsiDateHelper.toPersianDigits("123450")
        assertEquals("۱۲۳۴۵۰", result)
    }

    @Test
    fun excel_csv_generation_hasUtf8Bom() {
        val sampleList = listOf(
            TransactionEntity(
                id = 1,
                title = "خرید سوپرمارکت",
                amount = 250000,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "خوراک",
                categoryIcon = "Restaurant",
                categoryColor = 0xFFF97316,
                timestamp = System.currentTimeMillis(),
                shamsiDate = "1403/08/10",
                paymentMethod = "کارت بانکی",
                note = "خرید میوه"
            )
        )
        val csv = ExcelExporter.generateCsvContent(sampleList, "تومان")
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("خرید سوپرمارکت"))
        assertTrue(csv.contains("۲۵۰۰۰۰") || csv.contains("250000"))
    }
}
