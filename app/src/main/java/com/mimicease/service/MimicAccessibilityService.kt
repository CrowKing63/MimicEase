package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Path
import android.os.Build
import android.os.IBinder
import android.view.InputDevice
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

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

    /**
     * BT 마우스 클릭을 dispatchGesture()로 재주입할 때 사용하는 콜백.
     * 표정 트리거 제스처와 별도로 관리되므로 gestureCallback과 분리합니다.
     */
    private val mouseClickCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {}
        override fun onCancelled(gestureDescription: GestureDescription) {
            Timber.w("Mouse click replay cancelled by system")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // SOURCE_MOUSE를 항상 인터셉트합니다.
        // — 클릭은 onMotionEvent(ACTION_DOWN)에서 dispatchGesture()로 재주입하여 앱에 전달합니다.
        // — 별도의 enable/disable 전환 없이 접근성 활성·비활성만으로 ON/OFF됩니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            serviceInfo = serviceInfo?.apply {
                setMotionEventSources(InputDevice.SOURCE_MOUSE)
            }
        }

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
     * SOURCE_MOUSE 이벤트를 수신합니다. (onServiceConnected에서 항상 활성화)
     *
     * Android는 SOURCE_MOUSE를 인터셉트하면 원본 클릭/드래그 이벤트가 앱에 전달되지 않습니다.
     * 따라서 ACTION_DOWN을 dispatchGesture(tap, 50ms)로 재주입하여 앱이 클릭을 수신하도록 합니다.
     *
     * 표정 트리거 제스처가 진행 중일 때(isGestureDispatching == true)는 재주입을 건너뛰어
     * 제스처 충돌을 방지합니다.
     */
    override fun onMotionEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_ENTER -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
            }
            MotionEvent.ACTION_DOWN -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
                // 표정 트리거 제스처가 진행 중이면 충돌 방지를 위해 재주입 건너뜀
                val isExpressionGestureActive = faceDetectionService?.isGestureDispatching() ?: false
                if (!isExpressionGestureActive) {
                    val path = Path().apply { moveTo(event.x, event.y) }
                    val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
                    dispatchGesture(
                        GestureDescription.Builder().addStroke(stroke).build(),
                        mouseClickCallback,
                        null
                    )
                }
            }
        }
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
        // 인터럽트 발생 시 진행 중인 제스처를 먼저 취소해야 BT 마우스가 풀립니다.
        faceDetectionService?.cancelCurrentGesture()
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
