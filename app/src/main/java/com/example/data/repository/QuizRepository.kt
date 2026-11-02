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

    val allQuestions: Flow<List<QuizItem>> = quizDao.getAllQuestions()

    suspend fun getQuestionCount(): Int = quizDao.getQuestionCount()

    suspend fun getDueQuestionCount(currentTimestamp: Long): Int =
        quizDao.getDueQuestionCount(currentTimestamp)

    suspend fun update(item: QuizItem) = quizDao.updateQuizItem(item)

    suspend fun insert(item: QuizItem): Long = quizDao.insertQuizItem(item)

    suspend fun insertAll(items: List<QuizItem>) = quizDao.insertQuizItems(items)

    suspend fun delete(item: QuizItem) = quizDao.deleteQuizItem(item)

    suspend fun deleteAll() = quizDao.deleteAllQuizItems()

    suspend fun resetAll(currentTimestamp: Long) = quizDao.resetAllProgress(currentTimestamp)
}
