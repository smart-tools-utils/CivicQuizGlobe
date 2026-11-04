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
 * تدعم الحزم المتعددة (Multi-Decks)، والجلسات المرنة الذكية (Smart Random Batching).
 */
@Dao
interface QuizDao {

    /**
     * جلب الأسئلة المستحقة للمراجعة حالياً (خوارزمية SM-2 الاعتيادية)
     */
    @Query("SELECT * FROM quiz_items WHERE nextReviewDate <= :currentTimestamp ORDER BY nextReviewDate ASC")
    fun getDueQuestions(currentTimestamp: Long): Flow<List<QuizItem>>

    /**
     * جلب الأسئلة المستحقة بحسب الحزمة (Deck)
     */
    @Query("SELECT * FROM quiz_items WHERE (:deckName IS NULL OR :deckName = '' OR deckName = :deckName) AND nextReviewDate <= :currentTimestamp ORDER BY nextReviewDate ASC")
    fun getDueQuestionsByDeck(currentTimestamp: Long, deckName: String?): Flow<List<QuizItem>>

    /**
     * استعلام الجلسة المرنة الذكية (Smart On-Demand Batching):
     * يتجاوز قيد الـ 24 ساعة وينتقي حزمة مرنة من :limit سؤال.
     * الأولوية:
     * 1. الأسئلة المستحقة زمنياً (nextReviewDate <= currentTimestamp)
     * 2. الأسئلة الجديدة التي لم تُراجع بعد (repetition = 0)
     * 3. الأسئلة الصعبة ذات معامل السهولة المنخفض (easeFactor ASC)
     * ثم خلط الحزمة الناتجة عشوائياً بـ ORDER BY RANDOM().
     */
    @Query("""
        SELECT * FROM (
            SELECT * FROM quiz_items
            WHERE (:deckName IS NULL OR :deckName = '' OR deckName = :deckName)
            ORDER BY 
                CASE 
                    WHEN nextReviewDate <= :currentTimestamp THEN 0
                    WHEN repetition = 0 THEN 1
                    ELSE 2
                END ASC,
                easeFactor ASC,
                nextReviewDate ASC
            LIMIT :limit
        )
        ORDER BY RANDOM()
    """)
    suspend fun getSmartSessionBatch(
        currentTimestamp: Long,
        limit: Int,
        deckName: String? = null
    ): List<QuizItem>

    /**
     * جلب جميع الأسئلة المسجلة في التطبيق
     */
    @Query("SELECT * FROM quiz_items ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<QuizItem>>

    /**
     * جلب الأسئلة التابعة لحزمة محددة
     */
    @Query("SELECT * FROM quiz_items WHERE deckName = :deckName ORDER BY id ASC")
    fun getQuestionsByDeck(deckName: String): Flow<List<QuizItem>>

    /**
     * جلب قائمة الحزم المسجلة فريدة
     */
    @Query("SELECT DISTINCT deckName FROM quiz_items WHERE deckName IS NOT NULL AND deckName != '' ORDER BY deckName ASC")
    fun getDistinctDecks(): Flow<List<String>>

    /**
     * عدد الأسئلة الإجمالي المسجل في قاعدة البيانات
     */
    @Query("SELECT COUNT(*) FROM quiz_items")
    suspend fun getQuestionCount(): Int

    /**
     * عدد الأسئلة المسجلة لحزمة محددة
     */
    @Query("SELECT COUNT(*) FROM quiz_items WHERE (:deckName IS NULL OR :deckName = '' OR deckName = :deckName)")
    suspend fun getQuestionCountByDeck(deckName: String?): Int

    /**
     * عدد الأسئلة المستحقة للمراجعة الآن
     */
    @Query("SELECT COUNT(*) FROM quiz_items WHERE nextReviewDate <= :currentTimestamp")
    suspend fun getDueQuestionCount(currentTimestamp: Long): Int

    /**
     * تحديث بيانات السؤال بعد تطبيق خوارزمية SM-2
     */
    @Update
    suspend fun updateQuizItem(item: QuizItem)

    /**
     * إدراج سؤال جديد
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizItem(item: QuizItem): Long

    /**
     * إدراج مجموعة أسئلة دفعة واحدة
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizItems(items: List<QuizItem>)

    /**
     * حذف سؤال
     */
    @Delete
    suspend fun deleteQuizItem(item: QuizItem)

    /**
     * حذف جميع أسئلة حزمة محددة
     */
    @Query("DELETE FROM quiz_items WHERE deckName = :deckName")
    suspend fun deleteDeck(deckName: String)

    /**
     * حذف جميع الأسئلة
     */
    @Query("DELETE FROM quiz_items")
    suspend fun deleteAllQuizItems()

    /**
     * إعادة تعيين تقدم التعلم لجميع الأسئلة
     */
    @Query("UPDATE quiz_items SET interval = 0, repetition = 0, easeFactor = 2.5, nextReviewDate = :currentTimestamp")
    suspend fun resetAllProgress(currentTimestamp: Long)
}
