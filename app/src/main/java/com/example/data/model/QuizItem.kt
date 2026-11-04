package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a quiz item / flashcard with multi-deck categorization,
 * bilingual question support, and SuperMemo-2 (SM-2) parameters.
 */
@Entity(tableName = "quiz_items")
data class QuizItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val questionSv: String = "",
    val questionAr: String = "",
    val questionEn: String = "",
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = "",
    val deckName: String = "Medborgarskap", // اسم الحزمة / التصنيف
    // حقول خوارزمية SM-2
    val interval: Int = 0,         // الفاصل الزمني بالأيام
    val repetition: Int = 0,       // عدد التكرارات الناجحة المتتالية
    val easeFactor: Float = 2.5f,  // معامل السهولة (يبدأ بـ 2.5 كمعيار SM-2)
    val nextReviewDate: Long = 0L  // تاريخ المراجعة القادمة كطابع زمني (Timestamp)
) {
    /**
     * إرجاع نص السؤال الأنسب وفقاً لكود لغة التطبيق الحالية مع الحفاظ على النص الأصلي كاحتياط
     */
    fun getLocalizedQuestion(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "ar" -> when {
                questionAr.isNotBlank() -> questionAr
                question.isNotBlank() -> question
                else -> questionSv
            }
            "sv" -> when {
                questionSv.isNotBlank() -> questionSv
                question.isNotBlank() -> question
                else -> questionAr
            }
            "en" -> when {
                questionEn.isNotBlank() -> questionEn
                question.isNotBlank() -> question
                else -> questionSv
            }
            else -> question.ifBlank { questionSv }
        }
    }
}
