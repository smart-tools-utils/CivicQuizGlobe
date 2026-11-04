package com.example.data.repository

import com.example.data.db.QuizDao
import com.example.data.model.QuizItem
import kotlinx.coroutines.flow.Flow

/**
 * مستودع البيانات (Repository Pattern) للربط بين قاعدة بيانات Room وطبقة العرض (ViewModel)
 */
class QuizRepository(private val quizDao: QuizDao) {

    fun getDueQuestions(currentTimestamp: Long): Flow<List<QuizItem>> {
        return quizDao.getDueQuestions(currentTimestamp)
    }

    fun getDueQuestionsByDeck(currentTimestamp: Long, deckName: String?): Flow<List<QuizItem>> {
        return quizDao.getDueQuestionsByDeck(currentTimestamp, deckName)
    }

    suspend fun getSmartSessionBatch(
        currentTimestamp: Long,
        limit: Int,
        deckName: String? = null
    ): List<QuizItem> {
        return quizDao.getSmartSessionBatch(currentTimestamp, limit, deckName)
    }

    val allQuestions: Flow<List<QuizItem>> = quizDao.getAllQuestions()

    val distinctDecks: Flow<List<String>> = quizDao.getDistinctDecks()

    fun getQuestionsByDeck(deckName: String): Flow<List<QuizItem>> = quizDao.getQuestionsByDeck(deckName)

    suspend fun getQuestionCount(): Int = quizDao.getQuestionCount()

    suspend fun getQuestionCountByDeck(deckName: String?): Int = quizDao.getQuestionCountByDeck(deckName)

    suspend fun getDueQuestionCount(currentTimestamp: Long): Int =
        quizDao.getDueQuestionCount(currentTimestamp)

    suspend fun update(item: QuizItem) = quizDao.updateQuizItem(item)

    suspend fun insert(item: QuizItem): Long = quizDao.insertQuizItem(item)

    suspend fun insertAll(items: List<QuizItem>) = quizDao.insertQuizItems(items)

    suspend fun delete(item: QuizItem) = quizDao.deleteQuizItem(item)

    suspend fun deleteDeck(deckName: String) = quizDao.deleteDeck(deckName)

    suspend fun deleteAll() = quizDao.deleteAllQuizItems()

    suspend fun resetAll(currentTimestamp: Long) = quizDao.resetAllProgress(currentTimestamp)
}
