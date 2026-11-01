package com.iltatrainer.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.iltatrainer.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseUrl: String = "",
    val notes: String = "",
    val sizeBytes: Long = 0L,
    val digest: String? = null,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class Installing(val info: UpdateInfo) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Checks the latest GitHub Release and installs a newer signed APK.
 * Auto-checks stay silent when the network is unavailable; manual checks report errors.
 */
class UpdateManager(
    private val context: Context,
    private val repository: String = BuildConfig.UPDATE_REPO,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    suspend fun check(force: Boolean): UpdateState = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        val result = try {
            val release = fetchLatestRelease()
            if (release == null) {
                if (force) UpdateState.Failed("Релизы не найдены") else UpdateState.Idle
            } else if (isNewerVersion(release.versionName, BuildConfig.VERSION_NAME)) {
                UpdateState.Available(release)
            } else {
                UpdateState.UpToDate
            }
        } catch (error: Exception) {
            if (force) UpdateState.Failed(error.message ?: "Не удалось проверить обновления") else UpdateState.Idle
        }
        _state.value = result
        result
    }

    suspend fun downloadAndInstall(info: UpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = UpdateState.Downloading(0)
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "ilta-update.apk")
            download(info, apkFile)
            val verification = verifyApk(apkFile)
            if (verification != null) {
                _state.value = UpdateState.Failed(verification)
                return@withContext Result.failure(IllegalStateException(verification))
            }
            _state.value = UpdateState.Installing(info)
            install(apkFile)
            Result.success(Unit)
        } catch (error: Exception) {
            _state.value = UpdateState.Failed(error.message ?: "Не удалось скачать обновление")
            Result.failure(error)
        }
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        val url = URL("https://api.github.com/repos/$repository/releases/latest")
        if (url.protocol != "https") error("Некорректный URL обновления")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "IlTa/${BuildConfig.VERSION_NAME}")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            val code = connection.responseCode
            if (code == 404) error("Релизы не найдены")
            if (code == 403 || code == 429) error("GitHub временно ограничил запросы. Попробуйте позже.")
            if (code != 200) error("GitHub ответил кодом $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString<GitHubRelease>(body)
            val version = release.tagName.removePrefix("v").trim()
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return null
            if (version.isBlank() || apk.browserDownloadUrl.isBlank()) return null
            return UpdateInfo(
                versionName = version,
                downloadUrl = apk.browserDownloadUrl,
                releaseUrl = release.htmlUrl,
                notes = release.body.trim(),
                sizeBytes = apk.size,
                digest = apk.digest,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun download(info: UpdateInfo, destination: File) {
        val url = URL(info.downloadUrl)
        if (url.protocol != "https") error("Обновление должно скачиваться по HTTPS")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "IlTa/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Не удалось скачать APK: код ${connection.responseCode}")
            }
            val total = info.sizeBytes.takeIf { it > 0 } ?: connection.contentLengthLong
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        val progress = if (total > 0) ((readTotal * 100) / total).toInt() else 0
                        _state.value = UpdateState.Downloading(progress.coerceIn(0, 100))
                    }
                }
            }
            info.digest?.takeIf { it.startsWith("sha256:") }?.let { digest ->
                val actual = sha256(destination)
                if (!actual.equals(digest.removePrefix("sha256:"), ignoreCase = true)) {
                    destination.delete()
                    error("Контрольная сумма обновления не совпала")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyApk(file: File): String? {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: return "Файл обновления повреждён"
        if (archive.packageName != context.packageName) return "Обновление относится к другому приложению"
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }
        if (archiveCode <= BuildConfig.VERSION_CODE) return "Версия обновления не новее установленной"
        val currentSignatures = signatures(packageManager.getPackageInfo(context.packageName, flags))
        val archiveSignatures = signatures(archive)
        if (currentSignatures.isEmpty() || archiveSignatures != currentSignatures) {
            return "Подпись обновления не совпадает с установленным приложением"
        }
        return null
    }

    private fun signatures(info: PackageInfo): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
        }

    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            _state.value = UpdateState.Failed("Разрешите установку неизвестных приложений и нажмите ещё раз")
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String = "",
        val body: String = "",
        @SerialName("html_url") val htmlUrl: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        @SerialName("browser_download_url") val browserDownloadUrl: String = "",
        val size: Long = 0L,
        val digest: String? = null,
    )
}

/** Compares dotted numeric versions such as 2.4.0 and 2.10.1. */
fun isNewerVersion(remote: String, current: String): Boolean {
    val remoteParts = remote.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
    val currentParts = current.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
    if (remoteParts.isEmpty()) return false
    val length = maxOf(remoteParts.size, currentParts.size)
    for (index in 0 until length) {
        val remotePart = remoteParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (remotePart != currentPart) return remotePart > currentPart
    }
    return false
}
