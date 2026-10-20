package com.cricutandroid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricutandroid.app.Constants
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.viewmodel.MainViewModel

/**
 * Screen listing all available 3D Crafts.
 * Mirrors iOS CraftsVC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectCraft: (Craft, Boolean) -> Unit
) {
    val crafts = viewModel.getAvailableCrafts()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crafts",
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = crafts,
                key = { _, craft -> "craft_${craft.s3Name}" },
                contentType = { _, _ -> "craft_card" }
            ) { index, craft ->
                val isPremium = index > 2 && !viewModel.isProUser
                val thumbUrl = "${Constants.CLOUD_URL}Data/Crafts/${craft.s3Name}/Thumbnail/0.png"
                ThumbnailCard(
                    title = craft.nameInApp,
                    imageUrl = thumbUrl,
                    isPremium = isPremium,
                    cardHeight = 130.dp,
                    fillMaxWidth = true,
                    contentScale = ContentScale.Crop,
                    cornerRadius = 10.dp,
                    imagePadding = 0.dp,
                    onClick = { onSelectCraft(craft, isPremium) }
                )
            }
        }
    }
}
