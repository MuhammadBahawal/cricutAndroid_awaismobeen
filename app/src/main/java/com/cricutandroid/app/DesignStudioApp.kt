package com.cricutandroid.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.cricutandroid.app.managers.AppDataManager
import com.cricutandroid.app.managers.PrefsHelper
import com.cricutandroid.app.managers.SVGDownloadManager
import com.cricutandroid.app.models.AppDataModel
import com.cricutandroid.app.models.Craft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application subclass that mirrors iOS AppDelegate's lifecycle.
 * Initialises managers and loads bundled + remote data.
 */
class DesignStudioApp : Application(), SingletonImageLoader.Factory {

    /** Global app-data available to the entire app – mirrors `appDelegate.categoriesData` in iOS. */
    var categoriesData: AppDataModel = AppDataModel()
        private set
    var crafts: List<Craft> = emptyList()
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Init managers
        PrefsHelper.init(this)
        SVGDownloadManager.init(filesDir)

        // Load bundled data (mirrors loadCategories / loadCrafts in iOS)
        categoriesData = AppDataManager.loadCategoriesFromAssets(this) ?: AppDataModel()
        crafts = AppDataManager.loadCraftsFromAssets(this)

        // Download remote data and merge
        appScope.launch(Dispatchers.IO) {
            val result = AppDataManager.downloadAppData("AppData_v2")
            result.onSuccess { remote ->
                val merged = remote.merged(categoriesData, crafts)
                categoriesData = merged
                crafts = merged.crafts
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: DesignStudioApp
            private set
    }
}
