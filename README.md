# UDM - Universal Download Manager & Shielded Web Browser

<p align="center">
  <b>High-Performance Native Android Download Accelerator, Multi-Engine Media Sniffer & Brave Shields-Grade Ad-Blocker</b>
</p>

<p align="center">
  <a href="https://github.com/bijoykumarmistri/UDM/releases/latest">
    <img src="https://img.shields.io/github/v/release/bijoykumarmistri/UDM?color=blue&label=Download%20APK&style=for-the-badge" alt="Download Latest APK" />
  </a>
</p>

---

### 📥 Direct Download (APK)
Users can download and install the latest ready-to-run APK directly from the **[Releases](https://github.com/bijoykumarmistri/UDM/releases)** section.
- **Direct Download link**: [Latest Release APK](https://github.com/bijoykumarmistri/UDM/releases/latest)

---

## 🌟 Key Features

### 🚀 High-Speed Download Accelerator
- **Multi-Part Parallel Threading**: Segmented streaming engine for accelerated download speeds.
- **Accurate Real-Time Math**: Dynamic byte calculation, progress tracking, remaining time estimates, and transfer speed stats.
- **Robust Codec Detection**: Native resolution selector supporting AV1, VP9, H.264, AAC, and Opus.

### 🛡️ Brave Shields & AdGuard-Grade Ad-Blocker
- **3-Tier Protection**:
  1. **Network Level**: Host blocklist (500+ domains) and URL path pattern filtering (`/api/stats/ads`, `/pagead/`, `/adservice`).
  2. **Cosmetic DOM Hiding**: Removes banners, promoted videos, sponsored posts, and AdSense blocks cleanly.
  3. **Scriptlet Defense**: Auto-skips video ads, strips player telemetry, and neutralizes anti-adblock modals.

### 🌐 Elegant Modern Browser
- **Incognito & Multi-Tab Support**: Isolated private browsing mode and fast visual tab switching.
- **Search Engine Selector**: Instant switching across Google, DuckDuckGo, Bing, Brave, and Startpage.
- **HTML5 Desktop & Mobile Modes**: Fullscreen video support, pinch zoom, and SPA history navigation detection.

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose & Material 3
- **Architecture**: MVVM with reactive StateFlow & Coroutines
- **Networking**: OkHttp 4 / HttpURLConnection
- **Target SDK**: Android 15 / 16 (API 31 - 37)

---

## 📦 Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/UDM.git
   ```
2. Open the project in **Android Studio**.
3. Build and run debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License
This project is licensed under the Apache License 2.0.
