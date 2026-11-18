package com.dynamiccapsule.core.system.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.dynamiccapsule.core.model.AppProjectInfo
import com.dynamiccapsule.core.model.AppUpdateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("html_url") val htmlUrl: String = AppProjectInfo.GITHUB_RELEASES_URL,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
internal data class GitHubAssetDto(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("size") val size: Long = 0L
)

/**
 * Gestionnaire de mises à jour automatiques connecté aux GitHub Releases
 * du dépôt officiel https://github.com/MrSchnnurbart/Dynamic-Capsule.
 */
class GitHubUpdateManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _updateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val updateStatus: StateFlow<AppUpdateStatus> = _updateStatus.asStateFlow()

    fun checkForUpdates(currentVersion: String = AppProjectInfo.VERSION_NAME) {
        if (_updateStatus.value is AppUpdateStatus.Checking || _updateStatus.value is AppUpdateStatus.Downloading) {
            return
        }
        _updateStatus.value = AppUpdateStatus.Checking

        scope.launch {
            runCatching {
                val url = URL(AppProjectInfo.GITHUB_API_LATEST_RELEASE)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "DynamicCapsule-Android/$currentVersion")
                }

                val code = conn.responseCode
                if (code == 404) {
                    // Aucune release encore publiée sur le dépôt -> l'application est à jour
                    _updateStatus.value = AppUpdateStatus.UpToDate(currentVersion)
                    conn.disconnect()
                    return@launch
                }
                if (code !in 200..299) {
                    conn.disconnect()
                    _updateStatus.value = AppUpdateStatus.Error(
                        messageFr = "Serveur GitHub indisponible (HTTP $code)",
                        messageEn = "GitHub server unavailable (HTTP $code)"
                    )
                    return@launch
                }

                val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val release = json.decodeFromString<GitHubReleaseDto>(bodyText)
                val cleanRemoteVersion = release.tagName.trim().removePrefix("v").removePrefix("V")

                if (cleanRemoteVersion.isNotBlank() && isRemoteVersionNewer(currentVersion, cleanRemoteVersion)) {
                    val apkAsset = release.assets.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true) && !it.name.contains("debug", ignoreCase = true)
                    } ?: release.assets.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true)
                    }

                    _updateStatus.value = AppUpdateStatus.UpdateAvailable(
                        currentVersion = currentVersion,
                        latestVersion = cleanRemoteVersion,
                        releaseTitle = release.name.ifBlank { "Dynamic Capsule v$cleanRemoteVersion" },
                        releaseNotesBody = release.body.ifBlank {
                            "Nouvelle version v$cleanRemoteVersion disponible sur GitHub."
                        },
                        apkDownloadUrl = apkAsset?.browserDownloadUrl,
                        htmlReleaseUrl = release.htmlUrl.ifBlank { AppProjectInfo.GITHUB_RELEASES_URL },
                        publishedAt = release.publishedAt.take(10)
                    )
                } else {
                    _updateStatus.value = AppUpdateStatus.UpToDate(currentVersion)
                }
            }.onFailure {
                _updateStatus.value = AppUpdateStatus.Error(
                    messageFr = "Impossible de vérifier les mises à jour (vérifiez votre connexion Internet).",
                    messageEn = "Unable to check for updates (please check your Internet connection)."
                )
            }
        }
    }

    fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        latestVersion: String
    ) {
        val appContext = context.applicationContext
        _updateStatus.value = AppUpdateStatus.Downloading(latestVersion = latestVersion, progressPercent = 0)

        scope.launch {
            runCatching {
                val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
                val targetFile = File(updatesDir, "DynamicCapsule-v$latestVersion.apk")

                val url = URL(downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    setRequestProperty("User-Agent", "DynamicCapsule-Android/${AppProjectInfo.VERSION_NAME}")
                }

                val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
                var downloadedBytes = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(1, 99)
                            _updateStatus.value = AppUpdateStatus.Downloading(latestVersion, pct)
                        }
                    }
                }
                conn.disconnect()

                _updateStatus.value = AppUpdateStatus.ReadyToInstall(
                    latestVersion = latestVersion,
                    localApkPath = targetFile.absolutePath
                )
                triggerPackageInstaller(appContext, targetFile)
            }.onFailure {
                // En cas d'échec du téléchargement direct, ouvre la page GitHub Releases dans le navigateur
                _updateStatus.value = AppUpdateStatus.Error(
                    messageFr = "Échec du téléchargement direct — ouverture de GitHub Releases.",
                    messageEn = "Direct download failed — opening GitHub Releases page."
                )
                openUrlInBrowser(appContext, downloadUrl)
            }
        }
    }

    fun triggerPackageInstaller(context: Context, apkFile: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        }.onFailure {
            openUrlInBrowser(context, AppProjectInfo.GITHUB_RELEASES_URL)
        }
    }

    fun openUrlInBrowser(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    companion object {
        /**
         * Compare deux versions sémantiques (ex: "1.0.0" vs "1.1.0").
         * Retourne true si remoteVersion > currentVersion.
         */
        fun isRemoteVersionNewer(currentVersion: String, remoteVersion: String): Boolean {
            val currentParts = currentVersion.trim().removePrefix("v").split(".", "-")
                .mapNotNull { it.toIntOrNull() }
            val remoteParts = remoteVersion.trim().removePrefix("v").split(".", "-")
                .mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val cur = currentParts.getOrElse(i) { 0 }
                val rem = remoteParts.getOrElse(i) { 0 }
                if (rem > cur) return true
                if (rem < cur) return false
            }
            return false
        }
    }
}
