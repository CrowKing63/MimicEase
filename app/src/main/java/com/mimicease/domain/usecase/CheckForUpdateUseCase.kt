package com.mimicease.domain.usecase

import com.mimicease.BuildConfig
import com.mimicease.domain.repository.ReleaseInfo
import com.mimicease.domain.repository.UpdateRepository
import javax.inject.Inject

private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L  // 24시간

sealed class CheckResult {
    object Skipped : CheckResult()                            // 24시간 미경과, 체크 생략
    object Failed : CheckResult()                             // 네트워크 오류 또는 파싱 실패
    object UpToDate : CheckResult()                           // 체크 완료, 최신 버전
    data class UpdateAvailable(val release: ReleaseInfo) : CheckResult()
}

class CheckForUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    /**
     * 최신 릴리즈를 확인합니다.
     *
     * @param lastCheckMs 마지막으로 성공적으로 체크한 시각 (AppSettings.lastUpdateCheckMs)
     * @param nowMs       현재 시각 — System.currentTimeMillis() 사용
     * @param forceCheck  true이면 24시간 쓰로틀 무시 (사용자가 수동으로 체크 버튼 누를 때)
     */
    suspend fun execute(
        lastCheckMs: Long,
        nowMs: Long,
        forceCheck: Boolean = false
    ): CheckResult {
        val shouldCheck = forceCheck || (nowMs - lastCheckMs > CHECK_INTERVAL_MS)
        if (!shouldCheck) return CheckResult.Skipped

        val release = updateRepository.fetchLatestRelease()
            ?: return CheckResult.Failed

        val isNewer = updateRepository.isNewerVersion(
            localVersion = BuildConfig.VERSION_NAME,
            remoteVersion = release.versionName
        )
        return if (isNewer) CheckResult.UpdateAvailable(release)
        else CheckResult.UpToDate
    }
}
