package com.mimicease.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mimicease.R
import timber.log.Timber

/**
 * 빠른 설정(Quick Settings) 타일 서비스.
 *
 * 알림 창을 내려서 타일을 탭하거나, Bixby 루틴의 "빠른 설정 변경" 동작으로
 * MimicEase를 ON/OFF할 수 있습니다.
 *
 * Bixby 루틴 연동 방법:
 *   루틴 추가 → 동작 추가 → 빠른 설정 → 미믹이즈 타일 선택
 */
class MimicToggleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = FaceDetectionForegroundService.isRunning
        val isPaused = FaceDetectionForegroundService.isPaused.value

        Timber.d("MimicToggleTile: onClick — isRunning=$isRunning, isPaused=$isPaused")

        val action = when {
            !isRunning -> {
                // 서비스 미실행 → 접근성 서비스가 켜야 하므로 여기선 안내만
                Timber.w("MimicToggleTile: FaceDetectionForegroundService가 실행 중이 아님")
                return
            }
            isPaused -> FaceDetectionForegroundService.ACTION_RESUME
            else     -> FaceDetectionForegroundService.ACTION_PAUSE
        }

        startForegroundService(
            Intent(this, FaceDetectionForegroundService::class.java).setAction(action)
        )
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isRunning = FaceDetectionForegroundService.isRunning
        val isPaused  = FaceDetectionForegroundService.isPaused.value

        tile.state = when {
            !isRunning -> Tile.STATE_UNAVAILABLE
            isPaused   -> Tile.STATE_INACTIVE
            else       -> Tile.STATE_ACTIVE
        }
        tile.label = getString(R.string.tile_label)
        tile.contentDescription = when {
            !isRunning -> getString(R.string.tile_unavailable)
            isPaused   -> getString(R.string.tile_paused)
            else       -> getString(R.string.tile_active)
        }
        tile.updateTile()
    }
}
