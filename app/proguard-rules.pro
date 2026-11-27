# Preserve WebView @JavascriptInterface bridge methods (UdmMediaBridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# YoutubeDL, FFmpeg, and common runtime wrappers / deserialized video info models
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Jackson annotations used by YoutubeDL ObjectMapper deserialization
-keep class com.fasterxml.jackson.annotation.** { *; }
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}

# Apache Commons Compress instantiates ZipExtraField subclasses via reflection in ExtraFieldUtils.<clinit>()
-keep class * implements org.apache.commons.compress.archivers.zip.ZipExtraField { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-dontwarn org.apache.commons.compress.**
