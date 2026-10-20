package com.cricutandroid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cricutandroid.app.Constants
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.R
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.viewmodel.MainViewModel
import java.io.File

/**
 * Main Home Dashboard screen mirroring iOS HomeVC & HomeScreenView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenCrafts: () -> Unit,
    onOpenCraftDetail: (Craft, Boolean) -> Unit,
    onOpenCategory: (CurrentDataType) -> Unit,
    onOpenAiGenerator: () -> Unit,
    onEditSvg: (File) -> Unit
) {
    var activePreviewItem by remember { mutableStateOf<PreviewTarget?>(null) }

    val primaryGreen = Color(0xFF08B67D)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Design Studio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer Menu",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    if (!viewModel.isProUser) {
                        IconButton(
                            onClick = onOpenPaywall,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC72C))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_crown),
                                contentDescription = "Upgrade to Pro",
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Pro Banner Card
            if (!viewModel.isProUser) {
                item(key = "banner_pro") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFCBE5B3), Color(0xFFF6F2E6))
                                )
                            )
                            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Upgrade to ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(text = "Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0FA66A))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unlock premium designs,\nexclusive tools & more.",
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onOpenPaywall,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DB56F)),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(text = "Upgrade Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Image(
                                painter = painterResource(id = R.drawable.ic_home_banner),
                                contentDescription = "Upgrade to Pro",
                                modifier = Modifier.size(width = 124.dp, height = 104.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // AI Creator Banner
            item(key = "banner_ai") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFA96C))
                        .clickable { onOpenAiGenerator() }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "AI Image & SVG Generator",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Turn your prompts into custom craft designs!",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.ic_create_ai),
                            contentDescription = "AI Generator",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 1. Crafts Section
            item(key = "section_crafts") {
                val crafts = remember(viewModel.crafts, viewModel.categoriesData.crafts) {
                    viewModel.getAvailableCrafts()
                }
                SectionHeader(
                    title = "Crafts",
                    onSeeAll = onOpenCrafts
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (crafts.isEmpty()) {
                        items(4, key = { "craft_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 150.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = crafts,
                            key = { _, craft -> "craft_${craft.s3Name}" },
                            contentType = { _, _ -> "craft_card" }
                        ) { index, craft ->
                            val isPremium = index > 2 && !viewModel.isProUser
                            val thumbUrl = "${Constants.CLOUD_URL}Data/Crafts/${craft.s3Name}/Thumbnail/0.png"
                            ThumbnailCard(
                                title = null,
                                imageUrl = thumbUrl,
                                isPremium = isPremium,
                                cardWidth = 150.dp,
                                cardHeight = 110.dp,
                                contentScale = ContentScale.Crop,
                                cornerRadius = 10.dp,
                                imagePadding = 0.dp,
                                onClick = { onOpenCraftDetail(craft, isPremium) }
                            )
                        }
                    }
                }
            }

            // 2. Cutfiles Section
            item(key = "section_cutfiles") {
                val items = viewModel.categoriesData.cutFiles
                SectionHeader(
                    title = "Cutfile",
                    onSeeAll = { onOpenCategory(CurrentDataType.Cutfiles) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (items.isEmpty()) {
                        items(5, key = { "cutfile_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> "cutfile_${item.s3Name}" },
                            contentType = { _, _ -> "cutfile_card" }
                        ) { index, item ->
                            val thumbUrl = "${Constants.CLOUD_URL}Data/CutFiles/${item.s3Name}/Thumbnail/0.png"
                            ThumbnailCard(
                                title = null,
                                imageUrl = thumbUrl,
                                isPremium = false,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("CutFiles", item.s3Name, 0, thumbUrl, false)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Sublimation Section
            item(key = "section_sublimations") {
                val items = viewModel.categoriesData.sublimations
                SectionHeader(
                    title = "Sublimation",
                    onSeeAll = { onOpenCategory(CurrentDataType.Sublimation) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (items.isEmpty()) {
                        items(5, key = { "sublimation_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> "sublimation_${item.s3Name}" },
                            contentType = { _, _ -> "sublimation_card" }
                        ) { index, item ->
                            val thumbUrl = "${Constants.CLOUD_URL}Data/Sublimations/${item.s3Name}/Thumbnail/0.png"
                            ThumbnailCard(
                                title = null,
                                imageUrl = thumbUrl,
                                isPremium = false,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("Sublimations", item.s3Name, 0, thumbUrl, false)
                                }
                            )
                        }
                    }
                }
            }

            // 4. Stickers Section
            item(key = "section_stickers") {
                val items = viewModel.categoriesData.stickers
                SectionHeader(
                    title = "Sticker",
                    onSeeAll = { onOpenCategory(CurrentDataType.Stickers) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (items.isEmpty()) {
                        items(5, key = { "sticker_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> "sticker_${item.s3Name}" },
                            contentType = { _, _ -> "sticker_card" }
                        ) { index, item ->
                            val thumbUrl = "${Constants.CLOUD_URL}Data/Stickers/${item.s3Name}/Thumbnail/0.png"
                            ThumbnailCard(
                                title = null,
                                imageUrl = thumbUrl,
                                isPremium = false,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("Stickers", item.s3Name, 0, thumbUrl, false)
                                }
                            )
                        }
                    }
                }
            }

            // 5. Watercolors Section
            item(key = "section_watercolors") {
                val items = viewModel.categoriesData.waterColour
                SectionHeader(
                    title = "Watercolour",
                    onSeeAll = { onOpenCategory(CurrentDataType.Watercolors) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (items.isEmpty()) {
                        items(5, key = { "watercolor_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> "watercolor_${item.s3Name}" },
                            contentType = { _, _ -> "watercolor_card" }
                        ) { index, item ->
                            val thumbUrl = "${Constants.CLOUD_URL}Data/WaterColour/${item.s3Name}/Thumbnail/0.png"
                            ThumbnailCard(
                                title = null,
                                imageUrl = thumbUrl,
                                isPremium = false,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("WaterColour", item.s3Name, 0, thumbUrl, false)
                                }
                            )
                        }
                    }
                }
            }

            // 6. T-Shirts Section
            item(key = "section_tshirts") {
                val count = viewModel.categoriesData.tshirt
                SectionHeader(
                    title = "T-Shirts",
                    onSeeAll = { onOpenCategory(CurrentDataType.Tshirts) }
                )
                val tshirtDirs = remember { CurrentDataType.Tshirts.directoryCandidates() }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (count == 0) {
                        items(5, key = { "tshirt_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        items(
                            count = count,
                            key = { "tshirt_$it" },
                            contentType = { "asset_thumb" }
                        ) { index ->
                            val isPremium = index > 2 && !viewModel.isProUser
                            val candidates = remember(index) {
                                tshirtDirs.map { dir ->
                                    "${Constants.CLOUD_URL}Data/$dir/Thumbnail/$index.png"
                                }
                            }
                            ThumbnailCard(
                                title = null,
                                imageUrl = candidates.first(),
                                fallbackUrls = candidates.drop(1),
                                isPremium = isPremium,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("TShirts", "TShirts", index, candidates.first(), isPremium)
                                }
                            )
                        }
                    }
                }
            }

            // 7. Mugs Section
            item(key = "section_mugs") {
                val count = viewModel.categoriesData.mugs
                SectionHeader(
                    title = "Mugs",
                    onSeeAll = { onOpenCategory(CurrentDataType.Mugs) }
                )
                val mugDirs = remember { CurrentDataType.Mugs.directoryCandidates() }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (count == 0) {
                        items(5, key = { "mug_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        items(
                            count = count,
                            key = { "mug_$it" },
                            contentType = { "asset_thumb" }
                        ) { index ->
                            val isPremium = index > 2 && !viewModel.isProUser
                            val candidates = remember(index) {
                                mugDirs.map { dir ->
                                    "${Constants.CLOUD_URL}Data/$dir/Thumbnail/$index.png"
                                }
                            }
                            ThumbnailCard(
                                title = null,
                                imageUrl = candidates.first(),
                                fallbackUrls = candidates.drop(1),
                                isPremium = isPremium,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("Mugs", "Mugs", index, candidates.first(), isPremium)
                                }
                            )
                        }
                    }
                }
            }

            // 8. Caps Section
            item(key = "section_caps") {
                val count = viewModel.categoriesData.caps
                SectionHeader(
                    title = "Caps",
                    onSeeAll = { onOpenCategory(CurrentDataType.Caps) }
                )
                val capDirs = remember { CurrentDataType.Caps.directoryCandidates() }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (count == 0) {
                        items(5, key = { "cap_placeholder_$it" }) {
                            PlaceholderThumbnailCard(
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                cornerRadius = 10.dp
                            )
                        }
                    } else {
                        items(
                            count = count,
                            key = { "cap_$it" },
                            contentType = { "asset_thumb" }
                        ) { index ->
                            val isPremium = index > 2 && !viewModel.isProUser
                            val candidates = remember(index) {
                                capDirs.map { dir ->
                                    "${Constants.CLOUD_URL}Data/$dir/Thumbnail/$index.png"
                                }
                            }
                            ThumbnailCard(
                                title = null,
                                imageUrl = candidates.first(),
                                fallbackUrls = candidates.drop(1),
                                isPremium = isPremium,
                                cardWidth = 110.dp,
                                cardHeight = 110.dp,
                                onClick = {
                                    activePreviewItem = PreviewTarget("Caps", "Caps", index, candidates.first(), isPremium)
                                }
                            )
                        }
                    }
                }
            }
            }
        }

    // Modal Action Dialog
    activePreviewItem?.let { target ->
        EditSaveDialog(
            mainCategory = target.mainCategory,
            categoryName = target.categoryName,
            index = target.index,
            previewUrl = target.previewUrl,
            isProItem = target.isProItem,
            isProUser = viewModel.isProUser,
            onDismiss = { activePreviewItem = null },
            onOpenPaywall = {
                activePreviewItem = null
                onOpenPaywall()
            },
            onEditSvg = { file ->
                activePreviewItem = null
                onEditSvg(file)
            }
        )
    }
}

data class PreviewTarget(
    val mainCategory: String,
    val categoryName: String,
    val index: Int,
    val previewUrl: String,
    val isProItem: Boolean
)

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "See All",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF08B67D),
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}

@Composable
fun PlaceholderThumbnailCard(
    cardWidth: androidx.compose.ui.unit.Dp = 110.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 110.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp
) {
    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Box(
        modifier = Modifier
            .size(cardWidth, cardHeight)
            .clip(cardShape)
            .background(Color(0xFFF7F8FA)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color(0xFF08B67D),
            strokeWidth = 2.5.dp
        )
    }
}

@Composable
fun ThumbnailCard(
    title: String? = null,
    imageUrl: String,
    fallbackUrls: List<String> = emptyList(),
    isPremium: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp = 150.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 110.dp,
    fillMaxWidth: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    showBorder: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp,
    imagePadding: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit
) {
    val allUrls = remember(imageUrl, fallbackUrls) {
        if (fallbackUrls.isEmpty()) listOf(imageUrl) else listOf(imageUrl) + fallbackUrls
    }
    var urlIndex by remember(imageUrl, fallbackUrls) { mutableIntStateOf(0) }
    val currentUrl = allUrls.getOrNull(urlIndex) ?: imageUrl

    val context = LocalContext.current
    val isCached = remember(currentUrl) {
        try {
            SingletonImageLoader.get(context).memoryCache?.get(MemoryCache.Key(currentUrl)) != null
        } catch (_: Exception) {
            false
        }
    }
    var isImageLoading by remember(currentUrl) { mutableStateOf(!isCached) }

    val imageRequest = remember(currentUrl) {
        ImageRequest.Builder(context)
            .data(currentUrl)
            .crossfade(true)
            .build()
    }

    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (fillMaxWidth) {
            Modifier.fillMaxWidth().clickable { onClick() }
        } else {
            Modifier.width(cardWidth).clickable { onClick() }
        }
    ) {
        Box(
            modifier = (if (fillMaxWidth) {
                Modifier.fillMaxWidth().height(cardHeight)
            } else {
                Modifier.size(cardWidth, cardHeight)
            })
                .clip(cardShape)
                .then(
                    if (showBorder) {
                        Modifier
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.08f), cardShape)
                    } else if (isImageLoading) {
                        Modifier.background(Color(0xFFF7F8FA))
                    } else {
                        Modifier.background(Color.Transparent)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = contentScale,
                onLoading = {
                    if (!isCached) isImageLoading = true
                },
                onSuccess = {
                    isImageLoading = false
                },
                onError = {
                    if (urlIndex + 1 < allUrls.size) {
                        urlIndex++
                    } else {
                        isImageLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(imagePadding)
            )

            if (isImageLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF08B67D),
                    strokeWidth = 2.dp
                )
            }

            if (isPremium) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pro_tag),
                    contentDescription = "Pro",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                )
            }
        }

        if (!title.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
