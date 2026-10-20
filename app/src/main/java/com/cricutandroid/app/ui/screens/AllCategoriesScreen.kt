package com.cricutandroid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import com.cricutandroid.app.Constants
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.models.AppDataItem
import com.cricutandroid.app.viewmodel.MainViewModel
import java.io.File

/**
 * Screen displaying all category cards or count assets for a given data type.
 * Mirrors iOS AllCategoryVC and CraftsVC count asset listing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesScreen(
    type: CurrentDataType,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectCategory: (CurrentDataType, AppDataItem) -> Unit,
    onOpenPaywall: () -> Unit = {},
    onEditSvg: (File) -> Unit = {}
) {
    val items = viewModel.getCurrentData(type) ?: emptyList()
    val count = when (type) {
        CurrentDataType.Tshirts -> viewModel.categoriesData.tshirt
        CurrentDataType.Mugs -> viewModel.categoriesData.mugs
        CurrentDataType.Caps -> viewModel.categoriesData.caps
        CurrentDataType.ThreeDSticker -> viewModel.categoriesData.threeDSticker
        else -> 0
    }
    val dirCandidates = remember(type) { type.directoryCandidates() }
    var activeTarget by remember { mutableStateOf<PreviewTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = type.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (items.isNotEmpty()) {
                    itemsIndexed(
                        items = items,
                        key = { _, category -> "allcat_${category.s3Name}" },
                        contentType = { _, _ -> "allcat_category" }
                    ) { index, category ->
                        val thumbUrl = "${Constants.CLOUD_URL}Data/${type.getDirectory()}/${category.s3Name}/Thumbnail/0.png"
                        ThumbnailCard(
                            title = category.nameInApp,
                            imageUrl = thumbUrl,
                            isPremium = false,
                            cardHeight = 130.dp,
                            fillMaxWidth = true,
                            onClick = { onSelectCategory(type, category) }
                        )
                    }
                } else if (count > 0) {
                    items(
                        count = count,
                        key = { "count_asset_${type.name}_$it" },
                        contentType = { "count_asset" }
                    ) { index ->
                        val isPremium = index > 2 && !viewModel.isProUser
                        val candidates = dirCandidates.map { dir ->
                            "${Constants.CLOUD_URL}Data/$dir/Thumbnail/$index.png"
                        }
                        val thumbUrl = candidates.first()
                        ThumbnailCard(
                            title = null,
                            imageUrl = thumbUrl,
                            fallbackUrls = candidates.drop(1),
                            isPremium = isPremium,
                            cardHeight = 110.dp,
                            fillMaxWidth = true,
                            onClick = {
                                activeTarget = PreviewTarget(
                                    mainCategory = type.getDirectory(),
                                    categoryName = type.getDirectory(),
                                    index = index,
                                    previewUrl = thumbUrl,
                                    isProItem = isPremium
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    activeTarget?.let { target ->
        EditSaveDialog(
            mainCategory = target.mainCategory,
            categoryName = target.categoryName,
            index = target.index,
            previewUrl = target.previewUrl,
            isProItem = target.isProItem,
            isProUser = viewModel.isProUser,
            onDismiss = { activeTarget = null },
            onOpenPaywall = {
                activeTarget = null
                onOpenPaywall()
            },
            onEditSvg = { file ->
                activeTarget = null
                onEditSvg(file)
            }
        )
    }
}
