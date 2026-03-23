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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.mimicease.domain.model.ServiceState
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
    private var isBindingFaceDetectionService = false
    var faceDetectionService: FaceDetectionForegroundService? = null
        private set
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var globalToggleController: GlobalToggleController? = null
        private set

    val cursorTracker = CursorTracker()
    val switchAccessBridge by lazy { SwitchAccessBridge(this) }

    private val mouseClickCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) = Unit

        override fun onCancelled(gestureDescription: GestureDescription) {
            Timber.w("Mouse click replay cancelled by system")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // setMotionEventSources는 updateMouseInterceptMode()에서 동적으로 설정합니다.
        // 패스스루 모드(기본)에서는 BT 마우스 이벤트를 시스템에 그대로 전달합니다.

        globalToggleController = GlobalToggleController(
            context = this,
            onToggle = { requestTargetState(ServiceStatePolicy.targetStateAfterToggle(currentRuntimeState())) },
            onEnable = { requestTargetState(ServiceStatePolicy.targetStateAfterEnable()) },
            onDisable = { requestTargetState(ServiceStatePolicy.targetStateAfterDisable(currentRuntimeState())) }
        )

        val snapshot = MimicServiceStateStore.readSnapshotBlocking(this)
        if (ServiceStatePolicy.shouldRestoreService(snapshot.targetState)) {
            ensureFaceDetectionServiceBound(snapshot.targetState)
        } else {
            stopService(Intent(this, FaceDetectionForegroundService::class.java))
            MimicServiceStateStore.persistRuntimeStateBlocking(this, ServiceState.Stopped)
        }
    }

    fun ensureFaceDetectionServiceBound(targetState: ServiceState? = null) {
        if (targetState != null && targetState.isStarted) {
            startForegroundService(FaceDetectionForegroundService.createStartIntent(this, targetState))
        }

        if (faceDetectionService != null || isBindingFaceDetectionService) {
            return
        }

        if (faceDetectionServiceConnection == null) {
            faceDetectionServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    isBindingFaceDetectionService = false
                    val localBinder = binder as FaceDetectionForegroundService.LocalBinder
                    faceDetectionService = localBinder.getService()
                    faceDetectionService?.setAccessibilityService(this@MimicAccessibilityService)
                    globalToggleController?.let { controller ->
                        faceDetectionService?.setGlobalToggleController(controller)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    isBindingFaceDetectionService = false
                    faceDetectionService = null
                }
            }
        }

        isBindingFaceDetectionService = true
        bindService(
            Intent(this, FaceDetectionForegroundService::class.java),
            faceDetectionServiceConnection!!,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        cursorTracker.onAccessibilityEvent(event)
    }

    /**
     * BT 마우스 이벤트 인터셉트 모드를 런타임에 전환합니다.
     * @param intercept true → SOURCE_MOUSE 이벤트를 인터셉트 (게임 등 호버 이벤트 없는 앱용)
     *                  false → SOURCE_MOUSE 이벤트를 시스템에 패스스루 (네이티브 마우스 동작 유지)
     */
    fun updateMouseInterceptMode(intercept: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                serviceInfo = serviceInfo?.apply {
                    setMotionEventSources(if (intercept) InputDevice.SOURCE_MOUSE else 0)
                }
                Timber.i("Mouse intercept mode: ${if (intercept) "INTERCEPT" else "PASSTHROUGH"}")
            } catch (e: Exception) {
                Timber.w(e, "Failed to update mouse intercept mode")
            }
        }
    }

    // ── 인터셉트 모드: 마우스 이벤트 → 제스처 재주입 ──────────────────────
    private var mouseDownTimeMs = 0L
    private var mouseDownX = 0f
    private var mouseDownY = 0f
    private var isMouseButtonDown = false

    // 화면 가장자리 진입 시 시스템 UI(네비게이션 바/상태 표시줄) 엿보기 트리거
    private var edgeRevealLastMs = 0L
    private val EDGE_REVEAL_COOLDOWN_MS = 800L
    private val EDGE_THRESHOLD_DP = 10f

    /**
     * 마우스 커서가 화면 하단/상단 가장자리에 진입하면, 해당 위치에 짧은 탭 제스처를 주입하여
     * 삼성 One UI의 "시스템 UI 엿보기(peek)" 동작을 대신 트리거한다.
     *
     * 배경: setMotionEventSources(SOURCE_MOUSE)로 마우스를 인터셉트하면 삼성 포인터 레이어가
     * HOVER_MOVE 이벤트를 가장자리 감지 파이프라인에 넘기지 않아 네비게이션 바가 나타나지 않음.
     */
    private fun maybeRevealSystemBar(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - edgeRevealLastMs < EDGE_REVEAL_COOLDOWN_MS) return
        if (isMouseButtonDown) return  // 드래그 중에는 트리거하지 않음

        val dm = resources.displayMetrics
        val edgePx = EDGE_THRESHOLD_DP * dm.density

        val tapY = when {
            y >= dm.heightPixels - edgePx -> dm.heightPixels.toFloat() - 1f  // 하단 가장자리
            y <= edgePx -> 0f                                                  // 상단 가장자리
            else -> return
        }

        edgeRevealLastMs = now
        val path = Path().apply { moveTo(x, tapY) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
                .build(),
            null, null
        )
    }

    override fun onMotionEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_ENTER -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
                maybeRevealSystemBar(event.x, event.y)
            }

            MotionEvent.ACTION_DOWN -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
                mouseDownX = event.x
                mouseDownY = event.y
                mouseDownTimeMs = System.currentTimeMillis()
                isMouseButtonDown = true

                // 우클릭(BUTTON_SECONDARY) → 즉시 롱프레스 제스처
                if (event.buttonState and MotionEvent.BUTTON_SECONDARY != 0) {
                    val isExpressionGestureActive = faceDetectionService?.isGestureDispatching() ?: false
                    if (!isExpressionGestureActive) {
                        val path = Path().apply { moveTo(event.x, event.y) }
                        val stroke = GestureDescription.StrokeDescription(path, 0L, 800L)
                        dispatchGesture(
                            GestureDescription.Builder().addStroke(stroke).build(),
                            mouseClickCallback,
                            null
                        )
                    }
                    isMouseButtonDown = false // 우클릭은 DOWN에서 완결
                }
            }

            MotionEvent.ACTION_UP -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
                if (isMouseButtonDown) {
                    isMouseButtonDown = false
                    val isExpressionGestureActive = faceDetectionService?.isGestureDispatching() ?: false
                    if (!isExpressionGestureActive) {
                        val holdMs = System.currentTimeMillis() - mouseDownTimeMs
                        if (holdMs >= 500L) {
                            // 길게 눌렀으면 롱프레스 제스처
                            val path = Path().apply { moveTo(event.x, event.y) }
                            val stroke = GestureDescription.StrokeDescription(path, 0L, 800L)
                            dispatchGesture(
                                GestureDescription.Builder().addStroke(stroke).build(),
                                mouseClickCallback,
                                null
                            )
                        } else {
                            // 짧게 클릭
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

            MotionEvent.ACTION_MOVE -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
            }

            MotionEvent.ACTION_SCROLL -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
                val isExpressionGestureActive = faceDetectionService?.isGestureDispatching() ?: false
                if (!isExpressionGestureActive) {
                    val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val dm = resources.displayMetrics
                    val cx = dm.widthPixels / 2f
                    val scrollDist = dm.heightPixels * 0.25f
                    if (vScroll != 0f) {
                        val startY = if (vScroll > 0) cy(cx, scrollDist, true) else cy(cx, scrollDist, false)
                        val endY = if (vScroll > 0) cy(cx, scrollDist, false) else cy(cx, scrollDist, true)
                        val path = Path().apply { moveTo(cx, startY); lineTo(cx, endY) }
                        val stroke = GestureDescription.StrokeDescription(path, 0L, 200L)
                        dispatchGesture(
                            GestureDescription.Builder().addStroke(stroke).build(),
                            mouseClickCallback,
                            null
                        )
                    }
                }
            }
        }
    }

    /** 스크롤 Helper: 화면 중앙 기준 스크롤 시작/끝 Y좌표 계산 */
    private fun cy(cx: Float, dist: Float, isTop: Boolean): Float {
        val center = resources.displayMetrics.heightPixels / 2f
        return if (isTop) center - dist else center + dist
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return globalToggleController?.handleKeyEvent(event) ?: false
    }

    override fun onInterrupt() {
        faceDetectionService?.cancelCurrentGesture()
        faceDetectionService?.pauseAnalysis()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        globalToggleController?.destroy()
        globalToggleController = null
        faceDetectionServiceConnection?.let {
            try {
                unbindService(it)
            } catch (e: Exception) {
                Timber.w(e, "FaceDetectionForegroundService was already unbound")
            }
        }
        stopService(Intent(this, FaceDetectionForegroundService::class.java))
        MimicServiceStateStore.persistRuntimeStateBlocking(this, ServiceState.Stopped)
        faceDetectionService = null
        isBindingFaceDetectionService = false
        instance = null
        return super.onUnbind(intent)
    }

    private fun currentRuntimeState(): ServiceState {
        return MimicServiceStateStore.readSnapshotBlocking(this).runtimeState
    }

    private fun requestTargetState(targetState: ServiceState): ServiceState {
        return try {
            MimicServiceStateStore.persistTargetStateBlocking(this, targetState)
            when (targetState) {
                ServiceState.Running,
                ServiceState.Paused -> {
                    ensureFaceDetectionServiceBound(targetState)
                }

                ServiceState.Stopped -> {
                    if (currentRuntimeState().isStarted) {
                        startService(Intent(this, FaceDetectionForegroundService::class.java).apply {
                            action = FaceDetectionForegroundService.ACTION_STOP
                        })
                    } else {
                        MimicServiceStateStore.persistRuntimeStateBlocking(this, ServiceState.Stopped)
                    }
                }
            }
            targetState
        } catch (e: Exception) {
            Timber.e(e, "Failed to update MimicEase target state")
            currentRuntimeState()
        }
    }
}
