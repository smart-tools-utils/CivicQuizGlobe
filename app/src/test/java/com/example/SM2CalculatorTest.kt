package com.example

import com.example.data.db.Converters
import com.example.data.model.QuizItem
import com.example.logic.SM2Calculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SM2CalculatorTest {

    private val baseItem = QuizItem(
        id = 1,
        question = "ما هو القانون الأسمى للأمة في الولايات المتحدة؟",
        options = listOf("إعلان الاستقلال", "الدستور الأمريكي", "وثيقة الحقوق"),
        correctIndex = 1,
        explanation = "الدستور الأمريكي",
        interval = 0,
        repetition = 0,
        easeFactor = 2.5f,
        nextReviewDate = 0L
    )

    @Test
    fun testFirstSuccessfulReview() {
        val result = SM2Calculator.calculateSM2(baseItem, quality = 5, currentTimeMillis = 1000L)
        assertEquals(1, result.interval)
        assertEquals(1, result.repetition)
        assertTrue(result.easeFactor >= 2.5f)
        assertTrue(result.nextReviewDate > 1000L)
    }

    @Test
    fun testSecondSuccessfulReview() {
        val firstReview = SM2Calculator.calculateSM2(baseItem, quality = 4, currentTimeMillis = 1000L)
        val secondReview = SM2Calculator.calculateSM2(firstReview, quality = 4, currentTimeMillis = 2000L)
        assertEquals(6, secondReview.interval)
        assertEquals(2, secondReview.repetition)
    }

    @Test
    fun testThirdSuccessfulReviewMultipliesInterval() {
        val item = baseItem.copy(repetition = 2, interval = 6, easeFactor = 2.5f)
        val result = SM2Calculator.calculateSM2(item, quality = 4, currentTimeMillis = 1000L)
        assertEquals(3, result.repetition)
        // 6 * 2.5 = 15 days
        assertEquals(15, result.interval)
    }

    @Test
    fun testFailureResetsRepetitionAndInterval() {
        val masteredItem = baseItem.copy(repetition = 5, interval = 30, easeFactor = 2.6f)
        val result = SM2Calculator.calculateSM2(masteredItem, quality = 1, currentTimeMillis = 1000L)
        assertEquals(0, result.repetition)
        assertEquals(1, result.interval)
    }

    @Test
    fun testEaseFactorNeverDropsBelowMinimum() {
        var item = baseItem.copy(easeFactor = 1.35f)
        item = SM2Calculator.calculateSM2(item, quality = 0)
        assertTrue(item.easeFactor >= 1.3f)
    }

    @Test
    fun testConvertersSerialization() {
        val converters = Converters()
        val originalList = listOf("خيار 1", "خيار 2", "Option 3 with , comma and \" quotes")
        val json = converters.fromStringList(originalList)
        val restoredList = converters.toStringList(json)
        assertEquals(originalList, restoredList)
    }
}
