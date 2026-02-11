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
private val SHA_256_HEX_REGEX = Regex("^[a-fA-F0-9]{64}$")
internal val TRUSTED_UPDATE_HOSTS = setOf(
    "github.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
    "github-releases.githubusercontent.com"
)

/**
 * Represents the result of an update check.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val assetName: String,
        val assetSizeBytes: Long,
        val sha256Hex: String
    ) : UpdateCheckResult()
    
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
    data class RateLimited(val retryAfterSeconds: Long?) : UpdateCheckResult()
}

/**
 * GitHub Release API response (minimal fields we need).
 */
@Serializable
internal data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
internal data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long = 0L,
    val digest: String? = null
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
                // 403 often means rate-limited by GitHub
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    val remaining = connection.getHeaderField("X-RateLimit-Remaining")
                    // Only treat as rate-limited when header is present and equals "0"
                    if (remaining == "0") {
                        val resetEpoch = connection.getHeaderField("X-RateLimit-Reset")
                            ?.toLongOrNull()
                        val retrySeconds = if (resetEpoch != null) {
                            (resetEpoch - System.currentTimeMillis() / 1000).coerceAtLeast(0)
                        } else {
                            null
                        }
                        Log.w(TAG, "Rate limited by GitHub, retry in ${retrySeconds}s")
                        return@withContext UpdateCheckResult.RateLimited(retrySeconds)
                    }
                    // Fall through to generic error if not rate-limited
                }
                return@withContext UpdateCheckResult.Error("HTTP $responseCode")
            }
            
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            parseUpdateResponse(responseBody, currentVersionName)
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

    internal fun parseUpdateResponse(
        responseBody: String,
        currentVersionName: String
    ): UpdateCheckResult {
        val release = json.decodeFromString<GitHubRelease>(responseBody)
        val latestVersion = release.tag_name.removePrefix("v")

        Log.d(TAG, "Current: $currentVersionName, Latest: $latestVersion")

        if (!VersionComparator.isNewerVersion(latestVersion, currentVersionName)) {
            return UpdateCheckResult.UpToDate
        }

        // Pick the first APK asset by lexical order for deterministic behavior.
        val apkAsset = release.assets
            .filter { it.name.endsWith(".apk", ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .firstOrNull()
            ?: return UpdateCheckResult.Error("Release has no APK asset")

        val sha256Hex = extractSha256Hex(apkAsset.digest)
            ?: return UpdateCheckResult.Error("Release APK is missing valid SHA-256 digest")

        if (!isTrustedHttpsUrl(apkAsset.browser_download_url)) {
            return UpdateCheckResult.Error("Invalid release URL")
        }

        return UpdateCheckResult.UpdateAvailable(
            latestVersion = latestVersion,
            downloadUrl = apkAsset.browser_download_url,
            releaseNotes = release.body ?: "No release notes available.",
            assetName = apkAsset.name,
            assetSizeBytes = apkAsset.size,
            sha256Hex = sha256Hex
        )
    }

    internal fun extractSha256Hex(digest: String?): String? {
        val normalized = digest
            ?.trim()
            ?.let { value ->
                if (value.startsWith("sha256:", ignoreCase = true)) {
                    value.substring("sha256:".length)
                } else {
                    value
                }
            }
            ?.lowercase()
            ?: return null
        return if (SHA_256_HEX_REGEX.matches(normalized)) normalized else null
    }

    internal fun isTrustedHttpsUrl(url: String): Boolean {
        return try {
            val parsed = URL(url)
            parsed.protocol.equals("https", ignoreCase = true) &&
                parsed.host.orEmpty().lowercase() in TRUSTED_UPDATE_HOSTS
        } catch (_: java.net.MalformedURLException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
