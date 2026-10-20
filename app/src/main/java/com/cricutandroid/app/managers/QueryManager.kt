package com.cricutandroid.app.managers

/**
 * Mirrors the iOS QueryManager. Limits free-tier AI queries to [maxQueries].
 */
object QueryManager {
    private const val maxQueries = 2
    private var usedQueries = 0
    private val lock = Any()

    fun canProceed(): Boolean = synchronized(lock) { usedQueries < maxQueries }

    fun consumeQuery(): Boolean = synchronized(lock) {
        if (usedQueries < maxQueries) {
            usedQueries++
            true
        } else false
    }

    fun remainingQueries(): Int = synchronized(lock) { maxQueries - usedQueries }

    fun reset() = synchronized(lock) { usedQueries = 0 }
}
