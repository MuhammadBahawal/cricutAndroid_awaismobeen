package com.cricutandroid.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MockupProductType(val displayName: String) {
    TShirt("T-Shirt"),
    Mug("Mug"),
    Cap("Cap"),
    Bag("Bag / Tote"),
    Bottle("Bottle")
}

data class MockupColorOption(
    val name: String,
    val color: Color,
    val suffix: String
)

/**
 * Screen previewing designed artwork applied directly onto real 3D product mockups.
 * Mirrors iOS MockupVC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockupScreen(
    designBitmap: Bitmap,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(MockupProductType.TShirt) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }

    val colorOptions = remember {
        listOf(
            MockupColorOption("White", Color.White, "white"),
            MockupColorOption("Black", Color(0xFF1E1E1E), "black"),
            MockupColorOption("Red", Color(0xFFD32F2F), "red"),
            MockupColorOption("Blue", Color(0xFF1976D2), "blue"),
            MockupColorOption("Gray", Color(0xFF757575), "gray"),
            MockupColorOption("Brown", Color(0xFF795548), "brown"),
            MockupColorOption("Green", Color(0xFF388E3C), "green"),
            MockupColorOption("Purple", Color(0xFF7B1FA2), "purple"),
            MockupColorOption("Yellow", Color(0xFFFBC02D), "yellow"),
            MockupColorOption("Orange", Color(0xFFF57C00), "orange")
        )
    }

    // Resolves the drawable resource ID dynamically based on type & color
    val drawableResId = remember(selectedType, selectedColorIndex) {
        val colorSuffix = colorOptions[selectedColorIndex].suffix
        val prefix = when (selectedType) {
            MockupProductType.TShirt -> "mockup_tshirt_"
            MockupProductType.Mug -> "mockup_mug_"
            MockupProductType.Cap -> "mockup_cap_"
            MockupProductType.Bag -> "mockup_bag_"
            MockupProductType.Bottle -> "mockup_bottle_"
        }
        val resName = "$prefix$colorSuffix"
        val id = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (id != 0) id else R.drawable.ic_launcher_background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Product Mockup Preview",
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
                        scope.launch {
                            isSaving = true
                            val combined = withContext(Dispatchers.IO) {
                                renderCompositeMockup(context, drawableResId, designBitmap, selectedType)
                            }
                            VectorExporter.shareBitmap(context, combined, "Share Product Mockup")
                            isSaving = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
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
            // Mockup Canvas Stage
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Base Product Image
                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = "Product Mockup",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Superimposed Artwork with matching iOS coordinates
                    val (widthDp, heightDp, offsetX, offsetY) = when (selectedType) {
                        MockupProductType.TShirt -> Quadruple(160.dp, 160.dp, 0.dp, 10.dp)
                        MockupProductType.Mug -> Quadruple(140.dp, 140.dp, 10.dp, (-8).dp)
                        MockupProductType.Cap -> Quadruple(100.dp, 100.dp, 0.dp, (-40).dp)
                        MockupProductType.Bag -> Quadruple(120.dp, 120.dp, 8.dp, 30.dp)
                        MockupProductType.Bottle -> Quadruple(110.dp, 110.dp, 0.dp, 20.dp)
                    }

                    Image(
                        bitmap = designBitmap.asImageBitmap(),
                        contentDescription = "Placed Design",
                        modifier = Modifier
                            .size(widthDp, heightDp)
                            .offset(x = offsetX, y = offsetY),
                        contentScale = ContentScale.Fit
                    )

                    if (isSaving) {
                        CircularProgressIndicator(color = Color(0xFF08B67D))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selector Row
            Text(
                text = "Product Color",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(colorOptions) { index, option ->
                    val isSelected = selectedColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF08B67D) else Color(0xFFD9D9D9),
                                shape = CircleShape
                            )
                            .clickable { selectedColorIndex = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product Type Selector Row
            Text(
                text = "Select Product",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(MockupProductType.values()) { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF08B67D) else Color.White,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { selectedType = type }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Mockup Button
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        val combined = withContext(Dispatchers.IO) {
                            renderCompositeMockup(context, drawableResId, designBitmap, selectedType)
                        }
                        val uri = withContext(Dispatchers.IO) {
                            VectorExporter.saveBitmapToGallery(
                                context,
                                combined,
                                Bitmap.CompressFormat.PNG,
                                "cricut_mockup_${selectedType.name.lowercase()}_${System.currentTimeMillis()}"
                            )
                        }
                        isSaving = false
                        if (uri != null) {
                            Toast.makeText(context, "Mockup saved to Gallery!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save mockup", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D))
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Save Mockup to Photos", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Composites the base product mockup drawable with the artwork into a high-res Bitmap.
 */
private fun renderCompositeMockup(
    context: Context,
    productResId: Int,
    designBitmap: Bitmap,
    type: MockupProductType
): Bitmap {
    val options = android.graphics.BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    val productBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, productResId, options)
        ?: Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)

    val width = productBitmap.width
    val height = productBitmap.height
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawBitmap(productBitmap, 0f, 0f, paint)

    val (designW, designH, xOffset, yOffset) = when (type) {
        MockupProductType.TShirt -> Quadruple((width * 0.36f).toInt(), (height * 0.36f).toInt(), 0, (height * 0.02f).toInt())
        MockupProductType.Mug -> Quadruple((width * 0.32f).toInt(), (height * 0.32f).toInt(), (width * 0.02f).toInt(), (-height * 0.02f).toInt())
        MockupProductType.Cap -> Quadruple((width * 0.24f).toInt(), (height * 0.24f).toInt(), 0, (-height * 0.09f).toInt())
        MockupProductType.Bag -> Quadruple((width * 0.28f).toInt(), (height * 0.28f).toInt(), (width * 0.02f).toInt(), (height * 0.07f).toInt())
        MockupProductType.Bottle -> Quadruple((width * 0.26f).toInt(), (height * 0.26f).toInt(), 0, (height * 0.05f).toInt())
    }

    val left = (width - designW) / 2 + xOffset
    val top = (height - designH) / 2 + yOffset
    val destRect = Rect(left, top, left + designW, top + designH)
    canvas.drawBitmap(designBitmap, null, destRect, paint)

    return resultBitmap
}
