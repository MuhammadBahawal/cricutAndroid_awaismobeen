package com.cricutandroid.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.cricutandroid.app.Constants
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.managers.DraftsManager
import com.cricutandroid.app.managers.SVGDownloadManager
import com.cricutandroid.app.managers.VectorExporter
import com.cricutandroid.app.models.AppDataItem
import com.cricutandroid.app.models.CanvasLayer
import com.cricutandroid.app.models.DraftProject
import com.cricutandroid.app.viewmodel.MainViewModel
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.cricutandroid.app.R

enum class EditorBottomTool(val title: String, val iconRes: Int) {
    Text("Text", R.drawable.ic_tool_text),
    Image("Image", R.drawable.ic_tool_image),
    Cutfiles("Cutfiles", R.drawable.ic_tool_cutfiles),
    Sublimation("Sublimation", R.drawable.ic_tool_sublimations),
    Stickers("Stickers", R.drawable.ic_tool_stickers),
    Watercolor("Watercolor", R.drawable.ic_tool_watercolors),
    Shapes("Shapes", R.drawable.ic_tool_shapes)
}

/**
 * Full Vector Editor Screen mirroring iOS EditorVC & VectorEditor.
 * Supports multi-layer composition, interactive selection/transform gestures,
 * undo/redo, text styling, shapes, gallery imports, category assets, and export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialSvgFile: File?,
    initialDraft: DraftProject?,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onExport: (Bitmap, String) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Canvas Layers State
    val layers = remember { mutableStateListOf<CanvasLayer>() }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var canvasVersion by remember { mutableIntStateOf(0) }

    // Undo / Redo History
    val undoStack = remember { mutableStateListOf<List<CanvasLayer>>() }
    val redoStack = remember { mutableStateListOf<List<CanvasLayer>>() }

    fun pushHistory() {
        undoStack.add(layers.map { it.copyLayer() })
        redoStack.clear()
        if (undoStack.size > 30) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(layers.map { it.copyLayer() })
            val previous = undoStack.removeAt(undoStack.lastIndex)
            layers.clear()
            layers.addAll(previous.map { it.copyLayer() })
            if (layers.none { it.id == selectedLayerId }) selectedLayerId = null
            canvasVersion++
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(layers.map { it.copyLayer() })
            val next = redoStack.removeAt(redoStack.lastIndex)
            layers.clear()
            layers.addAll(next.map { it.copyLayer() })
            if (layers.none { it.id == selectedLayerId }) selectedLayerId = null
            canvasVersion++
        }
    }

    // Load initial SVG or Draft if provided
    LaunchedEffect(initialSvgFile, initialDraft) {
        if (layers.isEmpty()) {
            if (initialDraft != null && initialDraft.layersJson.isNotEmpty()) {
                val restored = LayerJsonHelper.deserialize(initialDraft.layersJson)
                layers.addAll(restored)
                selectedLayerId = layers.firstOrNull()?.id
                canvasVersion++
            } else if (initialSvgFile != null) {
                layers.add(
                    CanvasLayer.Image(
                        id = UUID.randomUUID().toString(),
                        x = 500f,
                        y = 500f,
                        urlOrPath = initialSvgFile.absolutePath,
                        width = 400f,
                        height = 400f
                    )
                )
                selectedLayerId = layers.firstOrNull()?.id
                canvasVersion++
            }
        }
    }

    // Dialog & Sheet States
    var showLayersSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var activeCategoryPickerType by remember { mutableStateOf<CurrentDataType?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var canvasAspectRatio by remember { mutableFloatStateOf(1f) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var customColorTarget by remember { mutableStateOf("fill") }

    // Intercept Back Press gracefully inside Editor
    androidx.activity.compose.BackHandler {
        if (showLayersSheet) {
            showLayersSheet = false
        } else if (showAddTextDialog) {
            showAddTextDialog = false
        } else if (showEditTextDialog) {
            showEditTextDialog = false
        } else if (activeCategoryPickerType != null) {
            activeCategoryPickerType = null
        } else if (selectedLayerId != null) {
            selectedLayerId = null
        } else if (undoStack.isNotEmpty()) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    // Bottom tool selection
    var selectedTool by remember { mutableStateOf<EditorBottomTool?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pushHistory()
            val newLayer = CanvasLayer.Image(
                id = UUID.randomUUID().toString(),
                x = 500f,
                y = 500f,
                urlOrPath = it.toString(),
                width = 350f,
                height = 350f
            )
            layers.add(newLayer)
            selectedLayerId = newLayer.id
            canvasVersion++
        }
    }

    val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Canvas Editor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (undoStack.isNotEmpty()) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) Color.Black else Color.LightGray
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) Color.Black else Color.LightGray
                        )
                    }

                    // Reset
                    IconButton(onClick = {
                        if (layers.isNotEmpty()) {
                            pushHistory()
                            layers.clear()
                            selectedLayerId = null
                            canvasVersion++
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Canvas",
                            tint = Color.Black
                        )
                    }

                    // Layers Sheet Button
                    IconButton(onClick = { showLayersSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (showLayersSheet) Color(0xFF08B67D) else Color.Black
                        )
                    }

                    // Export / Save Button
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val exportWidth = 1000
                                val exportHeight = (1000f * canvasAspectRatio).toInt().coerceIn(600, 3000)
                                val bitmap = withContext(Dispatchers.IO) {
                                    VectorExporter.renderToBitmap(
                                        context = context,
                                        layers = layers.toList(),
                                        width = exportWidth,
                                        height = exportHeight,
                                        backgroundColor = android.graphics.Color.WHITE
                                    )
                                }
                                val svgStr = VectorExporter.generateSvgString(
                                    layers = layers.toList(),
                                    width = exportWidth,
                                    height = exportHeight
                                )

                                // Auto-save draft with preview image and serialized layers
                                val draftId = initialDraft?.id ?: UUID.randomUUID().toString()
                                val previewFile = File(context.filesDir, "draft_${draftId}.png")
                                try {
                                    FileOutputStream(previewFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                val draft = DraftProject(
                                    id = draftId,
                                    title = initialDraft?.title ?: "Project ${System.currentTimeMillis() % 10000}",
                                    timestamp = System.currentTimeMillis(),
                                    previewImagePath = previewFile.absolutePath,
                                    layersJson = LayerJsonHelper.serialize(layers.toList())
                                )
                                DraftsManager.saveDraft(context, draft)

                                isExporting = false
                                onExport(bitmap, svgStr)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
            ) {
                // Top divider separating canvas grid and bottom editor
                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                // Secondary Property Bar if a Layer is selected
                if (selectedLayer != null) {
                    LayerPropertiesBar(
                        layer = selectedLayer,
                        canvasVersion = canvasVersion,
                        onUpdate = { canvasVersion++ },
                        onPushHistory = { pushHistory() },
                        onEditContent = {
                            if (selectedLayer is CanvasLayer.Text) {
                                showEditTextDialog = true
                            }
                        },
                        onDuplicate = {
                            pushHistory()
                            val clone = selectedLayer.copyLayer(newId = true)
                            clone.x += 30f
                            clone.y += 30f
                            layers.add(clone)
                            selectedLayerId = clone.id
                            canvasVersion++
                        },
                        onDelete = {
                            pushHistory()
                            layers.remove(selectedLayer)
                            selectedLayerId = null
                            canvasVersion++
                        },
                        onDeselect = {
                            selectedLayerId = null
                        },
                        onOpenColorPicker = { target ->
                            customColorTarget = target
                            showCustomColorPicker = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }

                // Primary Bottom Tools (Text, Image, Cutfiles, Sublimation, Stickers, Watercolor, Shapes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorBottomTool.values().forEach { tool ->
                        val isSelected = selectedTool == tool
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTool = tool
                                    when (tool) {
                                        EditorBottomTool.Text -> {
                                            if (selectedLayer is CanvasLayer.Text) {
                                                showEditTextDialog = true
                                            } else {
                                                showAddTextDialog = true
                                            }
                                        }
                                        EditorBottomTool.Image -> {
                                            imagePickerLauncher.launch("image/*")
                                        }
                                        EditorBottomTool.Cutfiles -> {
                                            activeCategoryPickerType = CurrentDataType.Cutfiles
                                        }
                                        EditorBottomTool.Sublimation -> {
                                            activeCategoryPickerType = CurrentDataType.Sublimation
                                        }
                                        EditorBottomTool.Stickers -> {
                                            activeCategoryPickerType = CurrentDataType.Stickers
                                        }
                                        EditorBottomTool.Watercolor -> {
                                            activeCategoryPickerType = CurrentDataType.Watercolors
                                        }
                                        EditorBottomTool.Shapes -> {
                                            activeCategoryPickerType = CurrentDataType.Shapes
                                        }
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = tool.iconRes),
                                contentDescription = tool.title,
                                modifier = Modifier.size(22.dp),
                                colorFilter = ColorFilter.tint(if (isSelected) Color(0xFF00A94C) else Color(0xFF2C2C2E))
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tool.title,
                                fontSize = 10.sp,
                                letterSpacing = (-0.3).sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF00A94C) else Color(0xFF2C2C2E),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFEBEBEB)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val horizontalMargin = 10.dp
            val topMargin = 8.dp

            val canvasWidthDp = maxWidth - (horizontalMargin * 2)
            // Edit board height dynamically fills the available space right down to the bottom editor
            val canvasHeightDp = maxHeight - topMargin

            LaunchedEffect(canvasWidthDp, canvasHeightDp) {
                if (canvasWidthDp.value > 0f) {
                    canvasAspectRatio = canvasHeightDp.value / canvasWidthDp.value
                }
            }

            val density = LocalDensity.current
            val canvasWidthPx = with(density) { canvasWidthDp.toPx() }
            val pxToCanvas = 1000f / canvasWidthPx
            val factor = canvasWidthDp.value / 1000f

            // Read canvasVersion to ensure this canvas scope recomposes when layers mutate
            val currentVersion = canvasVersion

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Responsive Canvas Box extending right down to the bottom editor
                Box(
                    modifier = Modifier
                        .padding(top = topMargin)
                        .size(width = canvasWidthDp, height = canvasHeightDp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Tap outside layers to deselect
                                selectedLayerId = null
                            }
                        }
                ) {
                    // Draw background grid lines (matching iOS GridEditingView)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 40f * factor
                        val gridColor = Color(0xFFF2F2F2)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += gridSize
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += gridSize
                        }
                    }

                    // Render Canvas Layers
                    for (layer in layers) {
                        if (!layer.isVisible) continue
                        key(layer.id) {
                            CanvasLayerItem(
                                layer = layer,
                                isSelected = layer.id == selectedLayerId,
                                factor = factor,
                                pxToCanvas = pxToCanvas,
                                canvasVersion = canvasVersion,
                                onSelect = { selectedLayerId = layer.id },
                                onPushHistory = { pushHistory() },
                                onDelete = {
                                    layers.remove(layer)
                                    selectedLayerId = null
                                    canvasVersion++
                                },
                                onDuplicate = {
                                    val clone = layer.copyLayer(newId = true)
                                    clone.x += 30f
                                    clone.y += 30f
                                    layers.add(clone)
                                    selectedLayerId = clone.id
                                    canvasVersion++
                                },
                                onLockToggle = {
                                    layer.isLocked = !layer.isLocked
                                    canvasVersion++
                                },
                                onUpdate = { canvasVersion++ }
                            )
                        }
                    }
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF08B67D))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Exporting Cutfile Design...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Layers Bottom Sheet
    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layers (${layers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = { showLayersSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (layers.isEmpty()) {
                    Text(
                        text = "No layers yet. Add text, shapes, or cutfiles below.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(layers.reversed()) { revIndex, layer ->
                            val actualIndex = layers.size - 1 - revIndex
                            val isSelected = layer.id == selectedLayerId

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF9F9F9),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF08B67D)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLayerId = layer.id
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (layer) {
                                                is CanvasLayer.Text -> Icons.Default.TextFields
                                                is CanvasLayer.Shape -> Icons.Default.Category
                                                is CanvasLayer.Image -> Icons.Default.Image
                                            },
                                            contentDescription = null,
                                            tint = Color.DarkGray
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        val title = when (layer) {
                                            is CanvasLayer.Text -> "Text: \"${layer.text}\""
                                            is CanvasLayer.Shape -> "Shape: ${layer.shapeType}"
                                            is CanvasLayer.Image -> "Vector Image"
                                        }
                                        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Visibility toggle
                                        IconButton(onClick = {
                                            layer.isVisible = !layer.isVisible
                                            canvasVersion++
                                        }) {
                                            Icon(
                                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Visibility",
                                                tint = if (layer.isVisible) Color.DarkGray else Color.LightGray
                                            )
                                        }

                                        // Lock toggle
                                        IconButton(onClick = {
                                            layer.isLocked = !layer.isLocked
                                            canvasVersion++
                                        }) {
                                            Icon(
                                                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Toggle Lock",
                                                tint = if (layer.isLocked) Color(0xFFD32F2F) else Color.DarkGray
                                            )
                                        }

                                        // Move Down in stack
                                        if (actualIndex > 0) {
                                            IconButton(onClick = {
                                                pushHistory()
                                                val item = layers.removeAt(actualIndex)
                                                layers.add(actualIndex - 1, item)
                                                canvasVersion++
                                            }) {
                                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                            }
                                        }

                                        // Move Up in stack
                                        if (actualIndex < layers.size - 1) {
                                            IconButton(onClick = {
                                                pushHistory()
                                                val item = layers.removeAt(actualIndex)
                                                layers.add(actualIndex + 1, item)
                                                canvasVersion++
                                            }) {
                                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                            }
                                        }

                                        // Delete layer
                                        IconButton(onClick = {
                                            pushHistory()
                                            layers.removeAt(actualIndex)
                                            if (selectedLayerId == layer.id) selectedLayerId = null
                                            canvasVersion++
                                        }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Add Text Dialog
    if (showAddTextDialog) {
        var input by remember { mutableStateOf("New Text") }
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text(text = "Add Text Layer", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Text Content") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (input.trim().isNotEmpty()) {
                                    pushHistory()
                                    val newLayer = CanvasLayer.Text(
                                        id = UUID.randomUUID().toString(),
                                        x = 500f,
                                        y = 500f,
                                        text = input.trim(),
                                        fontSize = 60f,
                                        color = 0xFF1E1E1E
                                    )
                                    layers.add(newLayer)
                                    selectedLayerId = newLayer.id
                                    canvasVersion++
                                }
                                showAddTextDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D))
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddTextDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // 3. Edit Text Dialog
            if (showEditTextDialog && selectedLayer is CanvasLayer.Text) {
                var input by remember { mutableStateOf((selectedLayer as CanvasLayer.Text).text) }
                AlertDialog(
                    onDismissRequest = { showEditTextDialog = false },
                    title = { Text(text = "Edit Text", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            label = { Text("Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pushHistory()
                                (selectedLayer as CanvasLayer.Text).text = input
                                canvasVersion++
                                showEditTextDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D))
                        ) {
                            Text("Apply")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditTextDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // 4. Asset / Shape Picker Sheet (Cutfiles, Sublimation, Stickers, Watercolor, Shapes) - Mirroring iOS DataListVC
            activeCategoryPickerType?.let { type ->
                ModalBottomSheet(
                    onDismissRequest = { activeCategoryPickerType = null },
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose ${type.displayName}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            IconButton(onClick = { activeCategoryPickerType = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (type == CurrentDataType.Shapes) {
                            val shapes = listOf("circle", "rectangle", "rounded_rect", "heart", "star", "triangle", "diamond", "hexagon", "oval")
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                            ) {
                                items(shapes) { shape ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF7F8FA),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                        modifier = Modifier
                                            .height(96.dp)
                                            .clickable {
                                                pushHistory()
                                                val newShape = CanvasLayer.Shape(
                                                    id = UUID.randomUUID().toString(),
                                                    x = 500f,
                                                    y = 500f,
                                                    shapeType = shape,
                                                    width = 260f,
                                                    height = 260f,
                                                    fillColor = 0xFF00A94C
                                                )
                                                layers.add(newShape)
                                                selectedLayerId = newShape.id
                                                canvasVersion++
                                                activeCategoryPickerType = null
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = when (shape) {
                                                    "circle" -> Icons.Default.Circle
                                                    "rectangle" -> Icons.Default.CropSquare
                                                    "rounded_rect" -> Icons.Default.CropPortrait
                                                    "heart" -> Icons.Default.Favorite
                                                    "star" -> Icons.Default.Star
                                                    "triangle" -> Icons.Default.ChangeHistory
                                                    "diamond" -> Icons.Default.Diamond
                                                    "hexagon" -> Icons.Default.Hexagon
                                                    else -> Icons.Default.Category
                                                },
                                                contentDescription = shape,
                                                tint = Color(0xFF00A94C),
                                                modifier = Modifier.size(34.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = shape.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val categories = viewModel.getCurrentData(type) ?: emptyList()
                            var selectedCatIndex by remember { mutableIntStateOf(0) }
                            val activeCat = categories.getOrNull(selectedCatIndex)

                            // Horizontal Subcategories Bar (mirroring iOS categoryCV)
                            if (categories.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(categories) { idx, cat ->
                                        val isCatSelected = idx == selectedCatIndex
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isCatSelected) Color(0xFF00A94C) else Color(0xFFF2F2F7),
                                            modifier = Modifier.clickable { selectedCatIndex = idx }
                                        ) {
                                            Text(
                                                text = cat.nameInApp,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCatSelected) Color.White else Color.Black,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // 3-Column Items Grid (mirroring iOS logoCollectionView)
                            if (activeCat != null) {
                                val itemCount = activeCat.count.coerceAtLeast(1)
                                var downloadingIndex by remember { mutableStateOf<Int?>(null) }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp)
                                ) {
                                    items(itemCount) { itemIdx ->
                                        val thumbUrl = "${Constants.CLOUD_URL}Data/${type.getDirectory()}/${activeCat.s3Name}/Thumbnail/$itemIdx.png"
                                        val isDownloading = downloadingIndex == itemIdx
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF7F8FA),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                            modifier = Modifier
                                                .height(105.dp)
                                                .clickable(enabled = !isDownloading) {
                                                    scope.launch {
                                                        downloadingIndex = itemIdx
                                                        val svgUrl = "${Constants.CLOUD_URL}Data/${type.getDirectory()}/${activeCat.s3Name}/Svg/$itemIdx.svg"
                                                        val result = SVGDownloadManager.getSVGFile(type.getDirectory(), activeCat.s3Name, svgUrl)
                                                        downloadingIndex = null
                                                        result.onSuccess { file ->
                                                            pushHistory()
                                                            val newLayer = CanvasLayer.Image(
                                                                id = UUID.randomUUID().toString(),
                                                                x = 500f,
                                                                y = 500f,
                                                                urlOrPath = file.absolutePath,
                                                                width = 350f,
                                                                height = 350f
                                                            )
                                                            layers.add(newLayer)
                                                            selectedLayerId = newLayer.id
                                                            canvasVersion++
                                                            activeCategoryPickerType = null
                                                        }.onFailure {
                                                            Toast.makeText(context, "Could not load asset", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                                if (isDownloading) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF00A94C), strokeWidth = 2.dp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Color Picker Dialog
            if (showCustomColorPicker && selectedLayer != null) {
                val currentLayer = selectedLayer
                val initialCol = when (currentLayer) {
                    is CanvasLayer.Text -> Color(currentLayer.color)
                    is CanvasLayer.Shape -> {
                        if (customColorTarget == "stroke") Color(currentLayer.strokeColor ?: 0xFF000000)
                        else Color(currentLayer.fillColor)
                    }
                    is CanvasLayer.Image -> Color(currentLayer.tintColor ?: 0xFF00A94C)
                }
                ColorPickerDialog(
                    initialColor = initialCol,
                    onColorSelected = { picked: Color ->
                        pushHistory()
                        val colLong = (picked.value.toLong() shr 32) and 0xFFFFFFFFL
                        when (currentLayer) {
                            is CanvasLayer.Text -> currentLayer.color = colLong
                            is CanvasLayer.Shape -> {
                                if (customColorTarget == "stroke") {
                                    currentLayer.strokeColor = colLong
                                    if (currentLayer.strokeWidth == 0f) currentLayer.strokeWidth = 4f
                                } else {
                                    currentLayer.fillColor = colLong
                                }
                            }
                            is CanvasLayer.Image -> currentLayer.tintColor = colLong
                        }
                        canvasVersion++
                        showCustomColorPicker = false
                    },
                    onDismiss = { showCustomColorPicker = false }
                )
            }

    // 5. Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(text = "Discard Changes?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you certain you want to exit and discard all modifications?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
private fun CanvasLayerItem(
    layer: CanvasLayer,
    isSelected: Boolean,
    factor: Float,
    pxToCanvas: Float,
    canvasVersion: Int,
    onSelect: () -> Unit,
    onPushHistory: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onLockToggle: () -> Unit,
    onUpdate: () -> Unit
) {
    var localX by remember(layer.id) { mutableFloatStateOf(layer.x) }
    var localY by remember(layer.id) { mutableFloatStateOf(layer.y) }
    var localScale by remember(layer.id) { mutableFloatStateOf(layer.scaleX) }
    var localRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }

    LaunchedEffect(canvasVersion, layer.x, layer.y, layer.scaleX, layer.rotation) {
        localX = layer.x
        localY = layer.y
        localScale = layer.scaleX
        localRotation = layer.rotation
    }

    val layerWDp = (getLayerWidth(layer) * localScale * factor).dp
    val layerHDp = (getLayerHeight(layer) * localScale * factor).dp

    Box(
        modifier = Modifier
            .offset {
                val wPx = (getLayerWidth(layer) * localScale * factor).dp.roundToPx()
                val hPx = (getLayerHeight(layer) * localScale * factor).dp.roundToPx()
                val leftPx = (localX * factor).dp.roundToPx() - (wPx / 2)
                val topPx = (localY * factor).dp.roundToPx() - (hPx / 2)
                IntOffset(leftPx, topPx)
            }
            .size(width = layerWDp, height = layerHDp)
            .zIndex(if (isSelected) 10f else 0f)
            .graphicsLayer {
                rotationZ = localRotation
            }
    ) {
        // 1. Layer Content Container with tap to select and drag to move
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layer.id) {
                    detectTapGestures(
                        onTap = {
                            onSelect()
                            onUpdate()
                        }
                    )
                }
                .pointerInput(layer.id, layer.isLocked) {
                    if (!layer.isLocked) {
                        detectDragGestures(
                            onDragStart = {
                                onSelect()
                                onPushHistory()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                localX += dragAmount.x * pxToCanvas
                                localY += dragAmount.y * pxToCanvas
                                layer.x = localX
                                layer.y = localY
                                onUpdate()
                            }
                        )
                    }
                }
        ) {
            key(canvasVersion) {
                RenderLayerItem(layer = layer, factor = factor, canvasVersion = canvasVersion)
            }
        }

        // 2. Selection Border & Elevated Corner Action Handles when selected
        if (isSelected) {
            // Crisp selection border outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFF00A94C), RoundedCornerShape(8.dp))
            )

            if (!layer.isLocked) {
                // Top-Right: Delete handle (Elevated Red badge with white ring & X)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-16).dp)
                        .size(32.dp)
                        .zIndex(50f)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .border(2.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            onPushHistory()
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Bottom-Left: Duplicate handle (Elevated Green badge with white ring & copy icon)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-16).dp, y = 16.dp)
                        .size(32.dp)
                        .zIndex(50f)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF00A94C), CircleShape)
                        .border(2.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            onPushHistory()
                            onDuplicate()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Top-Left: Lock handle (Elevated dark gray badge with white ring & lock icon)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-16).dp, y = (-16).dp)
                        .size(32.dp)
                        .zIndex(50f)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF455A64), CircleShape)
                        .border(2.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            onPushHistory()
                            onLockToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Bottom-Right: Interactive Resize handle (Elevated Green badge with white ring & resize icon)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 16.dp)
                        .size(32.dp)
                        .zIndex(50f)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF00A94C), CircleShape)
                        .border(2.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .pointerInput(layer.id) {
                            detectDragGestures(
                                onDragStart = { onPushHistory() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val baseDim = maxOf(getLayerWidth(layer), getLayerHeight(layer))
                                    val deltaScale = (dragAmount.x + dragAmount.y) * pxToCanvas / maxOf(120f, baseDim)
                                    localScale = (localScale + deltaScale * 0.6f).coerceIn(0.2f, 4.0f)
                                    layer.scaleX = localScale
                                    layer.scaleY = localScale
                                    onUpdate()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                // Locked state: show Unlock badge at Top-Left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-16).dp, y = (-16).dp)
                        .size(32.dp)
                        .zIndex(50f)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .border(2.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            onPushHistory()
                            onLockToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderLayerItem(layer: CanvasLayer, factor: Float, canvasVersion: Int) {
    val _v = canvasVersion
    when (layer) {
        is CanvasLayer.Text -> {
            val font = when (layer.fontName.lowercase()) {
                "serif" -> FontFamily.Serif
                "sans-serif" -> FontFamily.SansSerif
                "monospace" -> FontFamily.Monospace
                "cursive" -> FontFamily.Cursive
                else -> FontFamily.Default
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = layer.text,
                    fontSize = (layer.fontSize * factor * layer.scaleX).sp,
                    fontFamily = font,
                    color = Color(layer.color),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        is CanvasLayer.Shape -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillPaint = Color(layer.fillColor)
                val strokePaint = layer.strokeColor?.let { Color(it) }
                val strokeWidthPx = layer.strokeWidth * factor
                val w = size.width
                val h = size.height
                when (layer.shapeType.lowercase()) {
                    "circle", "oval" -> {
                        drawOval(fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawOval(it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "rectangle" -> {
                        drawRect(fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawRect(it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "rounded_rect" -> {
                        drawRoundRect(fillPaint, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawRoundRect(it, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f), style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "diamond" -> {
                        val path = Path().apply {
                            moveTo(w / 2f, 0f)
                            lineTo(w, h / 2f)
                            lineTo(w / 2f, h)
                            lineTo(0f, h / 2f)
                            close()
                        }
                        drawPath(path, fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawPath(path, it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "hexagon" -> {
                        val path = Path().apply {
                            moveTo(w * 0.25f, 0f)
                            lineTo(w * 0.75f, 0f)
                            lineTo(w, h * 0.5f)
                            lineTo(w * 0.75f, h)
                            lineTo(w * 0.25f, h)
                            lineTo(0f, h * 0.5f)
                            close()
                        }
                        drawPath(path, fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawPath(path, it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "heart" -> {
                        val path = Path().apply {
                            moveTo(w / 2f, h * 0.85f)
                            cubicTo(-w * 0.1f, h * 0.4f, w * 0.1f, -h * 0.1f, w / 2f, h * 0.25f)
                            cubicTo(w * 0.9f, -h * 0.1f, w * 1.1f, h * 0.4f, w / 2f, h * 0.85f)
                            close()
                        }
                        drawPath(path, fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawPath(path, it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "star" -> {
                        val path = Path()
                        val outerR = minOf(w, h) / 2f
                        val innerR = outerR / 2.2f
                        val points = 5
                        val step = Math.PI / points
                        var angle = -Math.PI / 2
                        path.moveTo((w / 2f + outerR * Math.cos(angle)).toFloat(), (h / 2f + outerR * Math.sin(angle)).toFloat())
                        for (i in 0 until points) {
                            angle += step
                            path.lineTo((w / 2f + innerR * Math.cos(angle)).toFloat(), (h / 2f + innerR * Math.sin(angle)).toFloat())
                            angle += step
                            path.lineTo((w / 2f + outerR * Math.cos(angle)).toFloat(), (h / 2f + outerR * Math.sin(angle)).toFloat())
                        }
                        path.close()
                        drawPath(path, fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawPath(path, it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    "triangle" -> {
                        val path = Path().apply {
                            moveTo(w / 2f, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path, fillPaint)
                        strokePaint?.let {
                            if (strokeWidthPx > 0f) {
                                drawPath(path, it, style = Stroke(width = strokeWidthPx))
                            }
                        }
                    }
                    else -> {
                        drawRect(fillPaint)
                    }
                }
            }
        }
        is CanvasLayer.Image -> {
            AsyncImage(
                model = layer.urlOrPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = layer.tintColor?.let { ColorFilter.tint(Color(it), BlendMode.SrcIn) }
            )
        }
    }
}

enum class PropertyTab(val title: String, val iconRes: Int? = null, val imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Color("Color", iconRes = R.drawable.ic_edit_color),
    Stroke("Stroke", imageVector = Icons.Default.BorderColor),
    Size("Size", iconRes = R.drawable.ic_edit_size),
    Rotate("Rotate", iconRes = R.drawable.ic_edit_rotate),
    Position("Position", imageVector = Icons.Default.OpenWith),
    Font("Font", iconRes = R.drawable.ic_edit_font)
}

/**
 * Secondary tool ribbon providing dedicated, spacious controls for the selected layer.
 * Mirrors iOS ImageEditingView and TextEditingView tabs.
 */
