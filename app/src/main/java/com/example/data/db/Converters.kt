package com.example.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * محول الأنواع (TypeConverter) لحفظ واسترجاع قائمة الخيارات List<String>
 * بصيغة JSON داخل قاعدة بيانات Room بدون الحاجة لمكتبات طرف ثالث إضافية.
 */
class Converters {
    @TypeConverter
    fun fromStringList(options: List<String>?): String {
        if (options == null) return "[]"
        val jsonArray = JSONArray()
        for (option in options) {
            jsonArray.put(option)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(optionsJson: String?): List<String> {
        if (optionsJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(optionsJson)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // في حال وجود خطأ في تحليل JSON يتم إرجاع قائمة فارغة
        }
        return list
    }
}
