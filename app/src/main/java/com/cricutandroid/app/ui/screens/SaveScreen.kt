package com.cricutandroid.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricutandroid.app.R
import com.cricutandroid.app.managers.VectorExporter
import com.cricutandroid.app.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen displaying the rendered artwork export options.
 * Mirrors iOS SaveVC: JPG, PNG, SVG, Transparent, Share, Print, and Mockup Preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveScreen(
    previewBitmap: Bitmap,
    svgString: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenMockup: (Bitmap) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Save & Export",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!viewModel.isProUser) {
                            onOpenPaywall()
                        } else {
                            VectorExporter.printBitmap(context, previewBitmap)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Design",
                            tint = Color(0xFF08B67D)
                        )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artwork Preview Card with Mockup Button attached
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Export Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        if (isSaving) {
                            CircularProgressIndicator(color = Color(0xFF08B67D))
                        }
                    }

                    // Mockup Preview Strip Button at bottom of card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onOpenMockup(previewBitmap) },
                        color = Color(0xFF08B67D)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview on Mockup (T-Shirt, Mug, Cap...)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Formats Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Export Format",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5 Export Option Cards (JPG, PNG, SVG, Transparent, Share)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: JPG & PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportFormatCard(
                        title = "JPG",
                        subtitle = "Standard Quality",
                        color = Color(0xFFFCC100),
                        isPro = false,
                        isProUser = viewModel.isProUser,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val uri = withContext(Dispatchers.IO) {
                                    VectorExporter.saveBitmapToGallery(
                                        context,
                                        previewBitmap,
                                        Bitmap.CompressFormat.JPEG,
                                        "cricut_design_${System.currentTimeMillis()}"
                                    )
                                }
                                isSaving = false
                                if (uri != null) {
                                    Toast.makeText(context, "Saved to Gallery as JPG!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    ExportFormatCard(
                        title = "PNG",
                        subtitle = "High Resolution",
                        color = Color(0xFFA1D5FF),
                        isPro = true,
                        isProUser = viewModel.isProUser,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!viewModel.isProUser) {
                                onOpenPaywall()
                            } else {
                                scope.launch {
                                    isSaving = true
                                    val uri = withContext(Dispatchers.IO) {
                                        VectorExporter.saveBitmapToGallery(
                                            context,
                                            previewBitmap,
                                            Bitmap.CompressFormat.PNG,
                                            "cricut_design_${System.currentTimeMillis()}"
                                        )
                                    }
                                    isSaving = false
                                    if (uri != null) {
                                        Toast.makeText(context, "Saved to Gallery as PNG!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                // Row 2: SVG (Cricut Cutter Ready) & Transparent PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportFormatCard(
                        title = "SVG",
                        subtitle = "Vector Cutfile",
                        color = Color(0xFF7B86FF),
                        isPro = true,
                        isProUser = viewModel.isProUser,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!viewModel.isProUser) {
                                onOpenPaywall()
                            } else {
                                val file = VectorExporter.saveSvgToFile(
                                    context,
                                    svgString,
                                    "cricut_cutfile_${System.currentTimeMillis()}"
                                )
                                Toast.makeText(context, "SVG exported to ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    ExportFormatCard(
                        title = "Transparent",
                        subtitle = "No Background",
                        color = Color(0xFFFAABFA),
                        isPro = true,
                        isProUser = viewModel.isProUser,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!viewModel.isProUser) {
                                onOpenPaywall()
                            } else {
                                scope.launch {
                                    isSaving = true
                                    val uri = withContext(Dispatchers.IO) {
                                        VectorExporter.saveBitmapToGallery(
                                            context,
                                            previewBitmap,
                                            Bitmap.CompressFormat.PNG,
                                            "cricut_transparent_${System.currentTimeMillis()}"
                                        )
                                    }
                                    isSaving = false
                                    if (uri != null) {
                                        Toast.makeText(context, "Transparent PNG saved to Gallery!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                // Row 3: Full Width Share Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (!viewModel.isProUser) {
                                onOpenPaywall()
                            } else {
                                VectorExporter.shareBitmap(context, previewBitmap)
                            }
                        },
                    color = Color(0xFF39C282)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Share with Friends or Cricut Machine",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (!viewModel.isProUser) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFC72C),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pro",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    title: String,
    subtitle: String,
    color: Color,
    isPro: Boolean,
    isProUser: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = color
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }

            if (isPro && !isProUser) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFC72C),
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pro",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
