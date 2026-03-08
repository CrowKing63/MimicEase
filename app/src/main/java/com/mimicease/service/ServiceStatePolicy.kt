package com.mimicease.service

import com.mimicease.domain.model.ServiceState

object ServiceStatePolicy {
    fun resolveRuntimeState(
        storedRuntimeState: ServiceState,
        isForegroundServiceRunning: Boolean
    ): ServiceState {
        return when {
            !isForegroundServiceRunning -> ServiceState.Stopped
            storedRuntimeState == ServiceState.Stopped -> ServiceState.Running
            else -> storedRuntimeState
        }
    }

    fun shouldRestoreService(targetState: ServiceState): Boolean {
        return targetState.isStarted
    }

    fun targetStateAfterToggle(currentRuntimeState: ServiceState): ServiceState {
        return when (currentRuntimeState) {
            ServiceState.Running -> ServiceState.Paused
            ServiceState.Paused, ServiceState.Stopped -> ServiceState.Running
        }
    }

    fun targetStateAfterEnable(): ServiceState = ServiceState.Running

    fun targetStateAfterDisable(currentRuntimeState: ServiceState): ServiceState {
        // 사용자의 "완전 비활성화" 의도는 부팅/접근성 재연결에서도 자동 복구되지 않는
        // Stopped 상태로 정규화한다. (HomeScreen stop, 글로벌 disable, 브로드캐스트 disable 공통)
        return ServiceState.Stopped
    }

    fun shouldResumeAfterScreenOn(targetState: ServiceState): Boolean {
        return targetState == ServiceState.Running
    }
}
