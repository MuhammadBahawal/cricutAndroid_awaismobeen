package com.cricutandroid.app.managers

import android.content.Context
import com.cricutandroid.app.models.AppDataModel
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.models.CraftsDataModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages app data loading from bundled JSON assets and remote CloudFront.
 * Mirrors the iOS AppDataManager + AppDelegate data-loading logic.
 */
object AppDataManager {

    private val gson = Gson()

    /**
     * Load the bundled Templates.json from assets/.
     */
    fun loadCategoriesFromAssets(context: Context): AppDataModel? {
        return try {
            context.assets.open("Templates.json").use { stream ->
                gson.fromJson(InputStreamReader(stream), AppDataModel::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load the bundled CraftsJson.json from assets/.
     */
    fun loadCraftsFromAssets(context: Context): List<Craft> {
        return try {
            context.assets.open("CraftsJson.json").use { stream ->
                val response = gson.fromJson(InputStreamReader(stream), CraftsDataModel::class.java)
                response.crafts
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Download remote AppData_v2.json (or AppData.json fallback) from CloudFront.
     */
    suspend fun downloadAppData(fileName: String = "AppData_v2"): Result<AppDataModel> {
        val candidates = listOf(fileName, if (fileName == "AppData_v2") "AppData" else "AppData_v2")
        return downloadAppData(candidates, 0)
    }

    private suspend fun downloadAppData(candidates: List<String>, index: Int): Result<AppDataModel> {
        if (index >= candidates.size) {
            return Result.failure(Exception("Unable to download compatible app data."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://dehgajvsdmyp.cloudfront.net/${candidates[index]}.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                val code = connection.responseCode
                if (code !in 200..299) {
                    return@withContext downloadAppData(candidates, index + 1)
                }
                val json = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val appData = gson.fromJson(json, AppDataModel::class.java)

                if (appData.crafts.isEmpty()) {
                    val craftsResult = downloadCrafts()
                    if (craftsResult.isSuccess) {
                        Result.success(appData.copy(crafts = craftsResult.getOrDefault(emptyList())))
                    } else {
                        Result.success(appData)
                    }
                } else {
                    Result.success(appData)
                }
            } catch (e: Exception) {
                downloadAppData(candidates, index + 1)
            }
        }
    }

    private suspend fun downloadCrafts(): Result<List<Craft>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://dehgajvsdmyp.cloudfront.net/Crafts.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Invalid crafts response"))
            }
            val json = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            val craftsData = gson.fromJson(json, CraftsDataModel::class.java)
            Result.success(craftsData.crafts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
