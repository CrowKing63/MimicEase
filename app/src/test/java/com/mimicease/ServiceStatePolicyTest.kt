package com.mimicease

import com.mimicease.domain.model.ServiceState
import com.mimicease.service.ServiceStatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatePolicyTest {

    @Test
    fun `토글 - 완전 중지 상태에서는 실행 상태를 목표로 한다`() {
        val result = ServiceStatePolicy.targetStateAfterToggle(ServiceState.Stopped)

        assertEquals(ServiceState.Running, result)
    }

    @Test
    fun `disable - 어떤 런타임 상태에서도 목표 상태를 완전 중지로 맞춘다`() {
        assertEquals(ServiceState.Stopped, ServiceStatePolicy.targetStateAfterDisable(ServiceState.Running))
        assertEquals(ServiceState.Stopped, ServiceStatePolicy.targetStateAfterDisable(ServiceState.Paused))
        assertEquals(ServiceState.Stopped, ServiceStatePolicy.targetStateAfterDisable(ServiceState.Stopped))
    }

    @Test
    fun `런타임 상태 해석 - 서비스 프로세스가 없으면 저장값과 관계없이 완전 중지다`() {
        val result = ServiceStatePolicy.resolveRuntimeState(
            storedRuntimeState = ServiceState.Running,
            isForegroundServiceRunning = false
        )

        assertEquals(ServiceState.Stopped, result)
    }

    @Test
    fun `런타임 상태 해석 - 서비스는 살아있지만 저장값이 없으면 실행 중으로 복원한다`() {
        val result = ServiceStatePolicy.resolveRuntimeState(
            storedRuntimeState = ServiceState.Stopped,
            isForegroundServiceRunning = true
        )

        assertEquals(ServiceState.Running, result)
    }

    @Test
    fun `복구 정책 - 완전 중지는 재연결이나 부팅에서 자동 복구하지 않는다`() {
        assertFalse(ServiceStatePolicy.shouldRestoreService(ServiceState.Stopped))
        assertTrue(ServiceStatePolicy.shouldRestoreService(ServiceState.Running))
        assertTrue(ServiceStatePolicy.shouldRestoreService(ServiceState.Paused))
    }

    @Test
    fun `화면 켜짐 복구 - 목표 상태가 실행 중일 때만 자동 재개한다`() {
        assertTrue(ServiceStatePolicy.shouldResumeAfterScreenOn(ServiceState.Running))
        assertFalse(ServiceStatePolicy.shouldResumeAfterScreenOn(ServiceState.Paused))
        assertFalse(ServiceStatePolicy.shouldResumeAfterScreenOn(ServiceState.Stopped))
    }
}
