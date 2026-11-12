package com.universal.dm.core

/**
 * Single source of truth for Application metadata, Developer credits,
 * and Project donation/support endpoints.
 */
object AppMetadata {
    const val APP_NAME = "Universal Download Manager"
    const val APP_SHORT_NAME = "UDM"
    const val APP_VERSION = "1.0.0"
    const val BUILD_CHANNEL = "Stable Production"

    // Developer & Maintainer
    const val DEVELOPER_NAME = "Bijoy"
    const val DEVELOPER_ROLE = "Lead Developer & Maintainer"

    // Donation & Project Support
    const val DONATION_UPI_ID = "bijoymistri367@okaxis"
    const val DONATION_PAYEE_NAME = "Bijoy"
    const val DONATION_NOTE = "Support UDM Development"

    /**
     * Generates standard Android UPI payment deep link URI.
     * Compatible with Google Pay, PhonePe, Paytm, BHIM, and Cred.
     */
    fun getUpiPaymentUri(amountInr: String? = null): String {
        val base = "upi://pay?pa=$DONATION_UPI_ID&pn=$DONATION_PAYEE_NAME&cu=INR&tn=${java.net.URLEncoder.encode(DONATION_NOTE, "UTF-8")}"
        return if (!amountInr.isNullOrBlank()) "$base&am=$amountInr" else base
    }
}
