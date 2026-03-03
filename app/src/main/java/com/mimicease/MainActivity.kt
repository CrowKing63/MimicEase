package com.mimicease

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mimicease.navigation.MimicNavGraph
import com.mimicease.ui.theme.MimicEaseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Android 13+ 알림 권한 런타임 요청
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted 여부와 관계없이 계속 진행 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // POST_NOTIFICATIONS: Android 13(API 33)+에서 포그라운드 서비스 알림 표시에 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MimicEaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MimicNavGraph()
                }
            }
        }
    }
}
