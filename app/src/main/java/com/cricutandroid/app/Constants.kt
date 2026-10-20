package com.cricutandroid.app

/**
 * Global constants ported from the iOS Constants.swift.
 */
object Constants {
    const val CLOUD_URL = "https://dehgajvsdmyp.cloudfront.net/"
    const val EMAIL_ADDRESS = "contactawaismobeen@gmail.com"
    const val PRIVACY_POLICY = "https://sites.google.com/view/amprivacy/privacy-policy"
    const val TERMS_OF_USE = "https://sites.google.com/view/amterms/termsofuse"
    const val APP_GREEN = 0xFF08B67D.toInt()
}

/**
 * Maps iOS `CurrentDataType` enum.
 */
enum class CurrentDataType(val displayName: String) {
    Cutfiles("Cut Files"),
    Monograms("Monograms"),
    Watercolors("Watercolors"),
    Stickers("Stickers"),
    Shapes("Shapes"),
    Sublimation("Sublimations"),
    Fonts("Fonts"),
    MonogramFonts("Monogram Fonts"),
    Craft("Crafts"),
    Tshirts("T-Shirts"),
    Mugs("Mugs"),
    ThreeDSticker("3D Stickers"),
    Caps("Caps");

    fun getDirectory(): String = directoryCandidates().first()

    fun directoryCandidates(): List<String> = when (this) {
        Cutfiles -> listOf("CutFiles")
        Monograms -> listOf("Monograms")
        Watercolors -> listOf("WaterColour")
        Stickers -> listOf("Stickers")
        Shapes -> listOf("Shapes")
        Sublimation -> listOf("Sublimations")
        Fonts -> listOf("Fonts")
        MonogramFonts -> listOf("MonogramFonts")
        Craft -> listOf("Crafts")
        Tshirts -> listOf("TShirts", "Tshirt", "TShirt", "Tshirts")
        Mugs -> listOf("Mugs", "mugs")
        ThreeDSticker -> listOf("3DSticker", "3dSticker", "3DStickers")
        Caps -> listOf("Caps", "caps")
    }
}
