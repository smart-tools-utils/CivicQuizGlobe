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
 * تهيئة واستيراد البيانات من ملفات JSON مع دعم الحزم المتعددة (Decks) والحقول ثنائية اللغة.
 */
object AssetIngestion {

    private const val TAG = "AssetIngestion"
    private const val DEFAULT_ASSET_FILE = "questions.json"

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

            Log.d(TAG, "قاعدة البيانات فارغة، جاري قراءة $DEFAULT_ASSET_FILE من مجلد assets...")
            val jsonString = readAssetFile(context, DEFAULT_ASSET_FILE)
            val quizItems = parseQuestionsJson(jsonString, defaultDeckName = "Medborgarskap")

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

    /**
     * تحليل مصفوفة JSON بمرونة فائقة:
     * تدعم الحقول: question, question_sv, question_ar, question_en, deck_name, correct_index, options, explanation
     */
    fun parseQuestionsJson(
        jsonString: String,
        defaultDeckName: String = "Medborgarskap"
    ): List<QuizItem> {
        val list = mutableListOf<QuizItem>()
        val jsonArray = JSONArray(jsonString.trim())
        val currentTime = System.currentTimeMillis()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val qSv = obj.optString("question_sv", "").trim()
            val qAr = obj.optString("question_ar", "").trim()
            val qEn = obj.optString("question_en", "").trim()
            val genericQ = obj.optString("question", "").trim()

            // اختيار السؤال الرئيسي المعياري
            val mainQuestion = when {
                genericQ.isNotBlank() -> genericQ
                qSv.isNotBlank() -> qSv
                qAr.isNotBlank() -> qAr
                else -> qEn
            }

            if (mainQuestion.isBlank()) continue

            val optionsJson = obj.optJSONArray("options") ?: JSONArray()
            val options = mutableListOf<String>()
            for (j in 0 until optionsJson.length()) {
                val opt = optionsJson.optString(j, "").trim()
                if (opt.isNotBlank()) {
                    options.add(opt)
                }
            }

            if (options.size < 2) continue

            val correctIndex = when {
                obj.has("correct_index") -> obj.getInt("correct_index")
                obj.has("correctIndex") -> obj.getInt("correctIndex")
                else -> 0
            }.coerceIn(0, options.size - 1)

            val explanation = obj.optString("explanation", "").trim()

            val deck = when {
                obj.has("deck_name") && obj.getString("deck_name").isNotBlank() -> obj.getString("deck_name").trim()
                obj.has("deckName") && obj.getString("deckName").isNotBlank() -> obj.getString("deckName").trim()
                defaultDeckName.isNotBlank() -> defaultDeckName.trim()
                else -> "Medborgarskap"
            }

            list.add(
                QuizItem(
                    id = 0,
                    question = mainQuestion,
                    questionSv = qSv,
                    questionAr = qAr,
                    questionEn = qEn,
                    options = options,
                    correctIndex = correctIndex,
                    explanation = explanation,
                    deckName = deck,
                    interval = 0,
                    repetition = 0,
                    easeFactor = 2.5f,
                    nextReviewDate = currentTime
                )
            )
        }
        return list
    }
}
