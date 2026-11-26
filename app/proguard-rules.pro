# Preserve WebView @JavascriptInterface bridge methods (UdmMediaBridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# YoutubeDL & FFmpeg runtime wrappers and deserialized video info models
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
