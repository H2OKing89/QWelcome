package com.kingpaging.qwelcome.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "UpdateChecker"
private const val GITHUB_API_URL = "https://api.github.com/repos/H2OKing89/QWelcome/releases/latest"

/**
 * Represents the result of an update check.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateCheckResult()
    
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * GitHub Release API response (minimal fields we need).
 */
@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

/**
 * Checks GitHub Releases for app updates.
 */
object UpdateChecker {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Check if an update is available.
     * @param currentVersionName The current app version name (from BuildConfig.VERSION_NAME)
     */
    suspend fun checkForUpdate(
        currentVersionName: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "QWelcome-Android")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 404 means no releases yet
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.d(TAG, "No releases found on GitHub")
                    return@withContext UpdateCheckResult.UpToDate
                }
                return@withContext UpdateCheckResult.Error("HTTP $responseCode")
            }
            
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            
            val release = json.decodeFromString<GitHubRelease>(responseBody)
            val latestVersion = release.tag_name.removePrefix("v")
            
            Log.d(TAG, "Current: $currentVersionName, Latest: $latestVersion")
            
            // Compare versions
            if (isNewerVersion(latestVersion, currentVersionName)) {
                // Find APK asset
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                val downloadUrl = apkAsset?.browser_download_url ?: release.html_url
                
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = latestVersion,
                    downloadUrl = downloadUrl,
                    releaseNotes = release.body ?: "No release notes available."
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: CancellationException) {
            // Rethrow cancellation to preserve structured concurrency
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Compare semantic versions (e.g., "1.2.0" vs "1.1.0").
     * Handles pre-release suffixes: stable releases are preferred over pre-releases.
     * Returns true if remote is newer than current.
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        // Split off pre-release suffix (e.g., "1.2.0-beta" -> base="1.2.0", pre="beta")
        val (remoteBase, remotePre) = splitVersion(remote)
        val (currentBase, currentPre) = splitVersion(current)

        // Parse version parts, filtering out any non-numeric segments
        val remoteParts = parseVersionParts(remoteBase)
        val currentParts = parseVersionParts(currentBase)

        // If either version is completely invalid, don't suggest an update
        if (remoteParts.isEmpty() || currentParts.isEmpty()) {
            Log.w(TAG, "Invalid version format - remote: '$remote', current: '$current'")
            return false
        }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // Base versions are equal - prefer stable over pre-release
        // If remote is stable (no pre-release) and current has pre-release, update available
        if (remotePre == null && currentPre != null) return true
        // If remote has pre-release and current is stable, no update
        if (remotePre != null && currentPre == null) return false

        return false
    }

    /**
     * Parse version string into numeric parts.
     * Handles malformed versions gracefully (e.g., "1.", "1.2.", ".1.2").
     */
    private fun parseVersionParts(versionBase: String): List<Int> {
        val segments = versionBase.split(".").filter { it.isNotEmpty() }
        val parsed = segments.map { it.toIntOrNull() }
        // If any segment is non-numeric, treat entire version as invalid
        return if (parsed.any { it == null }) emptyList() else parsed.filterNotNull()
    }

    /**
     * Split version into base and optional pre-release suffix.
     */
    private fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split("-", limit = 2)
        return parts[0] to parts.getOrNull(1)
    }
}
