package com.universal.dm.domain.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * UDM Brave Shields / AdGuard Ad & Tracker Blocker Engine
 *
 * Provides 3-Tier Protection:
 * 1. Network Layer: Host-based & endpoint path pattern request interception.
 * 2. Cosmetic Layer: Global CSS rules hiding ad slots, banners, and overlays.
 * 3. Scriptlet Layer: YouTube player response sanitizer, ad skipper, and anti-adblock dialog neutralizer.
 */
class AdBlockerEngine private constructor(context: Context? = null) {

    private val blockedHosts = HashSet<String>(600)

    private val blockedPathPatterns = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/googleads",
        "/adservice",
        "/ad_break",
        "/adunit",
        "/doubleclick",
        "/adserver",
        "/ad-delivery",
        "/v1/player/ad",
        "/get_midroll_info",
        "/stats/ads?",
        "/aclk?",
        "/ads.js",
        "/prebid.js",
        "googleadservices.com",
        "googlesyndication.com"
    )

    init {
        // Pre-populate core ad networks, trackers, and popup domains
        val defaultHosts = setOf(
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "pagead2.googleadservices.com",
            "adservice.google.com",
            "googleadservices.com",
            "googlesyndication.com",
            "tpc.googlesyndication.com",
            "google-analytics.com",
            "googletagmanager.com",
            "googletagservices.com",
            "admob.com",
            "2mdn.net",
            "s0.2mdn.net",
            "stats.youtube.com",
            "ads.youtube.com",
            "an.facebook.com",
            "pixel.facebook.com",
            "connect.facebook.net",
            "ads-twitter.com",
            "ads.tiktok.com",
            "aax.amazon-adsystem.com",
            "c.amazon-adsystem.com",
            "fls-na.amazon.com",
            "applovin.com",
            "adcolony.com",
            "unityads.unity3d.com",
            "ironsrc.com",
            "vungle.com",
            "chartboost.com",
            "inmobi.com",
            "taboola.com",
            "trc.taboola.com",
            "outbrain.com",
            "widgets.outbrain.com",
            "zemanta.com",
            "criteo.com",
            "criteo.net",
            "thetradedesk.com",
            "openx.net",
            "rubiconproject.com",
            "pubmatic.com",
            "casalemedia.com",
            "adnxs.com",
            "ib.adnxs.com",
            "bidswitch.net",
            "smartadserver.com",
            "adform.net",
            "yieldmo.com",
            "adroll.com",
            "hotjar.com",
            "mixpanel.com",
            "segment.io",
            "scorecardresearch.com",
            "quantserve.com",
            "branch.io",
            "appsflyer.com",
            "adjust.com",
            "flurry.com",
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "exoclick.com",
            "adsterra.com",
            "juicyads.com",
            "trafficjunky.com",
            "bidvertiser.com",
            "revenuehits.com",
            "yllix.com",
            "adcash.com",
            "onclickads.net",
            "clickadu.com",
            "hilltopads.net",
            "monetag.com",
            "admaven.com",
            "richpush.com",
            "evadav.com",
            "rollerads.com",
            "moatads.com"
        )
        blockedHosts.addAll(defaultHosts)

        // Load comprehensive Brave Shields / AdGuard host list from assets
        context?.let { ctx ->
            try {
                ctx.assets.open("adblock_hosts.txt").use { input ->
                    InputStreamReader(input).forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            blockedHosts.add(trimmed.lowercase())
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to defaultHosts if asset file read encounters an issue
            }
        }
    }

    /**
     * Evaluates whether an incoming HTTP request should be blocked.
     * Evaluates hostname, subdomains, URL path patterns, and query params.
     */
    fun shouldBlockRequest(url: Uri): Boolean {
        val host = url.host?.lowercase() ?: return false
        if (isHostBlocked(host)) return true

        val fullUrl = url.toString().lowercase()
        for (pattern in blockedPathPatterns) {
            if (fullUrl.contains(pattern)) {
                // Safety guard: Never block legitimate media streams on YouTube / video CDNs
                if (fullUrl.contains("videoplayback") && !fullUrl.contains("adformat=")) {
                    continue
                }
                return true
            }
        }

        // Check query parameters commonly used for ad injection or ad formats
        val query = url.query?.lowercase()
        if (!query.isNullOrEmpty()) {
            if (query.contains("adformat=") || query.contains("&ad_type=") || query.contains("ad_tag=")) {
                return true
            }
        }

        return false
    }

    fun isHostBlocked(host: String): Boolean {
        if (blockedHosts.contains(host)) return true

        var subHost = host
        while (subHost.contains('.')) {
            subHost = subHost.substringAfter('.')
            if (blockedHosts.contains(subHost)) {
                return true
            }
        }
        return false
    }

    fun shouldBlockUrl(url: String): Boolean {
        return runCatching {
            shouldBlockRequest(Uri.parse(url))
        }.getOrDefault(false)
    }

    fun createEmptyResourceResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    companion object {
        @Volatile
        private var instance: AdBlockerEngine? = null

        fun getInstance(context: Context): AdBlockerEngine {
            return instance ?: synchronized(this) {
                instance ?: AdBlockerEngine(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Tier 2: Brave Shields Cosmetic Ad-Hiding CSS Injection
         * Strips empty ad banners, sponsored sections, promoted cards, and ad wrappers from DOM.
         */
        const val BRAVE_SHIELD_CSS_JS: String = """javascript:(function() {
            if (document.getElementById('udm-brave-css')) return;
            try {
                var style = document.createElement('style');
                style.id = 'udm-brave-css';
                style.type = 'text/css';
                style.textContent = '.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-message-container,.ytp-ad-action-interstitial,.ytp-ad-image-overlay,.video-ads,#player-ads,ytd-promoted-sparkles-web-renderer,ytd-promoted-video-renderer,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ytm-promoted-video-renderer,#masthead-ad,ytd-in-feed-ad-layout-renderer,ytd-ad-slot-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ins.adsbygoogle,[id^="google_ads_"],[id^="div-gpt-ad"],[class*="ad-banner"],[class*="ad-container"],[class*="ad-wrapper"],[class*="ad_wrapper"],[class*="advertisement"],[class*="ad-slot"],[class*="sponsored-post"],[data-ad-unit],[data-ad-slot],.taboola,.outbrain,.trc_rbox_outer,.trc_related_container,#carbonads,.carbon-wrap,#ad-wrap,.ad-box,.ad_box,.ad-header,.ad-footer,.ad-sidebar,.banner-ad,.native-ad,.pop-ad,.ad-overlay { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; min-height: 0 !important; max-height: 0 !important; margin: 0 !important; padding: 0 !important; }';
                (document.head || document.documentElement).appendChild(style);
            } catch(e) {}
        })();"""

        /**
         * Tier 3: Brave YouTube & Video Shield Scriptlet
         * - Sanitizes YouTube player responses via JSON.parse interceptor
         * - Neutralizes video ad playback (mutes and skips instantly at 16x speed)
         * - Auto-clicks skip buttons
         * - Dismisses YouTube anti-adblock dialogs and restores playback
         */
        const val BRAVE_YOUTUBE_SHIELD_JS: String = """javascript:(function() {
            if (window._udmBraveShieldsActive) return;
            window._udmBraveShieldsActive = true;

            function sanitizePlayerObj(obj) {
                if (!obj || typeof obj !== 'object') return;
                try {
                    delete obj.adPlacements;
                    delete obj.playerAds;
                    delete obj.adSlots;
                    delete obj.adBreakHeartbeatParams;
                    if (obj.playbackTracking) {
                        delete obj.playbackTracking.videostatsPlaybackUrl;
                        delete obj.playbackTracking.videostatsDelayplayUrl;
                        delete obj.playbackTracking.videostatsWatchtimeUrl;
                        delete obj.playbackTracking.qoeUrl;
                    }
                } catch(e) {}
            }

            try {
                var origParse = JSON.parse;
                JSON.parse = function() {
                    var res = origParse.apply(this, arguments);
                    if (res && typeof res === 'object') {
                        sanitizePlayerObj(res);
                    }
                    return res;
                };
            } catch(e) {}

            if (window.ytInitialPlayerResponse) {
                sanitizePlayerObj(window.ytInitialPlayerResponse);
            }
            try {
                var currResp = window.ytInitialPlayerResponse;
                Object.defineProperty(window, 'ytInitialPlayerResponse', {
                    get: function() { return currResp; },
                    set: function(val) {
                        sanitizePlayerObj(val);
                        currResp = val;
                    },
                    configurable: true
                });
            } catch(e) {}

            function purgeVideoAds() {
                try {
                    var dialogs = document.querySelectorAll('ytd-enforcement-message-view-model, tp-yt-paper-dialog');
                    for (var i = 0; i < dialogs.length; i++) {
                        var d = dialogs[i];
                        if (d.textContent && (d.textContent.indexOf('Ad blockers') !== -1 || d.textContent.indexOf('Terms of Service') !== -1)) {
                            d.remove();
                            var v = document.querySelector('video');
                            if (v && v.paused) v.play();
                        }
                    }

                    var isAd = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay, .ytp-ad-preview-text');
                    var video = document.querySelector('video');
                    if (video && isAd) {
                        video.muted = true;
                        video.playbackRate = 16.0;
                        if (isFinite(video.duration) && video.duration > 0) {
                            video.currentTime = video.duration;
                        }
                    }

                    var skipBtns = [
                        '.ytp-ad-skip-button',
                        '.ytp-ad-skip-button-modern',
                        '.ytp-skip-ad-button',
                        '.videoAdUiSkipButton',
                        'button.ytp-ad-skip-button-text',
                        '.ytp-ad-overlay-close-button'
                    ];
                    for (var j = 0; j < skipBtns.length; j++) {
                        var btn = document.querySelector(skipBtns[j]);
                        if (btn) {
                            btn.click();
                            break;
                        }
                    }
                } catch(e) {}
            }

            setInterval(purgeVideoAds, 250);

            try {
                var observer = new MutationObserver(purgeVideoAds);
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.body) observer.observe(document.body, { childList: true, subtree: true });
                    });
                }
            } catch(e) {}
        })();"""
    }
}
