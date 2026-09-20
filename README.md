<div align="center">

# 💗 si firda lovely

**A Frida GUI for rooted Android — hook apps without leaving your phone.**

Magisk · KernelSU · APatch · Android 7.0+

[![Telegram](https://img.shields.io/badge/Telegram-%40Gieneticc-2CA5E0?logo=telegram&logoColor=white)](https://t.me/Gieneticc)
[![GitHub](https://img.shields.io/badge/Repo-si--firda--lovely-181717?logo=github)](https://github.com/giienew/si-firda-lovely)
[![Root](https://img.shields.io/badge/Requires-Root-E91E63?logo=android&logoColor=white)](https://github.com/topjohnwu/Magisk)

*Powered by GIENETIC*

</div>

---

## 📖 About

**si firda lovely** is a native Android app that puts a full Frida workflow in your pocket. No laptop, no Termux gymnastics — pick an app, pick a script, hit RUN. The live terminal follows you in Picture-in-Picture while the hook does its work.

Built for mobile pentesters, reverse engineers, and the chronically curious.

## ✨ Features

- 🎯 **App Picker** — Choose targets from your installed and system apps. No package-name typing, no `adb shell pm list packages`.
- 📑 **Multi-Script Tabs** — Run several `.js` scripts at once, each in its own tab with its own Frida session.
- 🖼️ **Picture-in-Picture Terminal** — Press home and the terminal stays on screen. Watch logs while you use the target app.
- 📜 **Live Terminal** — Realtime stdout/stderr output in a monospace console, pink-on-dark anime-styled UI.
- 🔌 **Frida Server Control** — One toggle to start/stop `frida-server`. Auto-downloads the latest official build for your CPU architecture (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- 🔐 **Root Gate** — Nothing executes until root is confirmed. Supports Magisk, KernelSU, and APatch.
- ⚙️ **Settings Page** — Root status, installed Frida version, device CPU arch, and About — all in one place.
- 🪝 **Bundled Script** — `endpoint_capture.js` ships with the app: hooks `okhttp3`, `HttpURLConnection`, and `WebView` to dump network endpoints on the fly.

## 📱 Screenshots

> Screenshots coming soon — grab the APK and take your own! 📸


## 🛠️ Requirements

| Requirement | Detail |
|---|---|
| **Android** | 7.0 (API 24) or higher |
| **Root** | Magisk, KernelSU, or APatch — mandatory |
| **frida-server** | Installed on device — the app can auto-install it for you |
| **Scripts** | Any Frida `.js` hook script |

## 📥 Installation

1. Download the latest APK from [GitHub Releases](https://github.com/giienew/si-firda-lovely/releases).
2. Install the APK. If Android blocks it, allow **"Install unknown apps"** for your browser or file manager.
3. Open the app and grant the **root request** when prompted.
4. Done — the app handles the rest.

### Building from source

```bash
git clone https://github.com/giienew/si-firda-lovely.git
cd si-firda-lovely
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or use the included **GitHub Actions** workflow — push to the repo and grab the APK from the **Artifacts** section.

## 🚀 Usage

```
Grant root  →  Frida Server ON  →  Pick app  →  Pick script  →  RUN  →  Press Home  →  PiP follows
```

1. **Grant root.** The app blocks everything until a root shell is confirmed.
2. **Switch Frida Server ON.** First run auto-downloads the matching `frida-server` build.
3. **Pick a target app** from the installed / system app list.
4. **Pick a script** — choose a local `.js` file, or start with the bundled `endpoint_capture.js`.
5. **Hit RUN.** The terminal opens and streams output live.
6. **Press Home.** The terminal shrinks into Picture-in-Picture and keeps logging while you use the target app.

To run multiple scripts at once, open a new tab — each tab runs its own independent Frida session.

## 🔧 Troubleshooting

<details>
<summary><b>Root not detected</b></summary>

Open Magisk / KernelSU / APatch SuperUser list and make sure **si firda lovely** has root granted. Some setups need a reboot after granting. KernelSU users: confirm the app isn't excluded from the su list.
</details>

<details>
<summary><b>Scripts connect but no output</b></summary>

Your hook fires only when the target app triggers the code path. For `endpoint_capture.js`, the app must make a network request — browse a page, reload a feed, or trigger a login, then watch the terminal.
</details>

<details>
<summary><b><code>frida -D</code> works from PC but app fails</b></summary>

Use `-D` (device) instead of `-U` (USB). With frida-server running locally on the phone, the PC should target the device over the network or ADB, not USB-attached mode. Same reason the app runs a local server instance rather than relying on a USB connection.
</details>

<details>
<summary><b>frida-server won't start</b></summary>

Check the Settings page: verify your **CPU arch** and that the downloaded version matches. If it still fails, clear the app's downloaded server files and let it re-download, or install `frida-server` manually via your root shell.
</details>

<details>
<summary><b>PiP not showing</b></summary>

Enable **Picture-in-Picture** permission for the app in Android Settings → Apps → si firda lovely. PiP requires the terminal session to be actively running.
</details>

## 🧩 Bundled Script

**`endpoint_capture.js`** — zero-config network endpoint capture.

Hooks:
- `okhttp3` — OkHttp request/response URLs, methods, and bodies
- `HttpURLConnection` — classic `URL`/`URLConnection` traffic
- `WebView` — page loads and internal navigation

Use it as-is to map an app's API surface, or as a template for your own hooks.

## 👤 Developer

<div align="center">

**GIENETIC**

Telegram: [@Gieneticc](https://t.me/Gieneticc)

Repo: [github.com/giienew/si-firda-lovely](https://github.com/giienew/si-firda-lovely)

</div>

---

<div align="center">

⚠️ **For educational and authorized security testing only.**  
You are responsible for complying with all applicable laws. Only hook apps you own or have explicit permission to test.

</div>
