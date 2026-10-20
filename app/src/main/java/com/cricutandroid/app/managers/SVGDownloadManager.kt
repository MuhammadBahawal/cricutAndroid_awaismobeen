package com.cricutandroid.app.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and caches SVG files locally.
 * Mirrors the iOS SVGDownloadManager.
 */
object SVGDownloadManager {

    private lateinit var cacheDir: File

    fun init(cacheDir: File) {
        this.cacheDir = File(cacheDir, "AppData")
        if (!this.cacheDir.exists()) this.cacheDir.mkdirs()
    }

    /**
     * Get an SVG file — returns cached copy if available, otherwise downloads.
     */
    suspend fun getSVGFile(
        mainCategory: String,
        category: String,
        svgURL: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = getCategoryDir(mainCategory, category)
        val fileName = getFileName(svgURL, category)
        val file = File(dir, fileName)
        if (file.exists()) return@withContext Result.success(file)
        download(svgURL, file)
    }

    private fun getCategoryDir(mainCategory: String, category: String): File {
        val dir = File(cacheDir, "$mainCategory/$category")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getFileName(urlString: String, category: String): String {
        return try {
            val url = URL(urlString)
            val original = url.path.substringAfterLast('/')
            val extension = original.substringAfterLast('.', "svg")
            if (category == "Crafts" || extension.equals("zip", ignoreCase = true)) {
                val folder = url.path.split('/').dropLast(1).lastOrNull() ?: ""
                if (folder.isNotEmpty() && original.isNotEmpty()) "$folder-$original"
                else original.ifEmpty { "${System.currentTimeMillis()}.$extension" }
            } else {
                original.ifEmpty { "${System.currentTimeMillis()}.$extension" }
            }
        } catch (_: Exception) {
            "${System.currentTimeMillis()}.svg"
        }
    }

    private fun download(urlString: String, destination: File): Result<File> {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return Result.failure(Exception("Download failed with code ${connection.responseCode}"))
            }
            destination.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
