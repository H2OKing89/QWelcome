package com.kingpaging.qwelcome.data

import android.util.Log

private const val TAG = "VersionComparator"

/**
 * SemVer-aware version comparison.
 *
 * Handles base version comparison (major.minor.patch) and pre-release
 * suffixes per Semantic Versioning 2.0 rules:
 * - A version without a pre-release suffix has higher precedence than the
 *   same base with a pre-release suffix (1.0.0 > 1.0.0-beta).
 * - Pre-release identifiers are compared left-to-right, split on `.`.
 * - Numeric identifiers are compared as integers; string identifiers are
 *   compared lexicographically (ASCII sort).
 * - Numeric identifiers have lower precedence than string identifiers.
 * - A shorter set of identifiers has lower precedence when all preceding
 *   identifiers are equal.
 */
object VersionComparator {

    /**
     * Returns `true` when [remote] is a newer version than [current].
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        val (remoteBase, remotePre) = splitVersion(remote)
        val (currentBase, currentPre) = splitVersion(current)

        val remoteParts = parseVersionParts(remoteBase)
        val currentParts = parseVersionParts(currentBase)

        if (remoteParts.isEmpty() || currentParts.isEmpty()) {
            Log.w(TAG, "Invalid version format - remote: '$remote', current: '$current'")
            return false
        }

        // Compare numeric base parts
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // Base versions are equal — apply pre-release precedence rules
        return comparePreRelease(remotePre, currentPre) > 0
    }

    // ---- internal helpers (visible for testing) ----

    /**
     * Compare two optional pre-release strings according to SemVer §11.
     *
     * Returns positive if [remotePre] has higher precedence, negative if
     * lower, or zero if equal.
     *
     * Rules when bases are equal:
     * - stable (null) > any pre-release
     * - both null → 0
     * - both non-null → identifier-by-identifier comparison
     */
    internal fun comparePreRelease(remotePre: String?, currentPre: String?): Int {
        // Both stable → equal
        if (remotePre == null && currentPre == null) return 0
        // Stable beats pre-release
        if (remotePre == null && currentPre != null) return 1
        if (remotePre != null && currentPre == null) return -1

        // Both have pre-release — compare identifiers
        val remoteIds = remotePre!!.split(".")
        val currentIds = currentPre!!.split(".")

        val len = minOf(remoteIds.size, currentIds.size)
        for (i in 0 until len) {
            val cmp = compareIdentifiers(remoteIds[i], currentIds[i])
            if (cmp != 0) return cmp
        }

        // All compared identifiers equal — longer set has higher precedence
        return remoteIds.size.compareTo(currentIds.size)
    }

    /**
     * Compare two individual pre-release identifiers.
     *
     * - Both numeric → numeric comparison
     * - Both non-numeric → lexicographic (ASCII)
     * - Numeric has *lower* precedence than non-numeric
     */
    private fun compareIdentifiers(a: String, b: String): Int {
        val aNum = a.toLongOrNull()
        val bNum = b.toLongOrNull()

        return when {
            aNum != null && bNum != null -> aNum.compareTo(bNum)
            aNum != null -> -1  // numeric < string
            bNum != null -> 1   // string > numeric
            else -> a.compareTo(b)  // both strings
        }
    }

    /**
     * Parse version string into numeric parts.
     * Returns empty list if any segment is non-numeric.
     */
    internal fun parseVersionParts(versionBase: String): List<Int> {
        val segments = versionBase.split(".").filter { it.isNotEmpty() }
        val parsed = segments.map { it.toIntOrNull() }
        return if (parsed.any { it == null }) emptyList() else parsed.filterNotNull()
    }

    /**
     * Split version into base and optional pre-release suffix.
     * e.g. "1.2.0-beta.1" → ("1.2.0", "beta.1")
     */
    internal fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split("-", limit = 2)
        return parts[0] to parts.getOrNull(1)
    }
}
