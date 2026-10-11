<p align="center">
  <img src="assets/icon.png" alt="Spartan CE Logo" width="128" height="128" style="border-radius: 24px;" />
</p>

<h1 align="center">Spartan CE</h1>

<p align="center">
  <b>A lightweight, tactical mobile engine hub & adaptive touch controller layer.</b>
</p>

<p align="center">
  <a href="https://github.com/devlwte/spartance/releases/latest"><img src="https://img.shields.io/github/v/release/devlwte/spartance?color=00E5FF&label=Release&style=flat-square" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%209.0%2B-blue?style=flat-square" alt="Platform: Android 9+" />
  <img src="https://img.shields.io/badge/Architecture-ARM64--v8a-emerald?style=flat-square" alt="Arch: ARM64" />
  <img src="https://img.shields.io/badge/Author-%40kyrnforge-cyan?style=flat-square" alt="Author: @kyrnforge" />
  <img src="https://img.shields.io/badge/License-Proprietary-lightgrey?style=flat-square" alt="License" />
</p>

---

## 🌟 About Spartan CE

**Spartan CE** was crafted out of a passion for bringing classic desktop-grade gameplay to mobile devices without compromising control precision, ergonomics, or visual elegance. 

Built from the ground up by **@kyrnforge**, this launcher acts as a standalone runner and tactical input bridge. It empowers players to experience compatible game data archives with native ARM64 speed, fluid multitouch aiming, and a fully customizable virtual HUD that feels responsive and natural right under your thumbs.

---

## ⚡ Key Highlights

### 🎮 Tactical Multitouch HUD Controller
* **Authentic Action Button Layout:** Clean, recognizable high-contrast circular badges (**A, B, X, Y**) designed specifically for tactile thumb recognition.
* **Simultaneous Aim & Fire (Drag-to-Aim):** The dedicated ballistic fire controls allow you to seamlessly aim, sweep your crosshairs, and track targets while holding down fire—zero latency, zero camera snapping.
* **Fully Customizable Layout:** Move, scale, and adjust the opacity of any on-screen button to match your hand size and grip style.
* **Haptics & Motion Gyroscope:** Integrated optional rumble feedback and gyro-assisted precision aiming for competitive accuracy.

### 🌗 Minimalist Dual Theme Hub
* **Instant Theme Toggle:** Switch seamlessly between **Obsidian Dark Mode** (`#0A0D14`) for night gaming and **Ceramic Light Mode** (`#F4F6F9`) for daylight clarity.
* **Persistent Settings:** Your preferences, sensitivity multipliers, and button layouts are automatically saved and restored across sessions.

### 💿 Smart Archive & Multi-Language Detection
* **One-Tap Setup:** Automatically scans and detects compatible game image archives (`.iso` / `.xiso`).
* **Multi-Language Selector:** Detects localized audio and text map packs (Spanish, Italian, English, etc.), allowing you to choose your preferred language directly from the launcher.

### 📡 Dynamic Tactical Status HUD
* **Non-Intrusive Overlay:** A sleek, fixed tactical pill indicator in the top-right corner keeps you updated on engine status.
* **Rotating Community Messages:** Features dynamic session messages and version highlights with automatic offline fallback and optional cloud synchronization.

---

## 📲 Installation & Setup

1. **Download the APK:**
   Head over to the [**Releases**](https://github.com/devlwte/spartance/releases/latest) section and download `Spartan_CE_v1.0.0.apk`.
2. **Install on Android:**
   Open the downloaded APK on your Android device (Android 9.0 or higher, 64-bit ARM processor required).
3. **Provide Your Game Archive:**
   Launch **Spartan CE**, tap **Seleccionar Archivo ISO**, and choose your legitimate personal game archive.
4. **Deploy & Play:**
   Select your preferred audio language and tap **Iniciar Spartan CE** to jump straight into the action!

## 🛠️ Compilación desde el Código Fuente (Build from Source)

Si deseas clonar el proyecto en otro equipo y compilar el APK tú mismo:

1. **Requisitos previos:**
   * **Java JDK 17+** (o el JDK incluido con Android Studio).
   * **Android SDK** (API Platform 34 / Build-Tools).

2. **Clonar y compilar:**
   ```bash
   git clone https://github.com/devlwte/spartance.git
   cd spartance
   ```
   * En **Windows**:
     ```cmd
     gradlew.bat assembleRelease
     ```
   * En **Linux / macOS**:
     ```bash
     chmod +x gradlew
     ./gradlew assembleRelease
     ```

3. **Ubicación del APK resultante:**
   El paquete firmado listo para instalar se genera automáticamente en:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

---

## 🔒 Legal Disclaimer

> **Note on Legality & Intellectual Property:**  
> Spartan CE is an independent software tool and touch input interface developed strictly for educational, personal research, and launcher convenience purposes.  
> 
> This repository **does NOT contain, host, or distribute any proprietary assets, game archives, copyrighted textures, audio files, or game executables**. Users must provide their own legally acquired game archives. All trademarks, trade names, and related rights belong to their respective owners.

---

## 👤 Credits & Author

* **Architecture, Design & Systems:** **[@kyrnforge](https://github.com/devlwte)**
* For updates, questions, and new features, follow and star this project!

---
<p align="center">
  <sub>Built with passion by @kyrnforge • Spartan CE Mobile Edition</sub>
</p>
