package com.universal.dm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.universal.dm.ui.navigation.UdmAppShell
import com.universal.dm.ui.settings.AppSettingsState
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.UDMTheme

class MainActivity : ComponentActivity() {

    private val sharedUrlState = mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted/denied callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        requestAppPermissions()

        setContent {
            val appSettings = remember { AppSettingsState(this@MainActivity) }
            val sharedUrl by sharedUrlState

            CompositionLocalProvider(LocalAppSettings provides appSettings) {
                UDMTheme {
                    UdmAppShell(
                        initialSharedUrl = sharedUrl,
                        onSharedUrlHandled = { sharedUrlState.value = null }
                    )
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Notification permission for download progress on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Storage permissions for saving/accessing downloaded files
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Android 13+ Granular Media Permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val action = incomingIntent.action
        val type = incomingIntent.type

        val extractedUrl: String? = when (action) {
            Intent.ACTION_SEND -> {
                if (type == "text/plain" || type?.startsWith("text/") == true) {
                    val text = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                    extractFirstUrl(text)
                } else null
            }
            Intent.ACTION_VIEW -> {
                incomingIntent.dataString?.let { extractFirstUrl(it) }
            }
            else -> null
        }

        if (!extractedUrl.isNullOrBlank()) {
            sharedUrlState.value = extractedUrl
        }
    }

    private fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val regex = Regex("""https?://[^\s<>"{}|\\^`\[\]]+""")
        return regex.find(text)?.value
            ?: if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) text.trim() else null
    }
}
