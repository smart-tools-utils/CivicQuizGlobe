package com.example.data.assets

import android.content.Context
import android.util.Log
import com.example.data.model.QuizItem
import com.example.data.repository.QuizRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * تهيئة البيانات عند أول تشغيل (First Launch Ingestion)
 * تقرأ ملف questions.json من مجلد assets وتدرجه في Room فقط إذا كانت قاعدة البيانات فارغة.
 */
object AssetIngestion {

    private const val TAG = "AssetIngestion"
    private const val ASSET_FILE = "questions.json"

    suspend fun ingestQuestionsIfEmpty(
        context: Context,
        repository: QuizRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val count = repository.getQuestionCount()
            if (count > 0) {
                Log.d(TAG, "قاعدة البيانات تحتوي بالفعل على $count سؤال، تم تخطي التهيئة.")
                return@withContext
            }

            Log.d(TAG, "قاعدة البيانات فارغة، جاري قراءة $ASSET_FILE من مجلد assets...")
            val jsonString = readAssetFile(context, ASSET_FILE)
            val quizItems = parseQuestionsJson(jsonString)

            if (quizItems.isNotEmpty()) {
                repository.insertAll(quizItems)
                Log.d(TAG, "تم إدخال ${quizItems.size} سؤال بنجاح في قاعدة البيانات.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء قراءة أو إدخال أسئلة assets", e)
        }
    }

    fun readAssetFile(context: Context, filename: String): String {
        return context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
    }

    fun parseQuestionsJson(jsonString: String): List<QuizItem> {
        val list = mutableListOf<QuizItem>()
        val jsonArray = JSONArray(jsonString)
        val currentTime = System.currentTimeMillis()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val question = obj.getString("question")
            val optionsJson = obj.getJSONArray("options")
            val options = mutableListOf<String>()
            for (j in 0 until optionsJson.length()) {
                options.add(optionsJson.getString(j))
            }
            val correctIndex = if (obj.has("correct_index")) {
                obj.getInt("correct_index")
            } else {
                obj.optInt("correctIndex", 0)
            }
            val explanation = obj.optString("explanation", "")

            // تجهيز السؤال بقيم SM-2 الأولية للمراجعة الفورية
            list.add(
                QuizItem(
                    id = 0, // Room auto-generates id
                    question = question,
                    options = options,
                    correctIndex = correctIndex,
                    explanation = explanation,
                    interval = 0,
                    repetition = 0,
                    easeFactor = 2.5f,
                    nextReviewDate = currentTime // مستحق للمراجعة فوراً
                )
            )
        }
        return list
    }
}
