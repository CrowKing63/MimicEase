package com.mimicease.domain.repository

data class ReleaseInfo(
    val tagName: String,         // e.g. "v1.2"
    val versionName: String,     // tagName without 'v' prefix → "1.2"
    val apkDownloadUrl: String,  // direct download URL for the APK asset
    val releaseNotes: String     // body field from GitHub API
)

interface UpdateRepository {
    /**
     * GitHub Releases API에서 최신 릴리즈 정보를 조회합니다.
     * 네트워크 오류 또는 파싱 실패 시 null을 반환합니다.
     */
    suspend fun fetchLatestRelease(): ReleaseInfo?

    /**
     * [remoteVersion]이 [localVersion]보다 새로운 버전인지 비교합니다.
     * "1.0" vs "1.1" 형태의 Semver 비교 (숫자 단위 비교).
     */
    fun isNewerVersion(localVersion: String, remoteVersion: String): Boolean
}
