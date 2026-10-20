package com.cricutandroid.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.DesignStudioApp
import com.cricutandroid.app.managers.AppDataManager
import com.cricutandroid.app.managers.BillingManager
import com.cricutandroid.app.managers.DraftsManager
import com.cricutandroid.app.models.AppDataItem
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.models.DraftProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central ViewModel that manages app state, categories, crafts, drafts, and premium status.
 */
class MainViewModel : ViewModel() {

    var categoriesData by mutableStateOf(DesignStudioApp.instance.categoriesData)
        private set
    var crafts by mutableStateOf(DesignStudioApp.instance.crafts)
        private set
    var isProUser by mutableStateOf(BillingManager.isProUser())
        private set
    var drafts by mutableStateOf<List<DraftProject>>(emptyList())
        private set

    var showPaywall by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    init {
        refreshData()
    }

    fun refreshData() {
        categoriesData = DesignStudioApp.instance.categoriesData
        crafts = DesignStudioApp.instance.crafts
        isProUser = BillingManager.isProUser()

        viewModelScope.launch(Dispatchers.IO) {
            val result = AppDataManager.downloadAppData("AppData_v2")
            result.onSuccess { remote ->
                val merged = remote.merged(categoriesData, crafts)
                withContext(Dispatchers.Main) {
                    categoriesData = merged
                    crafts = merged.crafts
                }
            }
        }
    }

    fun loadDrafts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = DraftsManager.getDrafts(context)
            drafts = list
        }
    }

    fun deleteDraft(context: Context, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DraftsManager.deleteDraft(context, id)
            loadDrafts(context)
        }
    }

    fun refreshPremiumStatus() {
        isProUser = BillingManager.isProUser()
    }

    fun getAvailableCrafts(): List<Craft> {
        return crafts.ifEmpty { categoriesData.crafts }
    }

    fun getCurrentData(type: CurrentDataType): List<AppDataItem>? {
        return when (type) {
            CurrentDataType.Cutfiles -> categoriesData.cutFiles
            CurrentDataType.Monograms -> categoriesData.monograms
            CurrentDataType.Watercolors -> categoriesData.waterColour
            CurrentDataType.Stickers -> categoriesData.stickers
            CurrentDataType.Shapes -> categoriesData.shapes
            CurrentDataType.Sublimation -> categoriesData.sublimations
            CurrentDataType.Fonts,
            CurrentDataType.MonogramFonts,
            CurrentDataType.Craft,
            CurrentDataType.Tshirts,
            CurrentDataType.Mugs,
            CurrentDataType.ThreeDSticker,
            CurrentDataType.Caps -> null
        }
    }
}
