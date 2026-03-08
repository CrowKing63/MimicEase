package com.mimicease.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.mimicease.R
import com.mimicease.domain.model.ServiceState
import timber.log.Timber

class MimicToggleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()

        val snapshot = MimicServiceStateStore.readSnapshotBlocking(this)
        if (!snapshot.isAccessibilityServiceEnabled) {
            Timber.w("MimicToggleTile: accessibility service is disabled")
            showToast("MimicEase 접근성 서비스를 먼저 활성화하세요")
            refreshTile()
            return
        }

        try {
            // QS 타일 토글도 브로드캐스트 기반 글로벌 토글 경로를 사용한다.
            // → ToggleBroadcastReceiver + ServiceStatePolicy + MimicServiceStateStore 공통 처리
            sendBroadcast(
                Intent(ToggleBroadcastReceiver.ACTION_TOGGLE).setPackage(packageName)
            )
            refreshTile()
        } catch (e: Exception) {
            Timber.e(e, "MimicToggleTile: failed to send control intent")
            showToast("MimicEase 제어 실패")
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val snapshot = MimicServiceStateStore.readSnapshotBlocking(this)

        tile.state = when {
            !snapshot.isAccessibilityServiceEnabled -> Tile.STATE_UNAVAILABLE
            snapshot.runtimeState == ServiceState.Running -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_label)
        tile.contentDescription = when {
            !snapshot.isAccessibilityServiceEnabled -> getString(R.string.tile_unavailable)
            snapshot.runtimeState == ServiceState.Paused -> getString(R.string.tile_paused)
            snapshot.runtimeState == ServiceState.Running -> getString(R.string.tile_active)
            else -> getString(R.string.tile_unavailable)
        }
        tile.updateTile()
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.w(e, "Toast 표시 실패")
        }
    }
}
