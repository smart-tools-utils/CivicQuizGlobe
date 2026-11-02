package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QuizItem
import kotlinx.coroutines.flow.Flow

/**
 * واجهة التعامل مع قاعدة بيانات الأسئلة والبطاقات (Data Access Object)
 */
@Dao
interface QuizDao {

    /**
     * جلب الأسئلة التي حان موعد مراجعتها اليوم أو المتأخرة
     * (حيث يكون nextReviewDate أقل من أو يساوي الوقت الحالي)
     */
    @Query("SELECT * FROM quiz_items WHERE nextReviewDate <= :currentTimestamp ORDER BY nextReviewDate ASC")
    fun getDueQuestions(currentTimestamp: Long): Flow<List<QuizItem>>

    /**
     * جلب جميع الأسئلة المسجلة في التطبيق للمكتبة وقائمة البطاقات
     */
    @Query("SELECT * FROM quiz_items ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<QuizItem>>

    /**
     * عدد الأسئلة الإجمالي المسجل في قاعدة البيانات
     */
    @Query("SELECT COUNT(*) FROM quiz_items")
    suspend fun getQuestionCount(): Int

    /**
     * عدد الأسئلة المستحقة للمراجعة الآن (لـ Worker والإشعارات)
     */
    @Query("SELECT COUNT(*) FROM quiz_items WHERE nextReviewDate <= :currentTimestamp")
    suspend fun getDueQuestionCount(currentTimestamp: Long): Int

    /**
     * تحديث بيانات السؤال بعد تطبيق خوارزمية SM-2
     */
    @Update
    suspend fun updateQuizItem(item: QuizItem)

    /**
     * إدراج سؤال جديد يدوياً أو مولد بالذكاء الاصطناعي
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizItem(item: QuizItem): Long

    /**
     * إدراج مجموعة أسئلة دفعة واحدة (عند التهيئة الأولية من questions.json)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizItems(items: List<QuizItem>)

    /**
     * حذف سؤال من قاعدة البيانات
     */
    @Delete
    suspend fun deleteQuizItem(item: QuizItem)

    /**
     * حذف جميع الأسئلة المسجلة في التطبيق (عند استبدال الأسئلة بحزمة جديدة)
     */
    @Query("DELETE FROM quiz_items")
    suspend fun deleteAllQuizItems()

    /**
     * إعادة تعيين تقدم التعلم لجميع الأسئلة
     */
    @Query("UPDATE quiz_items SET interval = 0, repetition = 0, easeFactor = 2.5, nextReviewDate = :currentTimestamp")
    suspend fun resetAllProgress(currentTimestamp: Long)
}
