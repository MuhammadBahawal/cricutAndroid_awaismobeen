package com.cricutandroid.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.cricutandroid.app.CurrentDataType
import com.cricutandroid.app.managers.SVGDownloadManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Preview / Action Modal Popup mirroring iOS EditSaveVC.
 */
@Composable
fun EditSaveDialog(
    mainCategory: String,
    categoryName: String,
    index: Int,
    previewUrl: String,
    isProItem: Boolean,
    isProUser: Boolean,
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit,
    onEditSvg: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val svgUrl = if (mainCategory.equals(categoryName, ignoreCase = true)) {
        "${com.cricutandroid.app.Constants.CLOUD_URL}Data/$mainCategory/Svg/$index.svg"
    } else {
        "${com.cricutandroid.app.Constants.CLOUD_URL}Data/$mainCategory/$categoryName/Svg/$index.svg"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
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

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Edit Button
                    Button(
                        onClick = {
                            if (isProItem && !isProUser) {
                                onOpenPaywall()
                            } else {
                                scope.launch {
                                    isLoading = true
                                    val result = SVGDownloadManager.getSVGFile(mainCategory, categoryName, svgUrl)
                                    isLoading = false
                                    result.onSuccess { file ->
                                        onDismiss()
                                        onEditSvg(file)
                                    }.onFailure {
                                        Toast.makeText(context, "Failed to load SVG", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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
                            text = "Edit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Download/Save Button
                    OutlinedButton(
                        onClick = {
                            if (isProItem && !isProUser) {
                                onOpenPaywall()
                            } else {
                                scope.launch {
                                    isLoading = true
                                    val result = SVGDownloadManager.getSVGFile(mainCategory, categoryName, svgUrl)
                                    isLoading = false
                                    result.onSuccess { file ->
                                        Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    }.onFailure {
                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF08B67D)),
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
}
