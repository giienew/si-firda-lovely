# LSPosed vs Frida — verdict for si firda lovely

## TL;DR
Tetap Frida standalone. LSPosed module terlalu mahal untuk use case ini.

## Comparison

| Aspect | LSPosed (Xposed) | Frida standalone (current) |
|---|---|---|
| Target flexibility | Must write Java hooks in module, compile, reboot per change | Any .js script, hot reload, runtime |
| Script ecosystem | Custom code per module | Frida script marketplace (thousands) |
| Setup | LSPosed + Riru/Zygisk + module + reboot | frida-server binary only |
| Detection | Widely detected by banking apps (SafetyNet/Play Integrity flag LSPosed) | frida-server detectable but frida-gadget + server renaming easier to evade |
| Persistence | Hooks survive reboot automatically | frida-server must restart (app has switch) |
| Performance | Zygisk injection is fast | frida-server slightly heavier |
| Maintenance | Must maintain module against LSPosed API changes | Frida updates = replace binary |
| Multi-target | Module applies globally or per-app manually | Any app, any time, no reboot |

## Why Frida wins here

1. **Script marketplace** — user request "tangkap endpoint" = 1 script (endpoint_capture.js).
   Same in LSPosed = compile custom module, sideload, reboot, test, repeat.
2. **Hot reload** — script edit, re-run, no reboot. LSPosed requires reboot per change.
3. **frida -D local** — runs entirely on-device, no PC needed. That's the whole point
   of this app.
4. **Detection** — LSPosed gets flagged by Play Integrity (banking apps detect it).
   Frida is also detectable, but renaming frida-server + gadget mode evasion is
   well-documented.
5. **Maintenance** — one binary. LSPosed module = build system + module API + compat.

## When LSPosed WOULD be the answer

- Need hooks that survive reboot permanently
- Targeting only 1-2 specific apps with stable, never-changing hooks
- Want zero runtime overhead

## Verdict: keep Frida

The app's core value = pick any app, load any script, see output live. LSPosed
fundamentally cannot do this.
