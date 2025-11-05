# 🎙️ TwinMind Voice Recorder

## Overview
TwinMind Voice Recorder is an Android application designed for efficient audio recording, chunked transcription, and AI-powered summary generation using the Google Gemini API. It leverages Room database for persistent storage and Jetpack Compose for a modern UI experience.

<p align="center">
	<img src="https://img.icons8.com/fluency/96/000000/voice-recorder.png" alt="Voice Recorder" width="80"/>
	<img src="https://img.icons8.com/fluency/96/000000/summary-list.png" alt="Summary" width="80"/>
</p>

- **Important:** For details about classes, files, and function properties, refer to the documentation and class information.
## ⚙️ Minimum Requirements
- Android SDK 24+

## 🚀 What You'll Build
Three core features:
1. 🎤 **Record Audio Robustly**
	- Background recording with explicit interruptions handling
2. 📝 **Generate Transcript**
	- Convert audio to transcript
3. 📄 **Generate Summary**
	- Create structured summary from transcript

## 🛠️ Feature Details & Requirements

### 1. 🎤 Record Audio
- Foreground service for audio recording
- Split into 30-second audio chunks (with ~2-second overlap for continuity)
- Save chunks to local storage
- Persistent notification with Stop action
- Handles:
	- 📞 Incoming/outgoing phone calls (pause/resume, show status)
	- 🔇 Audio focus loss (pause/resume, show status)
	- 🎧 Microphone source changes (Bluetooth/wired headset, show notification)
	- 💾 Low storage (check before start, stop gracefully, show error)
	- 💀 Process death recovery (persist session state in Room, finalize chunk, resume transcription)
	- 🤫 Silent audio detection (warn after 10s silence)
- Live updates (Android 16+):
	- ⏱️ Recording timer
	- 🟢 Status indicator
	- ⏸️ Pause/Stop actions
	- 🎬 Visual indicator (icon)
- UI:
	- 🖥️ Single recording/post meeting screen
	- ⏺️ Record/Stop button
	- ⏲️ Timer
	- 🟢 Status indicator
	- 📋 Dashboard listing all meetings

### 2. 📝 Generate Transcript
- Upload chunks as soon as each 30s chunk is ready
- Uses Google Gemini 2.5 Flash (commented code for free version, limit <50kb)
- Save transcript to Room database (single source of truth)
- Ensure transcript order and chunk integrity

### 3. 📄 Generate Summary
- Send transcript to LLM API (Gemini)
- Generate structured summary
- Show specific error messages for failures

---

## 🏆 Accomplishments
- Used Gemini for both transcription and summary
- For transcription, use commented code for free Google AI Studio (limit <50kb)
- Added icons for UI clarity


## ✨ Features
- 🎤 Audio recording and session management
- 📝 Chunked audio transcription using Gemini API (`gemini-pro:generateContent`)
- 📄 AI-generated structured summaries from transcripts
- 💾 Persistent storage of transcripts and summaries via Room database
- 🖥️ Clean, minimal UI with navigation between transcript and summary screens
- 🛡️ Robust error handling and demo fallback for API/network issues

## 🧰 Technologies Used
- Kotlin
- Android Jetpack (Room, ViewModel, LiveData, Navigation)
- Jetpack Compose
- OkHttp (for API calls)
- Google Gemini API (v1beta)
- Gradle

## 📦 Installation Guide

### Prerequisites
- 🛠️ Android Studio (latest recommended)
- 📱 Android SDK 33+
- 🔑 Google Gemini API key (obtain from Google AI Console)

### Steps
1. **Clone the repository:**
	```sh
	git clone https://github.com/akv-iu/New-TwinMind.git
	cd New-TwinMind
	```
2. **Open in Android Studio:**
	- File > Open > Select the `New-TwinMind` folder.
3. **Configure API Key:**
	- Add your Gemini API key to `local.properties`:
	  ```
	  GEMINI_API_KEY=your_api_key_here
	  ```
	- Or update the key in the code (see `TranscriptionService.kt`).
4. **Build the project:**
	- Click "Build" or run:
	  ```sh
	  ./gradlew assembleDebug
	  ```
5. **Run on device/emulator:**
	- Select a device and click "Run" in Android Studio.

## 📖 Usage
- 🎤 Record audio sessions and view transcripts.
- 📄 Generate AI-powered summaries from transcripts.
- 🖥️ Navigate between transcript and summary screens.

## © Copyright
Copyright © 2025 akv-iu. All rights reserved.

This software and its source code are licensed for personal and educational use only. Commercial use, redistribution, or modification without explicit permission is prohibited.

## 📝 License
See LICENSE file for details (if available).

---

## 🔜 Next Focus Areas

### 1. 📄 Generate Summary (Streaming & Resilience)
- Stream structured summary in the UI as the response arrives
- Continuously update the UI with new summary content
- Ensure summary generation continues and completes even if the app is killed (use WorkManager or foreground service for resilience)

### 2. 🎤 Record Audio (Live Updates & Lock Screen)
- Show live recording status on the lock screen (Android 16+)
- Display recording timer (updates every second)
- Indicate current status: "Recording", "Paused - Phone call"
- Provide Pause/Stop actions directly from the lock screen
- Show a visual recording icon

---

## 🗄️ Database Evolution & Usage Notes

- The Room database schema has evolved to support chunked audio, transcripts, and summaries.
- **Important:** There is currently no version control or error handling for schema/data changes. If you change the data model, you must delete all database elements before reopening the app, or it may crash.


---
<p align="center">
	<b>For questions or contributions, please contact the repository owner via GitHub.</b>
</p>
