package com.mimicease

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.mimicease.service.ToggleBroadcastReceiver
import timber.log.Timber

/**
 * 딥링크(mimicease://toggle|enable|disable) 수신 및 앱 단축키 처리용 투명 액티비티.
 *
 * - Bixby 루틴 / 제미나이 앱 단축키에서 이 액티비티를 직접 호출.
 * - 수신한 URI를 브로드캐스트로 변환 후 즉시 종료 (UI 표시 없음).
 */
class ToggleActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val host = intent?.data?.host ?: run {
            Timber.w("ToggleActionActivity: data URI 없음")
            return
        }
        val action = when (host) {
            "toggle"  -> ToggleBroadcastReceiver.ACTION_TOGGLE
            "enable"  -> ToggleBroadcastReceiver.ACTION_ENABLE
            "disable" -> ToggleBroadcastReceiver.ACTION_DISABLE
            else -> {
                Timber.w("ToggleActionActivity: 알 수 없는 host=$host")
                return
            }
        }
        Timber.d("ToggleActionActivity: $action 브로드캐스트 전송")
        // setPackage 필수: Android 8.0+에서 암묵적 브로드캐스트는 매니페스트 리시버에 미도달
        sendBroadcast(Intent(action).setPackage(packageName))
    }
}
