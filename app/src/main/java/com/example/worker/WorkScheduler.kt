package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * مساعد جدولة مهام WorkManager للتذكير اليومي
 */
object WorkScheduler {

    private const val TAG = "WorkScheduler"
    private const val UNIQUE_PERIODIC_WORK_NAME = "daily_quiz_review_check"
    private const val UNIQUE_ONE_TIME_WORK_NAME = "test_quiz_review_check"

    /**
     * جدولة الفحص اليومي كل 24 ساعة
     */
    fun scheduleDailyReviewReminder(context: Context) {
        try {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<ReviewReminderWorker>(
                24, TimeUnit.HOURS,
                6, TimeUnit.HOURS // Flex interval
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            Log.d(TAG, "تم تسجيل الجدولة اليومية لـ WorkManager بنجاح.")
        } catch (e: Exception) {
            Log.e(TAG, "فشل في تسجيل جدولة WorkManager", e)
        }
    }

    /**
     * تشغيل فحص فوري لغرض التجربة والاختبار المباشر من داخل التطبيق
     */
    fun triggerImmediateCheck(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "تم إرسال طلب الفحص الفوري لـ WorkManager.")
        } catch (e: Exception) {
            Log.e(TAG, "فشل في تشغيل الفحص الفوري", e)
        }
    }
}
