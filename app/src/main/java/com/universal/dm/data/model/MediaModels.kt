package com.universal.dm.data.model

enum class MediaType {
    VIDEO,
    AUDIO,
    DIRECT_FILE
}

enum class VideoCodec(val displayName: String, val tag: String) {
    AV1("AV1 (High Efficiency)", "av01"),
    H264("H.264 (Universal)", "avc1"),
    VP9("VP9 (WebM/MP4)", "vp9"),
    OTHER("Standard", "unknown")
}

enum class AudioCodec(val displayName: String, val extension: String, val tag: String) {
    M4A("M4A (AAC Audio)", "m4a", "mp4a"),
    OPUS("Opus (High Clarity)", "opus", "opus"),
    MP3("MP3 (Compatible)", "mp3", "mp3")
}

data class StreamVariant(
    val formatId: String,
    val url: String,
    val mediaType: MediaType,
    val qualityLabel: String,
    val resolutionHeight: Int,
    val bitrateKbps: Int,
    val videoCodec: VideoCodec?,
    val audioCodec: AudioCodec?,
    val containerExt: String,
    val fileSizeBytes: Long,
    val requiresAudioMerge: Boolean = false,
    val pairedAudioStreamUrl: String? = null,
    val pairedAudioFormatId: String? = null,
)

data class DetectedPageMedia(
    val pageTitle: String,
    val pageUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val videoVariants: List<StreamVariant>,
    val audioVariants: List<StreamVariant>,
    val directFileUrl: String? = null,
    val directFileVariant: StreamVariant? = null,
) {
    val isDirectFile: Boolean
        get() = directFileVariant != null || (videoVariants.isEmpty() && audioVariants.isEmpty() && directFileUrl != null)
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    MERGING_MUXING,
    PAUSED,
    COMPLETED,
    FAILED
}
