package com.katoaapps.openminilaunch.features.minkspace.calculator

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal data class CalculatorHistoryEntry(
    val expression: String,
    val result: String,
)

internal class CalculatorHistoryRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableEntries = mutableStateListOf<CalculatorHistoryEntry>()
    val entries: List<CalculatorHistoryEntry> get() = mutableEntries

    init {
        mutableEntries.addAll(load())
    }

    fun add(expression: String, result: String) {
        mutableEntries.removeAll { it.expression == expression && it.result == result }
        mutableEntries.add(0, CalculatorHistoryEntry(expression, result))
        while (mutableEntries.size > MAX_ENTRIES) mutableEntries.removeAt(mutableEntries.lastIndex)
        save()
    }

    fun clear() {
        mutableEntries.clear()
        save()
    }

    private fun load(): List<CalculatorHistoryEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    CalculatorHistoryEntry(
                        expression = item.getString("expression"),
                        result = item.getString("result"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val array = JSONArray().apply {
            mutableEntries.forEach { entry ->
                put(
                    JSONObject()
                        .put("expression", entry.expression)
                        .put("result", entry.result),
                )
            }
        }
        prefs.edit { putString(KEY_HISTORY, array.toString()) }
    }

    companion object {
        private const val PREFERENCES = "mink_space_calculator"
        private const val KEY_HISTORY = "history"
        private const val MAX_ENTRIES = 50

        @Volatile
        private var instance: CalculatorHistoryRepository? = null

        fun get(context: Context): CalculatorHistoryRepository = instance ?: synchronized(this) {
            instance ?: CalculatorHistoryRepository(context.applicationContext).also { instance = it }
        }
    }
}
