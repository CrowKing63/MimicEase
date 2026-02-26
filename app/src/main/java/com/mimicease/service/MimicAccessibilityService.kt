package com.mimicease.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
        // 현재 포커스 창 정보 업데이트 (필요 시 활용)
        // MimicEase 자체 이벤트는 소비하지 않음
    }

    override fun onInterrupt() {
        faceDetectionService?.pauseAnalysis()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        faceDetectionServiceConnection?.let { unbindService(it) }
        faceDetectionService = null
        instance = null
        return super.onUnbind(intent)
    }
}
