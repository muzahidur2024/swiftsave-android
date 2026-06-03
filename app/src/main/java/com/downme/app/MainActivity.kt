package com.downme.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.downme.app.ui.DownMeApp

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var pendingSharedDownloadUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingSharedDownloadUrl = intent.sharedDownloadUrl()
        requestNotifIfNeeded()
        setContent {
            DownMeApp(
                pendingSharedDownloadUrl = pendingSharedDownloadUrl,
                onPendingSharedDownloadUrlConsumed = { pendingSharedDownloadUrl = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSharedDownloadUrl = intent.sharedDownloadUrl()
    }

    private fun requestNotifIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ok =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!ok) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun Intent?.sharedDownloadUrl(): String? =
        this?.getStringExtra(EXTRA_SHARED_DOWNLOAD_URL)?.takeIf { it.isNotBlank() }

    companion object {
        const val EXTRA_SHARED_DOWNLOAD_URL = "com.downme.app.extra.SHARED_DOWNLOAD_URL"
    }
}
