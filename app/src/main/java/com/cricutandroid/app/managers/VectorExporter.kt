package com.cricutandroid.app.managers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.cricutandroid.app.models.CanvasLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Handles rendering canvas layers to Bitmaps, generating SVG XML documents,
 * saving images to device Gallery (MediaStore), exporting SVGs, sharing, and printing.
 * Mirrors iOS SVGExporter, SaveVC, and performance Bezier helpers.
 */
object VectorExporter {

    /**
     * Converts canvas layers to a valid SVG XML document.
     */
    fun generateSvgString(
        layers: List<CanvasLayer>,
        width: Int = 1000,
        height: Int = 1000
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" """)
        sb.append("""width="$width" height="$height" viewBox="0 0 $width $height">""").append("\n")

        for (layer in layers) {
            if (!layer.isVisible) continue
            val transform = """transform="translate(${layer.x}, ${layer.y}) rotate(${layer.rotation}) scale(${layer.scaleX}, ${layer.scaleY})""""

            when (layer) {
                is CanvasLayer.Text -> {
                    val colorHex = String.format("#%06X", (0xFFFFFFL and layer.color))
                    val escapedText = escapeXml(layer.text)
                    sb.append("""  <g $transform>""").append("\n")
                    sb.append("""    <text x="0" y="0" font-size="${layer.fontSize}" font-family="${layer.fontName}" fill="$colorHex" text-anchor="middle" dominant-baseline="central">$escapedText</text>""").append("\n")
                    sb.append("""  </g>""").append("\n")
                }
                is CanvasLayer.Shape -> {
                    val fillHex = String.format("#%06X", (0xFFFFFFL and layer.fillColor))
                    val strokeAttr = layer.strokeColor?.let {
                        val sHex = String.format("#%06X", (0xFFFFFFL and it))
                        """stroke="$sHex" stroke-width="${layer.strokeWidth}""""
                    } ?: ""
                    val w = layer.width
                    val h = layer.height

                    sb.append("""  <g $transform>""").append("\n")
                    when (layer.shapeType.lowercase()) {
                        "circle" -> {
                            val r = minOf(w, h) / 2f
                            sb.append("""    <circle cx="0" cy="0" r="$r" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                        "rectangle" -> {
                            sb.append("""    <rect x="${-w/2}" y="${-h/2}" width="$w" height="$h" rx="8" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                        "heart" -> {
                            val pathD = "M 0,${h * 0.35f} C ${-w * 0.5f},${-h * 0.2f} ${-w * 0.5f},${-h * 0.5f} 0,${-h * 0.15f} C ${w * 0.5f},${-h * 0.5f} ${w * 0.5f},${-h * 0.2f} 0,${h * 0.35f} Z"
                            sb.append("""    <path d="$pathD" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                        "star" -> {
                            val starPath = generateStarSvgPath(w / 2f, w / 4f, 5)
                            sb.append("""    <polygon points="$starPath" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                        "triangle" -> {
                            val points = "0,${-h/2} ${w/2},${h/2} ${-w/2},${h/2}"
                            sb.append("""    <polygon points="$points" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                        else -> {
                            sb.append("""    <rect x="${-w/2}" y="${-h/2}" width="$w" height="$h" fill="$fillHex" $strokeAttr />""").append("\n")
                        }
                    }
                    sb.append("""  </g>""").append("\n")
                }
                is CanvasLayer.Image -> {
                    sb.append("""  <g $transform>""").append("\n")
                    sb.append("""    <image href="${layer.urlOrPath}" x="${-layer.width/2}" y="${-layer.height/2}" width="${layer.width}" height="${layer.height}" />""").append("\n")
                    sb.append("""  </g>""").append("\n")
                }
            }
        }
        sb.append("</svg>")
        return sb.toString()
    }

    /**
     * Renders layers to a Bitmap asynchronously, loading remote/cached images via Coil.
     */
    suspend fun renderToBitmap(
        context: Context,
        layers: List<CanvasLayer>,
        width: Int = 1000,
        height: Int = 1000,
        backgroundColor: Int? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (backgroundColor != null) {
            canvas.drawColor(backgroundColor)
        }

        val imageLoader = ImageLoader(context)

        for (layer in layers) {
            if (!layer.isVisible) continue

            canvas.save()
            canvas.translate(layer.x, layer.y)
            canvas.rotate(layer.rotation)
            canvas.scale(layer.scaleX, layer.scaleY)

            when (layer) {
                is CanvasLayer.Text -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.color.toInt()
                        textSize = layer.fontSize
                        textAlign = Paint.Align.CENTER
                        typeface = loadTypeface(context, layer.fontName)
                    }
                    // Adjust y for center alignment baseline
                    val fontMetrics = paint.fontMetrics
                    val yOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
                    canvas.drawText(layer.text, 0f, -yOffset, paint)
                }
                is CanvasLayer.Shape -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.fillColor.toInt()
                        style = Paint.Style.FILL
                    }
                    val strokePaint = layer.strokeColor?.let {
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = it.toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = layer.strokeWidth
                        }
                    }

                    val w = layer.width
                    val h = layer.height

                    when (layer.shapeType.lowercase()) {
                        "circle" -> {
                            val r = minOf(w, h) / 2f
                            canvas.drawCircle(0f, 0f, r, paint)
                            strokePaint?.let { canvas.drawCircle(0f, 0f, r, it) }
                        }
                        "rectangle" -> {
                            val rect = RectF(-w / 2, -h / 2, w / 2, h / 2)
                            canvas.drawRoundRect(rect, 16f, 16f, paint)
                            strokePaint?.let { canvas.drawRoundRect(rect, 16f, 16f, it) }
                        }
                        "heart" -> {
                            val path = Path().apply {
                                moveTo(0f, h * 0.35f)
                                cubicTo(-w * 0.5f, -h * 0.2f, -w * 0.5f, -h * 0.5f, 0f, -h * 0.15f)
                                cubicTo(w * 0.5f, -h * 0.5f, w * 0.5f, -h * 0.2f, 0f, h * 0.35f)
                                close()
                            }
                            canvas.drawPath(path, paint)
                            strokePaint?.let { canvas.drawPath(path, it) }
                        }
                        "star" -> {
                            val path = createStarPath(w / 2f, w / 4f, 5)
                            canvas.drawPath(path, paint)
                            strokePaint?.let { canvas.drawPath(path, it) }
                        }
                        "triangle" -> {
                            val path = Path().apply {
                                moveTo(0f, -h / 2f)
                                lineTo(w / 2f, h / 2f)
                                lineTo(-w / 2f, h / 2f)
                                close()
                            }
                            canvas.drawPath(path, paint)
                            strokePaint?.let { canvas.drawPath(path, it) }
                        }
                        else -> {
                            val rect = RectF(-w / 2, -h / 2, w / 2, h / 2)
                            canvas.drawRect(rect, paint)
                            strokePaint?.let { canvas.drawRect(rect, it) }
                        }
                    }
                }
                is CanvasLayer.Image -> {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(layer.urlOrPath)
                            .build()
                        val result = imageLoader.execute(request)
                        val imgBitmap = result.image?.toBitmap()
                        if (imgBitmap != null) {
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                            if (layer.tintColor != null) {
                                paint.colorFilter = PorterDuffColorFilter(
                                    layer.tintColor!!.toInt(),
                                    PorterDuff.Mode.SRC_IN
                                )
                            }
                            val destRect = RectF(
                                -layer.width / 2f,
                                -layer.height / 2f,
                                layer.width / 2f,
                                layer.height / 2f
                            )
                            canvas.drawBitmap(imgBitmap, null, destRect, paint)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            canvas.restore()
        }

        bitmap
    }

    /**
     * Saves a Bitmap to the user's Gallery (MediaStore).
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        filename: String
    ): Uri? {
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val fullFilename = "$filename.$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFilename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cricut")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { stream: OutputStream ->
                val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 92
                bitmap.compress(format, quality, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }

        return uri
    }

    /**
     * Saves SVG string to Downloads or local file.
     */
    fun saveSvgToFile(context: Context, svgString: String, filename: String): File {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "$filename.svg")
        FileOutputStream(file).use {
            it.write(svgString.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Shares a bitmap via Android Share Sheet.
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, title: String = "Share Design") {
        try {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "cricut_export_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Prints a bitmap using Android PrintHelper.
     */
    fun printBitmap(context: Context, bitmap: Bitmap, jobName: String = "Cricut Design") {
        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap(jobName, bitmap)
    }

    private fun loadTypeface(context: Context, fontName: String): Typeface {
        return try {
            val fontAssets = context.assets.list("fonts") ?: emptyArray()
            val match = fontAssets.firstOrNull { it.contains(fontName, ignoreCase = true) }
            if (match != null) {
                Typeface.createFromAsset(context.assets, "fonts/$match")
            } else {
                when (fontName.lowercase()) {
                    "serif" -> Typeface.SERIF
                    "sans-serif" -> Typeface.SANS_SERIF
                    "monospace" -> Typeface.MONOSPACE
                    else -> Typeface.DEFAULT
                }
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun createStarPath(outerRadius: Float, innerRadius: Float, points: Int): Path {
        val path = Path()
        val step = Math.PI / points
        var angle = -Math.PI / 2
        path.moveTo((outerRadius * Math.cos(angle)).toFloat(), (outerRadius * Math.sin(angle)).toFloat())

        for (i in 0 until points) {
            angle += step
            path.lineTo((innerRadius * Math.cos(angle)).toFloat(), (innerRadius * Math.sin(angle)).toFloat())
            angle += step
            path.lineTo((outerRadius * Math.cos(angle)).toFloat(), (outerRadius * Math.sin(angle)).toFloat())
        }
        path.close()
        return path
    }

    private fun generateStarSvgPath(outerRadius: Float, innerRadius: Float, points: Int): String {
        val sb = StringBuilder()
        val step = Math.PI / points
        var angle = -Math.PI / 2
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = (r * Math.cos(angle)).toFloat()
            val y = (r * Math.sin(angle)).toFloat()
            sb.append(String.format("%.1f,%.1f ", x, y))
            angle += step
        }
        return sb.toString().trim()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
