package com.universal.dm.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Single source of truth for Application metadata, Developer credits,
 * GitHub release update endpoints, and Project donation/support endpoints.
 */
object AppMetadata {
    const val APP_NAME = "Universal Download Manager"
    const val APP_SHORT_NAME = "UDM"
    const val APP_VERSION = "3.6"
    const val BUILD_CHANNEL = "V3.6 Release • Turbo Engine"

    // Developer & Maintainer
    const val DEVELOPER_NAME = "Bijoy"
    const val DEVELOPER_ROLE = "Lead Developer & Maintainer"

    // GitHub Repository & Release Endpoints
    const val GITHUB_OWNER = "bijoykumarmistri"
    const val GITHUB_REPO = "UDM"
    const val GITHUB_DISPLAY_LINK = "github.com/bijoykumarmistri/UDM"
    const val GITHUB_REPO_URL = "https://github.com/bijoykumarmistri/UDM"
    const val GITHUB_RELEASES_URL = "https://github.com/bijoykumarmistri/UDM/releases"
    const val GITHUB_LATEST_RELEASE_URL = "https://github.com/bijoykumarmistri/UDM/releases/latest"
    const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/bijoykumarmistri/UDM/releases/latest"
    const val GITHUB_RELEASES_API = "https://api.github.com/repos/bijoykumarmistri/UDM/releases"

    // Donation & Project Support
    const val DONATION_UPI_ID = "bijoymistri367@okaxis"
    const val DONATION_PAYEE_NAME = "Bijoy"
    const val DONATION_NOTE = "Support UDM Development"

    fun getInstalledVersionName(context: Context): String {
        return runCatching {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: APP_VERSION
        }.getOrDefault(APP_VERSION)
    }

    /**
     * Generates standard Android UPI payment deep link URI.
     * Compatible with Google Pay, PhonePe, Paytm, BHIM, and Cred.
     */
    fun getUpiPaymentUri(amountInr: String? = null): String {
        val base = "upi://pay?pa=$DONATION_UPI_ID&pn=$DONATION_PAYEE_NAME&cu=INR&tn=${java.net.URLEncoder.encode(DONATION_NOTE, "UTF-8")}"
        return if (!amountInr.isNullOrBlank()) "$base&am=$amountInr" else base
    }
}
