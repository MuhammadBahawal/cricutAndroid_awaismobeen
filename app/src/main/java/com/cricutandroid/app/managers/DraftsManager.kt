package com.cricutandroid.app.managers

import android.content.Context
import com.cricutandroid.app.models.DraftProject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Manages saving, loading, and deleting local project drafts.
 * Mirrors the iOS DraftVC functionality.
 */
object DraftsManager {

    private const val DRAFTS_FILE = "user_drafts.json"
    private val gson = Gson()

    fun getDrafts(context: Context): List<DraftProject> {
        val file = File(context.filesDir, DRAFTS_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<DraftProject>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveDraft(context: Context, draft: DraftProject) {
        val current = getDrafts(context).toMutableList()
        current.removeAll { it.id == draft.id }
        current.add(0, draft)
        saveAll(context, current)
    }

    fun deleteDraft(context: Context, id: String) {
        val current = getDrafts(context).toMutableList()
        current.removeAll { it.id == id }
        saveAll(context, current)
    }

    private fun saveAll(context: Context, list: List<DraftProject>) {
        val file = File(context.filesDir, DRAFTS_FILE)
        try {
            val json = gson.toJson(list)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
