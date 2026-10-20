package com.cricutandroid.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cricutandroid.app.Constants
import com.cricutandroid.app.managers.SVGDownloadManager
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Details view for a 3D Craft showing preview, assembly instructions, formats, and action buttons.
 * Mirrors iOS CraftsDetailsVC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftDetailsScreen(
    craft: Craft,
    isPremium: Boolean,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onEditSvg: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val thumbUrl = "${Constants.CLOUD_URL}Data/Crafts/${craft.s3Name}/Thumbnail/0.png"
    val svgUrl = "${Constants.CLOUD_URL}Data/Crafts/${craft.s3Name}/Svg/0.svg"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = craft.nameInApp,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = craft.nameInApp,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF08B67D))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Formats Badge
            if (craft.formatFile.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "Formats: ${craft.formatFile}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Description
            Text(
                text = "Description",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = craft.description.ifEmpty { "High-quality 3D craft template designed for cutting machines and paper craft projects." },
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Assembly Guide
            if (craft.assemble.isNotEmpty()) {
                Text(
                    text = "Assembly Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = craft.assemble,
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.8f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isPremium && !viewModel.isProUser) {
                            onOpenPaywall()
                        } else {
                            scope.launch {
                                isLoading = true
                                val result = SVGDownloadManager.getSVGFile("Crafts", craft.s3Name, svgUrl)
                                isLoading = false
                                result.onSuccess { file ->
                                    onEditSvg(file)
                                }.onFailure {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit Design",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (isPremium && !viewModel.isProUser) {
                            onOpenPaywall()
                        } else {
                            scope.launch {
                                isLoading = true
                                val result = SVGDownloadManager.getSVGFile("Crafts", craft.s3Name, svgUrl)
                                isLoading = false
                                result.onSuccess { file ->
                                    Toast.makeText(context, "Downloaded to ${file.name}", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF08B67D)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF08B67D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF08B67D),
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
