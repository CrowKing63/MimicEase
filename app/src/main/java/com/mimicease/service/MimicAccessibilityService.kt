package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MimicAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MimicAccessibilityService? = null
            private set
    }

    private var faceDetectionServiceConnection: ServiceConnection? = null
    var faceDetectionService: FaceDetectionForegroundService? = null
        private set
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 글로벌 토글 컨트롤러 (BroadcastReceiver에서 접근 가능)
    var globalToggleController: GlobalToggleController? = null
        private set

    // 커서 위치 추적 (CURSOR_CLICK 모드)
    val cursorTracker = CursorTracker()

    // Switch Access Bridge (SwitchKey 액션 → 접근성 동작 변환)
    val switchAccessBridge by lazy { SwitchAccessBridge(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // FaceDetectionForegroundService 시작 및 바인딩
        val intent = Intent(this, FaceDetectionForegroundService::class.java)
        startForegroundService(intent)
        bindFaceDetectionService()
    }

    private fun bindFaceDetectionService() {
        faceDetectionServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val localBinder = binder as FaceDetectionForegroundService.LocalBinder
                faceDetectionService = localBinder.getService()
                // ActionExecutor에 this(AccessibilityService) 참조 전달
                faceDetectionService?.setAccessibilityService(this@MimicAccessibilityService)

                // GlobalToggleController 초기화
                globalToggleController = GlobalToggleController(
                    context = this@MimicAccessibilityService,
                    onToggle = { faceDetectionService?.togglePause() },
                    onEnable = { faceDetectionService?.resumeAnalysis() },
                    onDisable = { faceDetectionService?.pauseAnalysis() },
                    isPaused = { FaceDetectionForegroundService.isPaused.value }
                )
                // 초기 설정 적용은 FaceDetectionForegroundService에서 observe하여 전달
                faceDetectionService?.setGlobalToggleController(globalToggleController!!)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                faceDetectionService = null
            }
        }
        bindService(
            Intent(this, FaceDetectionForegroundService::class.java),
            faceDetectionServiceConnection!!,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        // 커서 위치 추적 (hover + focus 이벤트)
        cursorTracker.onAccessibilityEvent(event)
    }

    /**
     * 물리 키 이벤트 수신.
     * 볼륨 Up+Down 동시 홀드 등으로 글로벌 토글을 처리합니다.
     * 처리하지 않은 키는 false를 반환하여 다른 앱에 전달합니다.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return globalToggleController?.handleKeyEvent(event) ?: false
    }

    override fun onInterrupt() {
        faceDetectionService?.pauseAnalysis()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        globalToggleController?.destroy()
        globalToggleController = null
        // unbindService() can throw IllegalArgumentException if the binding was never
        // fully established (e.g., FaceDetectionForegroundService crashed during creation).
        faceDetectionServiceConnection?.let {
            try { unbindService(it) } catch (e: Exception) { /* already unbound */ }
        }
        // Stop the foreground service when accessibility is disabled so it doesn't
        // keep running indefinitely via START_STICKY.
        stopService(Intent(this, FaceDetectionForegroundService::class.java))
        faceDetectionService = null
        instance = null
        return super.onUnbind(intent)
    }
}

