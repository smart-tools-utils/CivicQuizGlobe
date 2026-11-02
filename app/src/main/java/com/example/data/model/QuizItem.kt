package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل سؤال بطاقة الاختبار (Flashcard / Quiz Item)
 * مدمجاً مع حقول خوارزمية التكرار المتباعد SuperMemo-2 (SM-2).
 */
@Entity(tableName = "quiz_items")
data class QuizItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = "",
    // حقول خوارزمية SM-2
    val interval: Int = 0,         // الفاصل الزمني بالأيام
    val repetition: Int = 0,       // عدد التكرارات الناجحة المتتالية
    val easeFactor: Float = 2.5f,  // معامل السهولة (يبدأ بـ 2.5 كمعيار SM-2)
    val nextReviewDate: Long = 0L  // تاريخ المراجعة القادمة كطابع زمني (Timestamp)
)
