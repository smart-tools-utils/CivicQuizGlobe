package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker للتحقق يومياً في الخلفية من وجود أسئلة مستحقة للمراجعة اليوم،
 * وإرسال إشعار محلي (Local Notification) عند وجود بطاقات مجدولة.
 */
class ReviewReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "بدء فحص الأسئلة المستحقة للمراجعة في الخلفية...")
            val database = AppDatabase.getInstance(context)
            val currentTimestamp = System.currentTimeMillis()
            val dueCount = database.quizDao().getDueQuestionCount(currentTimestamp)

            Log.d(TAG, "عدد الأسئلة المستحقة اليوم: $dueCount")

            if (dueCount > 0) {
                showReviewNotification(dueCount)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء تنفيذ ReviewReminderWorker", e)
            Result.retry()
        }
    }

    private fun showReviewNotification(dueCount: Int) {
        createNotificationChannel()

        // التحقق من صلاحية الإشعارات على أندرويد 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "صلاحية الإشعارات غير ممنوحة.")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_title)
        val contentText = "لديك $dueCount سؤال مستحق للمراجعة اليوم حسب خوارزمية SM-2."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "تم إرسال إشعار المراجعة بنجاح.")
        } catch (e: SecurityException) {
            Log.e(TAG, "تم رفض إرسال الإشعار بسبب قيود الأمان", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "ReviewReminderWorker"
        const val CHANNEL_ID = "quiz_review_reminders_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
