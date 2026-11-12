package com.universal.dm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.universal.dm.ui.navigation.UdmAppShell
import com.universal.dm.ui.settings.AppSettingsState
import com.universal.dm.ui.settings.LocalAppSettings
import com.universal.dm.ui.theme.UDMTheme

class MainActivity : ComponentActivity() {

    private val sharedUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val appSettings = remember { AppSettingsState() }
            val isDark = appSettings.isDarkThemeEnabled ?: isSystemInDarkTheme()
            val sharedUrl by sharedUrlState

            CompositionLocalProvider(LocalAppSettings provides appSettings) {
                UDMTheme(
                    darkTheme = isDark,
                    dynamicColor = appSettings.isDynamicColorEnabled,
                ) {
                    UdmAppShell(
                        initialSharedUrl = sharedUrl,
                        onSharedUrlHandled = { sharedUrlState.value = null }
                    )
                }
            }
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
