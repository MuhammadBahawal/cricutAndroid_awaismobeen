package com.cricutandroid.app.managers

/**
 * Android equivalent of iOS StorekitManager.
 *
 * For a fully working implementation you'd integrate Google Play BillingClient here.
 * This skeleton mirrors the same API surface used throughout the iOS app,
 * storing premium status via [PrefsHelper] (like the iOS KeychainHelper approach).
 */
object BillingManager {
    private const val PREF_KEY_UNLOCKED = "UnlockAI"

    val productIds = listOf(
        "com.weekly.craft",
        "com.monthly.craft",
        "com.yearly.craft",
        "com.crafts.lifetime.app"
    )

    /** Whether the user has an active premium subscription / purchase. */
    fun isProUser(): Boolean = PrefsHelper.getBool(PREF_KEY_UNLOCKED)

    /** Call after successful purchase verification to unlock premium. */
    fun setPremiumStatus(unlocked: Boolean) {
        val current = isProUser()
        if (current != unlocked) {
            PrefsHelper.saveBool(PREF_KEY_UNLOCKED, unlocked)
        }
    }
}
