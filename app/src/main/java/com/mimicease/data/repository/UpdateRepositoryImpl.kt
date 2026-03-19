package com.mimicease.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mimicease.domain.repository.ReleaseInfo
import com.mimicease.domain.repository.UpdateRepository
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor() : UpdateRepository {

    private val gson = Gson()

    override suspend fun fetchLatestRelease(): ReleaseInfo? {
        return try {
            val url = URL("https://api.github.com/repos/CrowKing63/MimicEase/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MimicEase-App")
            }
            if (connection.responseCode != 200) {
                Timber.w("GitHub API returned ${connection.responseCode}")
                connection.disconnect()
                return null
            }
            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = gson.fromJson(body, JsonObject::class.java)
            val tagName = json.get("tag_name")?.asString ?: return null
            val releaseNotes = json.get("body")?.asString ?: ""

            val assets = json.getAsJsonArray("assets") ?: return null
            val apkAsset = assets.firstOrNull { element ->
                element.asJsonObject.get("name")?.asString?.endsWith(".apk") == true
            }?.asJsonObject ?: return null

            val apkUrl = apkAsset.get("browser_download_url")?.asString ?: return null
            val versionName = tagName.removePrefix("v")

            ReleaseInfo(
                tagName = tagName,
                versionName = versionName,
                apkDownloadUrl = apkUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch latest release")
            null
        }
    }

    override fun isNewerVersion(localVersion: String, remoteVersion: String): Boolean {
        return try {
            val local = localVersion.split(".").map { it.toInt() }
            val remote = remoteVersion.split(".").map { it.toInt() }
            val maxLen = maxOf(local.size, remote.size)
            for (i in 0 until maxLen) {
                val l = local.getOrElse(i) { 0 }
                val r = remote.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            false // 동일 버전
        } catch (e: NumberFormatException) {
            Timber.e(e, "Version parse error: local=$localVersion remote=$remoteVersion")
            false
        }
    }
}
