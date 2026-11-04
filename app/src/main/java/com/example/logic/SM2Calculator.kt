package com.example.logic

import com.example.data.model.QuizItem
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * درجات تقييم الإجابة في خوارزمية SM-2
 */
enum class ReviewQuality(val grade: Int, val labelAr: String, val labelEn: String, val labelSv: String) {
    BLACKOUT(0, "فوات الوقت (نسيان تام)", "Timeout (Blackout)", "Tidsgräns nådd"),
    AGAIN(1, "إعادة (غير صحيح)", "Again (Incorrect)", "Igen (Fel svar)"),
    HARD(2, "صعب (خطأ بعد تفكير)", "Hard (Struggled)", "Svårt (Fel efter tvekan)"),
    PASS_HARD(3, "صحيح بعد تردد طويل", "Pass (Hesitant)", "Rätt efter tvekan"),
    GOOD(4, "جيد (تذكر سليم)", "Good (Recalled)", "Bra (Säker)"),
    EASY(5, "سهل جداً (استدعاء فوري)", "Easy (Instant)", "Lätt (Direkt)")
}

/**
 * خوارزمية التكرار المتباعد SuperMemo-2 المدمجة مع وزن الوقت (Time-Weighted SM-2)
 */
object SM2Calculator {

    private const val MIN_EASE_FACTOR = 1.3f
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

    /**
     * استنتاج درجة الجودة (quality من 0 إلى 5) تلقائياً استناداً إلى صحة الإجابة وسرعة الاستحضار:
     *
     * 1. فوات الوقت (isTimedOut): quality = 0 (فشل تام في الاستدعاء).
     * 2. إجابة خاطئة: quality = 1 (استعجال) أو 2 (تفكير ثم خطأ).
     * 3. إجابة صحيحة:
     *    - في أول 35% من الوقت المحدد: quality = 5 (استدعاء تلقائي فوري وسهل).
     *    - بين 35% و 70%: quality = 4 (تذكر طبيعي جيد).
     *    - أكثر من 70% من الوقت المتاح: quality = 3 (صحيح ولكن بتردد وصعوبة).
     */
    fun deriveQualityFromTime(
        isCorrect: Boolean,
        isTimedOut: Boolean,
        timeTakenMillis: Long,
        timeLimitMillis: Long
    ): Int {
        if (isTimedOut) return 0

        if (!isCorrect) {
            return if (timeLimitMillis > 0 && timeTakenMillis > (timeLimitMillis * 0.5f)) 2 else 1
        }

        // إذا كان المؤقت معطلاً (timeLimit <= 0) نعتبر الاستدعاء جيداً كمعيار
        if (timeLimitMillis <= 0L) return 4

        val ratio = (timeTakenMillis.toFloat() / timeLimitMillis.toFloat()).coerceIn(0f, 1f)
        return when {
            ratio <= 0.35f -> 5 // سهل جداً (استدعاء سريع وفوري)
            ratio <= 0.70f -> 4 // تذكر جيد
            else -> 3           // تذكر مع صعوبة وتردد في آخر الثواني
        }
    }

    /**
     * حساب SM-2 التقليدي بناءً على درجة quality صريحة (0..5).
     */
    fun calculateSM2(
        item: QuizItem,
        quality: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): QuizItem {
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
            // إجابة غير موفقة أو فوات الوقت -> إعادة الدورة للصفر
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

    /**
     * حساب SM-2 المدمج مع الزمن المستغرق والمؤقت.
     * يُرجع كائن السؤال المحدث مع درجة الجودة المحسوبة (quality).
     */
    fun calculateSM2WithTime(
        item: QuizItem,
        isCorrect: Boolean,
        isTimedOut: Boolean,
        timeTakenMillis: Long,
        timeLimitMillis: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Pair<QuizItem, Int> {
        val quality = deriveQualityFromTime(isCorrect, isTimedOut, timeTakenMillis, timeLimitMillis)
        val updatedItem = calculateSM2(item, quality, currentTimeMillis)
        return Pair(updatedItem, quality)
    }
}
