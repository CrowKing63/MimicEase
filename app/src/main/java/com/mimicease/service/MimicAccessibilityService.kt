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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceInfo = serviceInfo?.apply {
                setMotionEventSources(InputDevice.SOURCE_MOUSE)
            }
        }

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

    override fun onMotionEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_ENTER -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
            }

            MotionEvent.ACTION_DOWN -> {
                cursorTracker.updateFromHeadTracker(event.x, event.y)
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
