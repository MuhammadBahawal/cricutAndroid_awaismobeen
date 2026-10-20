package com.cricutandroid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricutandroid.app.Constants
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.models.AppDataItem
import com.cricutandroid.app.viewmodel.MainViewModel
import java.io.File

/**
 * Grid screen showing all SVG item assets in a category.
 * Mirrors iOS DataListVC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataListScreen(
    type: CurrentDataType,
    category: AppDataItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onEditSvg: (File) -> Unit
) {
    var activeTarget by remember { mutableStateOf<PreviewTarget?>(null) }
    val itemCount = if (category.count > 0) category.count else 25

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category.nameInApp,
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
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    count = itemCount,
                    key = { "item_${category.s3Name}_$it" },
                    contentType = { "data_item" }
                ) { index ->
                    val isPremium = index > 2 && !viewModel.isProUser
                    val thumbUrl = "${Constants.CLOUD_URL}Data/${type.getDirectory()}/${category.s3Name}/Thumbnail/$index.png"
                    ThumbnailCard(
                        title = null,
                        imageUrl = thumbUrl,
                        isPremium = isPremium,
                        cardHeight = 105.dp,
                        fillMaxWidth = true,
                        onClick = {
                            activeTarget = PreviewTarget(
                                mainCategory = type.getDirectory(),
                                categoryName = category.s3Name,
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
