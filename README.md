# Simple Browser

A lightweight, fast browser for Android TV and Google TV, built around the remote control.

Simple Browser started as a fork of [TV Bro](https://github.com/truefedex/tv-bro), by Fedir Tsapana (c) 2019, under the original project license. It gained its own identity, a curated home, and a focused direction: distribute through the system WebView.

---

## Why Simple Browser

A TV means a big screen, a limited remote, and the expectation of instant response. Simple Browser delivers exactly that, asking for nothing but an install.

**Curiosity without friction.** Open any site and move with the remote's arrow keys. No mouse, no typing, no menu maze. Spatial cursor plus D-pad navigation let you explore the way you do on a phone, only from the couch.

**Security that lets you know what you touch.** Private mode, Safe Browsing always on, built-in ad blocking, and per-site permission control. You decide what the browser may access.

**Belonging in the living room.** A TV is a family meeting point. Tabs, bookmarks, and history stay within reach so everyone uses the same browser, the same way.

---

## Capabilities

### Remote-first navigation
- D-pad navigation and a virtual cursor with edge scrolling.
- Voice search wired to the system WebView.
- Per-site user agent switching (desktop or mobile views).

### Productivity
- Tabs with thumbnails and a bookmark list.
- Searchable browsing history.
- Download manager with a foreground service and notifications.
- Shortcuts on the home screen.

### Privacy and security
- Private mode isolated in its own process.
- Safe Browsing always on.
- Ad and tracker blocking with a native Kotlin engine.
- Per-site permission control (location, media, downloads).

### Media
- Native player built on ExoPlayer (Media3), with HLS (HTTP Live Streaming, a streaming protocol) and DASH (Dynamic Adaptive Streaming over HTTP) support.
- The remote's media keys control page players, including iframes.
- Screen refresh rate matching per content.

### Curated home and updates
- Home page hosted in the cloud, editable without rebuilding the app.
- Automatic updates from GitHub Releases, with version and signature checks.

---

## Engine

The project uses the **system WebView** (Android's component that renders web pages, based on Blink and V8). This keeps the APK (Android Package Kit, the installable package format for Android) small, ships no embedded engine, and benefits from the security updates the OS itself receives.

We chose WebView-only for broader market penetration and distribution: it is the standard on virtually every Android TV and Google TV.

---

## Getting started

```bash
git clone https://github.com/SimpleSoftwareLTDA/SimpleBrowser.git
cd SimpleBrowser
./sign-release.sh --dry-run   # validate the signing environment
./sign-release.sh              # build a signed release
```

You can also open the project in Android Studio. Use the `genericDebug` variant for development. The signed release is produced by CI (Continuous Integration, the automated pipeline that builds on every change) in [Actions](https://github.com/SimpleSoftwareLTDA/SimpleBrowser/actions).

Published APKs live under [Releases](https://github.com/SimpleSoftwareLTDA/SimpleBrowser/releases).

---

## License and attribution

This project is a fork of [TV Bro](https://github.com/truefedex/tv-bro) (c) 2019 Fedir Tsapana. The original license is preserved, and attribution appears in the app's About screen as required.

See [LICENSE.md](LICENSE.md) and [THIRD-PARTY.md](THIRD-PARTY.md).

---

## Links

- XDA community discussion: https://forum.xda-developers.com/android/apps-games/tv-bro-browser-android-based-tvs-t3545295
- TV Bro official repositories: [GitHub](https://github.com/truefedex/tv-bro)
