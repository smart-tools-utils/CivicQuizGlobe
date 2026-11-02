package com.example.logic

import com.example.data.model.QuizItem
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * درجات تقييم الإجابة في خوارزمية SM-2
 */
enum class ReviewQuality(val grade: Int, val labelAr: String, val labelEn: String) {
    AGAIN(1, "إعادة (صعب جداً)", "Again"),
    HARD(2, "صعب", "Hard"),
    GOOD(4, "جيد (متوسط)", "Good"),
    EASY(5, "سهل جداً", "Easy")
}

/**
 * خوارزمية التكرار المتباعد SuperMemo-2 (SM-2 Logic)
 * دالة برمجية نقية (Pure Function) بدون أي آثار جانبية.
 */
object SM2Calculator {

    private const val MIN_EASE_FACTOR = 1.3f
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

    /**
     * تحديث مقاييس SM-2 لكائن QuizItem بناءً على درجة تقييم المستخدم (quality من 0 إلى 5).
     *
     * معادلات SuperMemo-2:
     * 1. Ease Factor:
     *    EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     *    بحيث لا يقل EF' عن 1.3
     * 2. Interval (I):
     *    - إذا كانت q < 3 (فشل التذكر): repetition = 0, interval = 1
     *    - إذا كانت q >= 3 (نجاح التذكر):
     *        repetition 0 -> interval = 1 day
     *        repetition 1 -> interval = 6 days
     *        repetition >= 2 -> interval = round(I * EF)
     *        repetition' = repetition + 1
     */
    fun calculateSM2(
        item: QuizItem,
        quality: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): QuizItem {
        // حصر التقييم بين 0 و 5
        val q = quality.coerceIn(0, 5)

        val newRepetition: Int
        val newInterval: Int

        if (q >= 3) {
            // إجابة ناجحة
            when (item.repetition) {
                0 -> newInterval = 1
                1 -> newInterval = 6
                else -> {
                    val calculated = (item.interval * item.easeFactor).roundToInt()
                    newInterval = max(1, calculated)
                }
            }
            newRepetition = item.repetition + 1
        } else {
            // إجابة غير موفقة أو صعبة جداً -> إعادة الدورة
            newRepetition = 0
            newInterval = 1
        }

        // تحديث معامل السهولة (Ease Factor)
        val delta = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        val calculatedEf = item.easeFactor + delta
        val newEaseFactor = max(MIN_EASE_FACTOR, calculatedEf)

        // حساب تاريخ المراجعة القادمة
        val newNextReviewDate = currentTimeMillis + (newInterval.toLong() * ONE_DAY_MILLIS)

        return item.copy(
            interval = newInterval,
            repetition = newRepetition,
            easeFactor = newEaseFactor,
            nextReviewDate = newNextReviewDate
        )
    }
}