@Composable
private fun LayerPropertiesBar(
    layer: CanvasLayer,
    canvasVersion: Int,
    onUpdate: () -> Unit,
    onPushHistory: () -> Unit,
    onEditContent: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDeselect: () -> Unit,
    onOpenColorPicker: (String) -> Unit
) {
    var localScale by remember(layer.id) { mutableFloatStateOf(layer.scaleX) }
    var localRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }

    LaunchedEffect(layer.scaleX, canvasVersion) {
        localScale = layer.scaleX
    }
    LaunchedEffect(layer.rotation, canvasVersion) {
        localRotation = layer.rotation
    }

    val availableTabs = remember(layer) {
        when (layer) {
            is CanvasLayer.Text -> listOf(PropertyTab.Color, PropertyTab.Font, PropertyTab.Size, PropertyTab.Rotate, PropertyTab.Position)
            is CanvasLayer.Shape -> listOf(PropertyTab.Color, PropertyTab.Stroke, PropertyTab.Size, PropertyTab.Rotate, PropertyTab.Position)
            else -> listOf(PropertyTab.Color, PropertyTab.Size, PropertyTab.Rotate, PropertyTab.Position)
        }
    }
    var selectedTab by remember(layer.id) { mutableStateOf(availableTabs.first()) }

    val presetColors = remember {
        listOf(
            0xFF000000, 0xFF333333, 0xFF666666, 0xFFFFFFFF,
            0xFF00A94C, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
            0xFFE53935, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF795548, 0xFF607D8B
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Row 1: Active Tool Content (Full width, spacious 52dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when (selectedTab) {
                PropertyTab.Color -> {
                    var colorTarget by remember(layer.id) { mutableStateOf(if (layer is CanvasLayer.Shape) "fill" else "color") }
                    val _version = canvasVersion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (layer is CanvasLayer.Shape) {
                            Row(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (colorTarget == "fill") Color(0xFF00A94C) else Color.Transparent,
                                    modifier = Modifier.clickable { colorTarget = "fill" }
                                ) {
                                    Text(
                                        text = "Fill",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colorTarget == "fill") Color.White else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (colorTarget == "stroke") Color(0xFF00A94C) else Color.Transparent,
                                    modifier = Modifier.clickable { colorTarget = "stroke" }
                                ) {
                                    Text(
                                        text = "Stroke",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colorTarget == "stroke") Color.White else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // 1. More Color / Palette picker button (mirroring iOS moreColor)
                            item {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { onOpenColorPicker(colorTarget) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_more_color),
                                            contentDescription = "More Colors",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Original / No tint circle
                            item {
                                val isResetSelected = when (layer) {
                                    is CanvasLayer.Image -> layer.tintColor == null
                                    is CanvasLayer.Text -> layer.color == 0xFF1E1E1E
                                    is CanvasLayer.Shape -> {
                                        if (colorTarget == "stroke") layer.strokeColor == null || layer.strokeWidth == 0f
                                        else layer.fillColor == 0xFF00A94C
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE))
                                        .border(
                                            width = if (isResetSelected) 2.5.dp else 1.dp,
                                            color = if (isResetSelected) Color(0xFF00A94C) else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onPushHistory()
                                            when (layer) {
                                                is CanvasLayer.Image -> layer.tintColor = null
                                                is CanvasLayer.Text -> layer.color = 0xFF1E1E1E
                                                is CanvasLayer.Shape -> {
                                                    if (colorTarget == "stroke") {
                                                        layer.strokeColor = null
                                                        layer.strokeWidth = 0f
                                                    } else {
                                                        layer.fillColor = 0xFF00A94C
                                                    }
                                                }
                                            }
                                            onUpdate()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "No Tint / Original",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // 3. Preset Palette Colors
                            items(presetColors, key = { it }) { colLong ->
                                val _v = canvasVersion
                                val isColSelected = when (layer) {
                                    is CanvasLayer.Image -> layer.tintColor == colLong
                                    is CanvasLayer.Text -> layer.color == colLong
                                    is CanvasLayer.Shape -> {
                                        if (colorTarget == "stroke") layer.strokeColor == colLong && layer.strokeWidth > 0f
                                        else layer.fillColor == colLong
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colLong))
                                        .border(
                                            width = if (isColSelected) 2.5.dp else 1.dp,
                                            color = if (isColSelected) Color(0xFF00A94C) else if (colLong == 0xFFFFFFFF) Color.LightGray else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onPushHistory()
                                            when (layer) {
                                                is CanvasLayer.Text -> layer.color = colLong
                                                is CanvasLayer.Shape -> {
                                                    if (colorTarget == "stroke") {
                                                        layer.strokeColor = colLong
                                                        if (layer.strokeWidth == 0f) layer.strokeWidth = 4f
                                                    } else {
                                                        layer.fillColor = colLong
                                                    }
                                                }
                                                is CanvasLayer.Image -> layer.tintColor = colLong
                                            }
                                            onUpdate()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colLong == 0xFFFFFFFF || colLong == 0xFFFFEB3B || colLong == 0xFFFFC107) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PropertyTab.Stroke -> {
                    if (layer is CanvasLayer.Shape) {
                        var localStrokeWidth by remember(layer.id) { mutableFloatStateOf(layer.strokeWidth) }
                        LaunchedEffect(canvasVersion, layer.strokeWidth) {
                            localStrokeWidth = layer.strokeWidth
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        onPushHistory()
                                        val newW = (layer.strokeWidth - 2f).coerceAtLeast(0f)
                                        layer.strokeWidth = newW
                                        localStrokeWidth = newW
                                        if (newW > 0f && layer.strokeColor == null) {
                                            layer.strokeColor = 0xFF000000
                                        }
                                        onUpdate()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            Slider(
                                value = localStrokeWidth,
                                onValueChange = {
                                    localStrokeWidth = it
                                    layer.strokeWidth = it
                                    if (it > 0f && layer.strokeColor == null) {
                                        layer.strokeColor = 0xFF000000
                                    }
                                    onUpdate()
                                },
                                onValueChangeFinished = {
                                    onPushHistory()
                                },
                                valueRange = 0f..24f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00A94C),
                                    activeTrackColor = Color(0xFF00A94C),
                                    inactiveTrackColor = Color(0xFFE5E5E5)
                                )
                            )

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        onPushHistory()
                                        val newW = (layer.strokeWidth + 2f).coerceAtMost(30f)
                                        layer.strokeWidth = newW
                                        localStrokeWidth = newW
                                        if (layer.strokeColor == null) {
                                            layer.strokeColor = 0xFF000000
                                        }
                                        onUpdate()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                Text(
                                    text = "${localStrokeWidth.toInt()}px",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                PropertyTab.Size -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    onPushHistory()
                                    val newScale = (layer.scaleX - 0.05f).coerceIn(0.2f, 4.0f)
                                    layer.scaleX = newScale
                                    layer.scaleY = newScale
                                    localScale = newScale
                                    onUpdate()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Slider(
                            value = localScale,
                            onValueChange = {
                                localScale = it
                                layer.scaleX = it
                                layer.scaleY = it
                                onUpdate()
                            },
                            onValueChangeFinished = {
                                onPushHistory()
                            },
                            valueRange = 0.2f..3.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00A94C),
                                activeTrackColor = Color(0xFF00A94C),
                                inactiveTrackColor = Color(0xFFE5E5E5)
                            )
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    onPushHistory()
                                    val newScale = (layer.scaleX + 0.05f).coerceIn(0.2f, 4.0f)
                                    layer.scaleX = newScale
                                    layer.scaleY = newScale
                                    localScale = newScale
                                    onUpdate()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5F5F5),
                            modifier = Modifier.clickable {
                                onPushHistory()
                                layer.scaleX = 1f
                                layer.scaleY = 1f
                                localScale = 1f
                                onUpdate()
                            }
                        ) {
                            Text(
                                text = "${(localScale * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                PropertyTab.Rotate -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier.clickable {
                                onPushHistory()
                                val newRot = (layer.rotation - 90f)
                                layer.rotation = newRot
                                localRotation = newRot
                                onUpdate()
                            }
                        ) {
                            Text(
                                text = "-90°",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Slider(
                            value = localRotation,
                            onValueChange = {
                                localRotation = it
                                layer.rotation = it
                                onUpdate()
                            },
                            onValueChangeFinished = {
                                onPushHistory()
                            },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00A94C),
                                activeTrackColor = Color(0xFF00A94C),
                                inactiveTrackColor = Color(0xFFE5E5E5)
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier.clickable {
                                onPushHistory()
                                val newRot = (layer.rotation + 90f)
                                layer.rotation = newRot
                                localRotation = newRot
                                onUpdate()
                            }
                        ) {
                            Text(
                                text = "+90°",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (localRotation != 0f) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                            border = if (localRotation != 0f) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A94C)) else null,
                            modifier = Modifier.clickable {
                                onPushHistory()
                                layer.rotation = 0f
                                localRotation = 0f
                                onUpdate()
                            }
                        ) {
                            Text(
                                text = "${localRotation.toInt()}°",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (localRotation != 0f) Color(0xFF00A94C) else Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                PropertyTab.Position -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Nudge", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier.size(36.dp).clickable { onPushHistory(); layer.x -= 20f; onUpdate() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left", modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier.size(36.dp).clickable { onPushHistory(); layer.x += 20f; onUpdate() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right", modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier.size(36.dp).clickable { onPushHistory(); layer.y -= 20f; onUpdate() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F0F0),
                                modifier = Modifier.size(36.dp).clickable { onPushHistory(); layer.y += 20f; onUpdate() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Down", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onPushHistory()
                                    layer.x = 500f
                                    onUpdate()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Center H", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onPushHistory()
                                    layer.y = 500f
                                    onUpdate()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Center V", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                PropertyTab.Font -> {
                    if (layer is CanvasLayer.Text) {
                        val _version = canvasVersion
                        val fonts = listOf("Roboto", "Serif", "Sans-Serif", "Monospace", "Cursive")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(fonts, key = { it }) { fontName ->
                                val _v = canvasVersion
                                val isFontSelected = layer.fontName.equals(fontName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isFontSelected) Color(0xFF00A94C) else Color(0xFFF0F0F0),
                                    modifier = Modifier.clickable {
                                        onPushHistory()
                                        layer.fontName = fontName
                                        onUpdate()
                                    }
                                ) {
                                    Text(
                                        text = fontName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isFontSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isFontSelected) Color.White else Color.Black,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Secondary Tool Navigation Tabs + Actions (Edit Text, Duplicate, Delete, Close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category Tabs with iOS Icons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(availableTabs) { tab ->
                    val isTabSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTabSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                        border = if (isTabSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00A94C)) else null,
                        modifier = Modifier
                            .height(34.dp)
                            .clickable { selectedTab = tab }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            if (tab.iconRes != null) {
                                Image(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(15.dp),
                                    colorFilter = ColorFilter.tint(if (isTabSelected) Color(0xFF00A94C) else Color.DarkGray)
                                )
                            } else if (tab.imageVector != null) {
                                Icon(
                                    imageVector = tab.imageVector,
                                    contentDescription = tab.title,
                                    tint = if (isTabSelected) Color(0xFF00A94C) else Color.DarkGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTabSelected) Color(0xFF00A94C) else Color.Black,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (layer is CanvasLayer.Text) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(32.dp).clickable { onEditContent() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Text", tint = Color(0xFF00A94C), modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.size(32.dp).clickable { onDuplicate() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.Black, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.size(32.dp).clickable { onDelete() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(32.dp).clickable { onDeselect() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF00A94C), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/**
 * Modern Full Color Picker Dialog supporting RGB sliders, hex representation,
 * and quick preset swatches. Mirrors iOS UIColorPickerViewController.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255f) }

    val currentColor = Color(
        red = (red / 255f).coerceIn(0f, 1f),
        green = (green / 255f).coerceIn(0f, 1f),
        blue = (blue / 255f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val quickColors = remember {
        listOf(
            0xFF000000, 0xFFFFFFFF, 0xFFE53935, 0xFFE91E63,
            0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3,
            0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF00A94C,
            0xFF8BC34A, 0xFFFFEB3B, 0xFFFF9800, 0xFFFF5722
        )
    }

    val hexString = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Color Palette", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "Hex Code: $hexString", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "R", fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.width(20.dp))
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                    )
                    Text(text = "${red.toInt()}", fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "G", fontWeight = FontWeight.Bold, color = Color(0xFF00A94C), modifier = Modifier.width(20.dp))
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00A94C), activeTrackColor = Color(0xFF00A94C))
                    )
                    Text(text = "${green.toInt()}", fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "B", fontWeight = FontWeight.Bold, color = Color.Blue, modifier = Modifier.width(20.dp))
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
                    )
                    Text(text = "${blue.toInt()}", fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text(text = "Presets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickColors) { cLong ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(cLong))
                                .border(1.dp, if (cLong == 0xFFFFFFFF) Color.LightGray else Color.Transparent, CircleShape)
                                .clickable {
                                    red = ((cLong shr 16) and 0xFF).toFloat()
                                    green = ((cLong shr 8) and 0xFF).toFloat()
                                    blue = (cLong and 0xFF).toFloat()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A94C))
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getLayerWidth(layer: CanvasLayer): Float = when (layer) {
    is CanvasLayer.Text -> maxOf(200f, layer.text.length * layer.fontSize * 0.65f)
    is CanvasLayer.Shape -> layer.width
    is CanvasLayer.Image -> layer.width
}

private fun getLayerHeight(layer: CanvasLayer): Float = when (layer) {
    is CanvasLayer.Text -> layer.fontSize * 1.8f
    is CanvasLayer.Shape -> layer.height
    is CanvasLayer.Image -> layer.height
}

/**
 * Robust JSON serializer and deserializer for CanvasLayer drafts.
 */
object LayerJsonHelper {
    fun serialize(layers: List<CanvasLayer>): String {
        val array = JsonArray()
        for (layer in layers) {
            val obj = JsonObject()
            obj.addProperty("id", layer.id)
            obj.addProperty("x", layer.x)
            obj.addProperty("y", layer.y)
            obj.addProperty("scaleX", layer.scaleX)
            obj.addProperty("scaleY", layer.scaleY)
            obj.addProperty("rotation", layer.rotation)
            obj.addProperty("isVisible", layer.isVisible)
            obj.addProperty("isLocked", layer.isLocked)
            when (layer) {
                is CanvasLayer.Text -> {
                    obj.addProperty("type", "text")
                    obj.addProperty("text", layer.text)
                    obj.addProperty("fontSize", layer.fontSize)
                    obj.addProperty("fontName", layer.fontName)
                    obj.addProperty("color", layer.color)
                }
                is CanvasLayer.Image -> {
                    obj.addProperty("type", "image")
                    obj.addProperty("urlOrPath", layer.urlOrPath)
                    obj.addProperty("width", layer.width)
                    obj.addProperty("height", layer.height)
                    layer.tintColor?.let { obj.addProperty("tintColor", it) }
                }
                is CanvasLayer.Shape -> {
                    obj.addProperty("type", "shape")
                    obj.addProperty("shapeType", layer.shapeType)
                    obj.addProperty("width", layer.width)
                    obj.addProperty("height", layer.height)
                    obj.addProperty("fillColor", layer.fillColor)
                    layer.strokeColor?.let { obj.addProperty("strokeColor", it) }
                    obj.addProperty("strokeWidth", layer.strokeWidth)
                }
            }
            array.add(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String): List<CanvasLayer> {
        val list = mutableListOf<CanvasLayer>()
        try {
            val array = JsonParser.parseString(json).asJsonArray
            for (element in array) {
                val obj = element.asJsonObject
                val id = obj.get("id")?.asString ?: UUID.randomUUID().toString()
                val x = obj.get("x")?.asFloat ?: 500f
                val y = obj.get("y")?.asFloat ?: 500f
                val scaleX = obj.get("scaleX")?.asFloat ?: 1f
                val scaleY = obj.get("scaleY")?.asFloat ?: 1f
                val rotation = obj.get("rotation")?.asFloat ?: 0f
                val isVisible = obj.get("isVisible")?.asBoolean ?: true
                val isLocked = obj.get("isLocked")?.asBoolean ?: false
                val type = obj.get("type")?.asString ?: "image"

                when (type) {
                    "text" -> {
                        list.add(
                            CanvasLayer.Text(
                                id = id, x = x, y = y, scaleX = scaleX, scaleY = scaleY,
                                rotation = rotation, isVisible = isVisible, isLocked = isLocked,
                                text = obj.get("text")?.asString ?: "",
                                fontSize = obj.get("fontSize")?.asFloat ?: 48f,
                                fontName = obj.get("fontName")?.asString ?: "Roboto",
                                color = obj.get("color")?.asLong ?: 0xFF000000
                            )
                        )
                    }
                    "shape" -> {
                        list.add(
                            CanvasLayer.Shape(
                                id = id, x = x, y = y, scaleX = scaleX, scaleY = scaleY,
                                rotation = rotation, isVisible = isVisible, isLocked = isLocked,
                                shapeType = obj.get("shapeType")?.asString ?: "circle",
                                width = obj.get("width")?.asFloat ?: 200f,
                                height = obj.get("height")?.asFloat ?: 200f,
                                fillColor = obj.get("fillColor")?.asLong ?: 0xFF08B67D,
                                strokeColor = if (obj.has("strokeColor") && !obj.get("strokeColor").isJsonNull) obj.get("strokeColor").asLong else null,
                                strokeWidth = obj.get("strokeWidth")?.asFloat ?: 0f
                            )
                        )
                    }
                    else -> {
                        list.add(
                            CanvasLayer.Image(
                                id = id, x = x, y = y, scaleX = scaleX, scaleY = scaleY,
                                rotation = rotation, isVisible = isVisible, isLocked = isLocked,
                                urlOrPath = obj.get("urlOrPath")?.asString ?: "",
                                width = obj.get("width")?.asFloat ?: 300f,
                                height = obj.get("height")?.asFloat ?: 300f,
                                tintColor = if (obj.has("tintColor") && !obj.get("tintColor").isJsonNull) obj.get("tintColor").asLong else null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
