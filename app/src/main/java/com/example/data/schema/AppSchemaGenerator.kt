package com.example.data.schema

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Generator and exporter for the standardized AI JSON Schema.
 * Designed to be copied to the clipboard for Google AI Studio Structured Prompt Studio.
 */
object AppSchemaGenerator {

    /**
     * Standard JSON Schema representation for AI generation.
     */
    val JSON_SCHEMA_TEMPLATE = """
        [
          {
            "id": "q_1",
            "deck_name": "Medborgarskap",
            "question_sv": "Vilken myndighet ansvarar för folkbokföring i Sverige?",
            "question_ar": "ما هي الهيئة المسؤولة عن تسجيل السكان في السويد؟",
            "question_en": "Which authority is responsible for civil registration in Sweden?",
            "options": [
              "Migrationsverket",
              "Skatteverket",
              "Försäkringskassan",
              "Arbetsförmedlingen"
            ],
            "correct_index": 1,
            "explanation": "Skatteverket ansvarar för folkbokföringen i Sverige. (مصلحة الضرائب هي المسؤولة عن تسجيل السكان وإقامتهم في السويد.)"
          }
        ]
    """.trimIndent()

    /**
     * Complete system prompt & schema instruction ready for AI Structured Prompt Studio.
     */
    val AI_STUDIO_PROMPT_GUIDE = """
You are an expert civic education and exam questions generator.
Extract and generate exam questions in strict JSON array format adhering to the following structure:

JSON Schema Template:
$JSON_SCHEMA_TEMPLATE

Key requirements:
1. "deck_name": The subject category (e.g. "Medborgarskap", "Körkort", "SFI", "Samhällskunskap").
2. "question_sv": Question in Swedish.
3. "question_ar": Question translated accurately in Arabic.
4. "question_en": (Optional) Question in English.
5. "options": Array of 3 to 4 distinct options.
6. "correct_index": Zero-based integer index pointing to the right option in "options" (0, 1, 2, or 3).
7. "explanation": Informative explanation citing the legal, historical, or factual rationale in Swedish and Arabic.
    """.trimIndent()

    /**
     * Copies the standard AI Schema prompt template to the Android Clipboard.
     */
    fun copySchemaToClipboard(context: Context): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("CivicQuiz AI JSON Schema", AI_STUDIO_PROMPT_GUIDE)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
