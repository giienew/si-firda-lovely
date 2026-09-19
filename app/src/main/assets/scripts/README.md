# Bundled Frida Scripts — si firda lovely

Ready-to-run Frida scripts shipped inside the APK. No download needed.

All scripts are single self-contained `.js` files with no external dependencies (no `require()`, no npm). Run with:

```
frida -U -f <package> -l <script.js>
```

Tested against frida-server 16.x and 17.x.

| File | Purpose | Source | License | Original file | Size |
|---|---|---|---|---|---|
| `ssl_pinning_bypass.js` | SSL Pinning Bypass (multi-stack) — Bypasses OkHttp3/4, Conscrypt, TrustKit, TrustManagerImpl, X509TrustManager, Apache HttpClient, Cronet, gRPC, Retrofit, HttpsURLConnection, NetworkSecurityConfig, react-native-ssl-pinning. | https://github.com/hits313/alab | MIT | `ssl-multi-unpin.js` | 7120 B |
| `root_bypass.js` | Root Detection Bypass — Bypasses RootBeer, su binary, busybox, Magisk paths, test-keys, Superuser.apk, dangerous props, basic SafetyNet. | https://github.com/hits313/alab | MIT | `root-bypass.js` | 6816 B |
| `rasp_bypass.js` | RASP / Anti-Frida Bypass — Bypasses anti-tamper, anti-debug, Frida/Emulator detection, SafetyNet/Play Integrity attestation, flag SECURE. | https://github.com/hits313/alab | MIT | `rasp-bypass.js` | 6654 B |
| `webview_debug.js` | WebView Remote Debug Enabler — Forces WebView.setWebContentsDebuggingEnabled(true) for chrome://inspect. | https://github.com/hits313/alab | MIT | `webview-debug.js` | 1597 B |
| `biometric_bypass.js` | Biometric Auth Bypass — Hooks BiometricPrompt / FingerprintManager to always report successful auth. | https://github.com/hits313/alab | MIT | `biometric-bypass.js` | 3578 B |
| `universal_ssl_unpin.js` | Universal SSL Unpinning (lite) — Lite fallback: TrustManager, HttpsURLConnection, WebViewClient, OkHttp3. | https://github.com/hits313/alab | MIT | `universal-ssl-unpin.js` | 2916 B |
| `app_info_enum.js` | Java Class & Method Enumerator — Enumerates loaded Java classes/methods; search classes by keyword. Runtime app recon (component discovery). | https://github.com/0xdea/frida-scripts | MIT | `raptor_enum.js` | 2878 B |
| `method_tracer.js` | Java Method Hook Tracer — Hooks and traces Java method calls of matching classes with args/return values. | https://github.com/0xdea/frida-scripts | MIT | `raptor_trace.js` | 5467 B |
| `dex_dumper.js` | DEX Dumper (fork-protected) — Blocks fork() anti-debug then dumps in-memory DEX files from the app process. | https://github.com/Alexjr2/Android_Dump_Dex | MIT | `dumpdex.js` | 6857 B |

## Attribution

Each file retains its original script body unmodified below the `si firda lovely` attribution header. See each file header for the exact upstream source and license.

- **hits313/alab** (MIT) — https://github.com/hits313/alab — android/ssl-multi-unpin.js, android/root-bypass.js, android/rasp-bypass.js, android/webview-debug.js, android/biometric-bypass.js, universal-ssl-unpin.js
- **0xdea/frida-scripts** (MIT, Copyright (c) 2017-2025 Marco Ivaldi <raptor@0xdeadbeef.info>) — https://github.com/0xdea/frida-scripts — raptor_frida_android_enum.js, raptor_frida_android_trace.js
- **Alexjr2/Android_Dump_Dex** (MIT) — https://github.com/Alexjr2/Android_Dump_Dex — DumpDex.js
