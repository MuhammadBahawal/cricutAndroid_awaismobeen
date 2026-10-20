package com.cricutandroid.app.models

import com.google.gson.annotations.SerializedName

/**
 * Root model for app data loaded from JSON / CloudFront.
 * Mirrors the iOS `AppDataModel`.
 */
data class AppDataModel(
    @SerializedName("CutFiles") val cutFiles: List<AppDataItem> = emptyList(),
    @SerializedName("Monograms") val monograms: List<AppDataItem> = emptyList(),
    @SerializedName("Shapes") val shapes: List<AppDataItem> = emptyList(),
    @SerializedName("WaterColour") val waterColour: List<AppDataItem> = emptyList(),
    @SerializedName("Stickers") val stickers: List<AppDataItem> = emptyList(),
    @SerializedName("Sublimations") val sublimations: List<AppDataItem> = emptyList(),
    @SerializedName("crafts") val crafts: List<Craft> = emptyList(),
    @SerializedName("Tshirt", alternate = ["Tshirts", "TShirts", "tshirt", "tshirts", "T-Shirts"]) val tshirt: Int = 30,
    @SerializedName("Caps", alternate = ["caps", "Cap", "capsCV"]) val caps: Int = 30,
    @SerializedName("Mugs", alternate = ["mugs", "Mug"]) val mugs: Int = 30,
    @SerializedName("3DSticker", alternate = ["threedSticker", "ThreeDSticker", "3dSticker"]) val threeDSticker: Int = 30
) {
    fun merged(fallback: AppDataModel, fallbackCrafts: List<Craft>): AppDataModel = AppDataModel(
        cutFiles = cutFiles.ifEmpty { fallback.cutFiles },
        monograms = monograms.ifEmpty { fallback.monograms },
        shapes = shapes.ifEmpty { fallback.shapes },
        waterColour = waterColour.ifEmpty { fallback.waterColour },
        stickers = stickers.ifEmpty { fallback.stickers },
        sublimations = sublimations.ifEmpty { fallback.sublimations },
        crafts = crafts.ifEmpty { fallbackCrafts },
        tshirt = if (tshirt == 0) fallback.tshirt else tshirt,
        caps = if (caps == 0) fallback.caps else caps,
        mugs = if (mugs == 0) fallback.mugs else mugs,
        threeDSticker = if (threeDSticker == 0) fallback.threeDSticker else threeDSticker
    )
}

/**
 * Individual data item within a category.
 * Mirrors the iOS `AppDataItem`.
 */
data class AppDataItem(
    val count: Int = 0,
    val nameInApp: String = "",
    val s3Name: String = "",
    val isNew: Boolean = false
)

/**
 * Wrapper used when parsing Crafts.json.
 * Mirrors the iOS `CraftsDataModel`.
 */
data class CraftsDataModel(
    val crafts: List<Craft> = emptyList()
)

/**
 * Individual craft model.
 * Mirrors the iOS `Craft`.
 */
data class Craft(
    val count: Int = 0,
    val nameInApp: String = "",
    val s3Name: String = "",
    val description: String = "",
    val formatFile: String = "",
    val assemble: String = "",
    val thumbnailCount: String? = null,
    val isNew: Boolean = false
)

/**
 * Vector Canvas Layer Model for saving/loading drafts and editing in Vector Canvas.
 */
sealed class CanvasLayer(
    open val id: String,
    open var x: Float,
    open var y: Float,
    open var scaleX: Float = 1f,
    open var scaleY: Float = 1f,
    open var rotation: Float = 0f,
    open var isVisible: Boolean = true,
    open var isLocked: Boolean = false
) {
    abstract fun copyLayer(newId: Boolean = false): CanvasLayer

    data class Text(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var scaleX: Float = 1f,
        override var scaleY: Float = 1f,
        override var rotation: Float = 0f,
        override var isVisible: Boolean = true,
        override var isLocked: Boolean = false,
        var text: String,
        var fontSize: Float = 32f,
        var fontName: String = "Roboto",
        var color: Long = 0xFF000000
    ) : CanvasLayer(id, x, y, scaleX, scaleY, rotation, isVisible, isLocked) {
        override fun copyLayer(newId: Boolean): CanvasLayer =
            copy(id = if (newId) java.util.UUID.randomUUID().toString() else id)
    }

    data class Image(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var scaleX: Float = 1f,
        override var scaleY: Float = 1f,
        override var rotation: Float = 0f,
        override var isVisible: Boolean = true,
        override var isLocked: Boolean = false,
        var urlOrPath: String,
        var width: Float = 200f,
        var height: Float = 200f,
        var tintColor: Long? = null
    ) : CanvasLayer(id, x, y, scaleX, scaleY, rotation, isVisible, isLocked) {
        override fun copyLayer(newId: Boolean): CanvasLayer =
            copy(id = if (newId) java.util.UUID.randomUUID().toString() else id)
    }

    data class Shape(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var scaleX: Float = 1f,
        override var scaleY: Float = 1f,
        override var rotation: Float = 0f,
        override var isVisible: Boolean = true,
        override var isLocked: Boolean = false,
        var shapeType: String = "circle", // circle, rectangle, heart, star, triangle, hexagon
        var width: Float = 180f,
        var height: Float = 180f,
        var fillColor: Long = 0xFF08B67D,
        var strokeColor: Long? = null,
        var strokeWidth: Float = 0f
    ) : CanvasLayer(id, x, y, scaleX, scaleY, rotation, isVisible, isLocked) {
        override fun copyLayer(newId: Boolean): CanvasLayer =
            copy(id = if (newId) java.util.UUID.randomUUID().toString() else id)
    }
}

/**
 * Saved Project / Draft model mirroring iOS DraftVC.
 */
data class DraftProject(
    val id: String,
    val title: String,
    val timestamp: Long,
    val previewImagePath: String,
    val layersJson: String
)
