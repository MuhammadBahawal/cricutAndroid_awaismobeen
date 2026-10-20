package com.cricutandroid.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricutandroid.app.managers.QueryManager
import com.cricutandroid.app.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiStyleOption(
    val title: String,
    val subtitle: String,
    val color: Color,
    val icon: ImageVector
)

/**
 * AI Creator Studio screen mirroring iOS AIEditingVC.
 * Allows users to generate SVG/cutfile prompts using curated artistic styles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEditingScreen(
    viewModel: MainViewModel,
    onOpenPaywall: () -> Unit,
    onDesignGenerated: (Bitmap, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var promptText by remember { mutableStateOf("") }
    var selectedStyleIndex by remember { mutableIntStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }

    val styles = remember {
        listOf(
            AiStyleOption("Create AI", "Design with AI", Color(0xFFFFA96C), Icons.Default.AutoAwesome),
            AiStyleOption("Crafts", "3D Paper & Vinyl", Color(0xFFFCC100), Icons.Default.ContentCut),
            AiStyleOption("Cutfiles", "Precision SVG", Color(0xFFA1D5FF), Icons.Default.ContentCut),
            AiStyleOption("Sublimation", "Vibrant Print", Color(0xFF7B86FF), Icons.Default.Palette),
            AiStyleOption("Stickers", "Die-Cut Graphics", Color(0xFFFAABFA), Icons.Default.Star),
            AiStyleOption("Watercolor", "Artistic Wash", Color(0xFF5FEDE6), Icons.Default.Brush),
            AiStyleOption("Shapes", "Geometric Vector", Color(0xFFD9E678), Icons.Default.AutoAwesome)
        )
    }

    val samplePrompts = listOf(
        "Floral monogram initial with vine details",
        "Cute coffee cup with steam heart",
        "Vintage mountain adventure badge",
        "Delicate butterfly wing silhouette",
        "Boho botanical wreath with leaves",
        "Cute Halloween ghost with pumpkin"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF08B67D)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Design Studio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Prompt Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Describe your design",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        val remaining = QueryManager.remainingQueries()
                        if (!viewModel.isProUser) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = "$remaining free AI queries",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = {
                            Text(
                                text = "Describe the image you want to create (e.g., 'Cute kitten with flower crown cutfile')...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF08B67D),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Or try an inspiration prompt:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(samplePrompts.size) { index ->
                            val sample = samplePrompts[index]
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF1F8F5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4EBDD)),
                                modifier = Modifier.clickable { promptText = sample }
                            ) {
                                Text(
                                    text = sample,
                                    fontSize = 12.sp,
                                    color = Color(0xFF08B67D),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Choose Artistic Style
            Text(
                text = "Choose Design Style",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(styles) { index, style ->
                    val isSelected = selectedStyleIndex == index
                    Surface(
                        modifier = Modifier
                            .width(120.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF08B67D) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedStyleIndex = index },
                        color = style.color.copy(alpha = 0.9f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.8f),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = style.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = style.subtitle,
                                    fontSize = 10.sp,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Generate Button
            Button(
                onClick = {
                    if (promptText.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter a design prompt first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!viewModel.isProUser && !QueryManager.canProceed()) {
                        onOpenPaywall()
                        return@Button
                    }

                    scope.launch {
                        isGenerating = true
                        QueryManager.consumeQuery()

                        val selectedStyle = styles[selectedStyleIndex]
                        val (generatedBitmap, svgStr) = withContext(Dispatchers.IO) {
                            delay(1800) // Simulated AI generation delay
                            createAiArtwork(promptText, selectedStyle.title)
                        }

                        isGenerating = false
                        onDesignGenerated(generatedBitmap, svgStr)
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D))
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Crafting your design...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Generate Design & Cutfile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

/**
 * Creates high resolution graphic cutfile art from the user's prompt.
 */
private fun createAiArtwork(prompt: String, styleName: String): Pair<Bitmap, String> {
    val size = 1000
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (styleName) {
            "Sublimation" -> 0xFF7B86FF.toInt()
            "Stickers" -> 0xFFFAABFA.toInt()
            "Watercolor" -> 0xFF08B67D.toInt()
            "Crafts" -> 0xFFFCC100.toInt()
            else -> 0xFF1E1E1E.toInt()
        }
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }

    // Outer decorative badge / circle
    canvas.drawCircle(500f, 500f, 400f, mainPaint)
    mainPaint.strokeWidth = 4f
    canvas.drawCircle(500f, 500f, 420f, mainPaint)

    // Decorative inner flower / star pattern
    val path = android.graphics.Path()
    for (i in 0 until 12) {
        val angle = i * (Math.PI / 6)
        val x1 = 500 + 360 * Math.cos(angle).toFloat()
        val y1 = 500 + 360 * Math.sin(angle).toFloat()
        val x2 = 500 + 200 * Math.cos(angle + 0.26).toFloat()
        val y2 = 500 + 200 * Math.sin(angle + 0.26).toFloat()
        if (i == 0) path.moveTo(x1, y1) else path.quadTo(x2, y2, x1, y1)
    }
    path.close()
    mainPaint.strokeWidth = 6f
    canvas.drawPath(path, mainPaint)

    // Center prompt banner text
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E1E1E.toInt()
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val displayWords = prompt.split(" ").take(4).joinToString(" ")
    canvas.drawText(displayWords.ifEmpty { "Cricut Custom Design" }, 500f, 480f, textPaint)

    textPaint.textSize = 28f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    textPaint.color = 0xFF757575.toInt()
    canvas.drawText("• $styleName Style •", 500f, 550f, textPaint)

    val svgString = """
        <?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" width="$size" height="$size" viewBox="0 0 $size $size">
          <circle cx="500" cy="500" r="400" fill="none" stroke="#1E1E1E" stroke-width="14"/>
          <circle cx="500" cy="500" r="420" fill="none" stroke="#1E1E1E" stroke-width="4"/>
          <text x="500" y="480" font-size="48" font-weight="bold" font-family="sans-serif" fill="#1E1E1E" text-anchor="middle">$displayWords</text>
          <text x="500" y="550" font-size="28" font-family="sans-serif" fill="#757575" text-anchor="middle">• $styleName Style •</text>
        </svg>
    """.trimIndent()

    return Pair(bitmap, svgString)
}
