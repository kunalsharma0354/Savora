# Savora

Savora is an Android application for downloading media from YouTube (videos, Shorts) and Instagram (Reels) directly to your device. It requires no manual input beyond pasting a link: the app detects the platform, resolves the available formats automatically, and saves the media to the Downloads folder.

This app is built purely for problem-solving and learning purposes and is not intended for commercial distribution.

The app follows a strict black-and-white design language inspired by the NEXORA brand system: glassmorphism cards, pill-shaped controls, gradient typography, and white glow accents.

## Features

- **Fully automatic resolution** - paste a link and the app handles everything: platform detection, format discovery, and download.
- **Three download modes**:
  - Video + Audio - MP4 with sound (YouTube DASH streams merged via MediaMuxer)
  - Video Only - silent MP4 (audio track stripped at runtime)
  - Audio Only - M4A/MP3 audio file (audio extracted at runtime)
- **Real-time quality options** - available resolutions are read from the actual video response (up to 4K where available), not a hardcoded list.
- **Audio bitrate selection** - 320 kbps and 128 kbps options for audio-only downloads.
- **Instagram support** - Reels are fetched through an automatic public service; the best available format is selected automatically.
- **Accurate preview card** - shows the real video title, estimated file size, and quality before saving.
- **Open File shortcut** - after saving, the button becomes "Open File" to launch the saved media immediately.
- **No silent overwrites** - if a file with the same name already exists, a numbered suffix is appended.
- **State-safe UI** - survives rotation and interruptions without getting stuck.

## Requirements

- Android 10 (API 29) or newer
- Internet connection
- Notification permission (Android 13+) for download progress
- Storage permission only on Android 10 and below; on newer versions media is saved via MediaStore

## Supported Platforms

| Platform | Link formats | Notes |
| --- | --- | --- |
| YouTube | youtube.com/watch, youtu.be, youtube.com/shorts | DASH formats, progressive fallback, merge support |
| Instagram | instagram.com/reel | Best available format, auto-selected |

Other video links fall back to a generic download service; availability depends on the third-party service.

## Tech Stack

- Kotlin, Jetpack Compose (Material 3)
- MediaExtractor / MediaMuxer for on-device video and audio processing (no FFmpeg)
- AndroidX Activity, Lifecycle (repeatOnLifecycle)
- YouTube Innertube API (ANDROID client) for format resolution
- MediaStore for saving to Downloads

## Build

### 1. Add the YouTube API key

The YouTube Innertube key is loaded from `local.properties` at build time and is never committed to the repository. Create or edit `local.properties` in the project root and add:

```
YOUTUBE_API_KEY=your_key_here
```

Without a valid key, YouTube downloads will fail with an error message; Instagram downloads still work.

### 2. Build the debug APK

```bash
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/java/com/nexora/savora/
├── MainActivity.kt          # Edge-to-edge setup
├── SaveoraApp.kt            # Permission flow + scaffold + theme
├── data/
│   ├── YouTubeApi.kt        # Innertube client, DASH format parsing, stream resolution
│   ├── InstagramApi.kt      # Automatic reel resolver
│   ├── DownloadEngine.kt    # Resolve routing, downloads, MediaStore saves
│   └── MediaMuxerHelper.kt  # Merge / strip / extract media tracks
├── model/
│   └── DownloadModels.kt    # Modes, phases, filename builders
├── ui/
│   ├── components/Glass.kt  # Glass cards, gradient buttons, chips
│   ├── icons/AppIcons.kt    # Custom vector icons
│   ├── screens/HomeScreen.kt
│   ├── screens/PermissionScreen.kt
│   └── theme/               # Colors, typography, theme
```

## Permissions

| Permission | When | Purpose |
| --- | --- | --- |
| POST_NOTIFICATIONS | Android 13+ | Download progress notifications |
| WRITE_EXTERNAL_STORAGE | Android 10 and below | Saving media to device storage |

Permissions are requested once during the first-run setup and can be skipped; the app re-asks for skipped permissions on subsequent launches until granted.

## Notes

- YouTube downloads use the official Innertube player endpoint with an Android client key; no account is required.
- Instagram downloads rely on a third-party public service which may change or become unavailable at any time.
- High-quality DASH videos encoded with AV1 require Android 14+ for merging; on older devices the app suggests picking a lower quality instead.

## License

Private project. All rights reserved.