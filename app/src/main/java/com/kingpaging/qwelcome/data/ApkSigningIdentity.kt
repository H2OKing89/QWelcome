package com.kingpaging.qwelcome.data

/**
 * Current signers must stay separate from the cryptographically verified rotation history.
 * Multiple simultaneous signers require an exact match; a single signer may move forward
 * along the archive's proven lineage, but never back to a historical key.
 */
internal data class ApkSigningIdentity(
    val currentSigners: Set<String>,
    val certificateHistory: Set<String> = currentSigners,
) {
    fun acceptsUpdate(archive: ApkSigningIdentity): Boolean =
        when {
            currentSigners.isEmpty() || archive.currentSigners.isEmpty() -> false
            currentSigners.size > 1 || archive.currentSigners.size > 1 -> currentSigners == archive.currentSigners
            else ->
                currentSigners == archive.currentSigners ||
                    archive.certificateHistory.containsAll(currentSigners)
        }
}
