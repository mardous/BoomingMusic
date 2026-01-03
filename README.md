<div align="center">

<img src="metadata/en-US/images/icon.png" width="160" height="160" alt="Booming Music icon">

# 🎵 Booming Music

### Modern design. Pure sound. Fully yours.

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge&logo=android)](https://android.com/)
[![API](https://img.shields.io/badge/API-26%2B-green.svg?&style=for-the-badge&logo=android)](https://android-arsenal.com/api?level=26)
[![Latest Release](https://img.shields.io/github/v/release/mardous/BoomingMusic?style=for-the-badge&label=Release&logo=github)](https://github.com/mardous/BoomingMusic/releases/latest)
[![Pre-Release](https://img.shields.io/github/v/release/mardous/BoomingMusic?style=for-the-badge&include_prereleases&logo=github&label=Pre-release)](https://github.com/mardous/BoomingMusic/releases)
[![F-Droid version](https://img.shields.io/f-droid/v/com.mardous.booming?style=for-the-badge&label=F-Droid&logo=fdroid)](https://f-droid.org/packages/com.mardous.booming/)
[![License: GPL v3](https://img.shields.io/github/license/mardous/BoomingMusic?style=for-the-badge&color=orange&label=License&logo=gnu)](LICENSE.txt)
[![Contributor Covenant](https://img.shields.io/badge/Contributor_Covenant-2.1-4baaaa.svg?style=for-the-badge&logo=contributorcovenant)](CODE_OF_CONDUCT.md)
[![Telegram Group](https://img.shields.io/badge/Telegram-Join_Chat-blue?style=for-the-badge&logo=telegram)](https://t.me/mardousdev)

<a href="https://github.com/mardous/BoomingMusic/releases"><img src="assets/badge-github.png" height="60" alt="GitHub Release"></a>
<a href="https://f-droid.org/packages/com.mardous.booming/"><img src="assets/badge-fdroid.png" height="60" alt="F-Droid"></a>
<a href="https://apt.izzysoft.de/packages/com.mardous.booming/"><img src="assets/badge-izzyondroid.png" height="60" alt="IzzyOnDroid"></a>
<a href="https://www.openapk.net/boomingmusic/com.mardous.booming/"><img src="assets/badge-openapk.png" height="60" alt="OpenAPK"></a>
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mardous/BoomingMusic/"><img src="assets/badge-obtainium.png" height="60" alt="Obtainium"></a>

</div>

## 🗂️ Table of Contents

- [✨ Key Features](#-key-features)
- [📸 Screenshots](#-screenshots)
- [💻 Tech Stack](#-tech-stack)
- [🧩 Roadmap](#-roadmap)
- [🔗 Useful Links](#-useful-links)
- [🤝 Contributing](#-contributing)
- [🙌 Credits](#-credits)
- [⚖️ License](#-license)

## ✨ Key Features

- 🎼 **Automatic Lyrics Download & Editing** – Automatically fetch, sync, and edit lyrics with ease.
- 💬 **Word-by-Word Synced Lyrics** – Enjoy immersive real-time lyric playback with word-level timing.
- 🌍 **Translated Lyrics Support** – Display dual-language lyrics via TTML or LRC with translations.
- 🔊 **Built-in Equalizer** – Fine-tune your sound, customize your presets, and share them.
- 🔄 **Gapless Playback** – Smooth transitions between songs with zero interruption.
- 🧠 **Smart Playlists** – Auto-generated lists like *Recently Played*, *Most Played*, and *History*.
- 🎧 **Bluetooth & Headset Controls** – Manage playback easily via connected devices.
- 🚗 **Android Auto Integration** – Full hands-free experience on the road.
- 🎨 **Material You Design** – Dynamic theming for a modern and personal interface.
- 📂 **Folder Browsing** – Play songs directly from any folder.
- ⏰ **Sleep Timer** – Automatically stop playback after a set time.
- 🧩 **Widgets** – Lock screen and home screen controls for quick access.
- 🔖 **Tag Editor** – Edit song metadata such as title, artist, and album info.
- 🔉 **ReplayGain Support** – Maintain consistent volume across all tracks.
- 🖼️ **Automatic Artist Images** – Download artist artwork for a polished library look.
- 🚫 **Library Filtering** – Easily exclude or include folders with blacklist/whitelist options.

## 📸 Screenshots

<div align="center">
<table>
<tr>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/1.jpg" alt="For You" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/2.jpg" alt="Songs" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/3.jpg" alt="Albums" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/4.jpg" alt="Album View" width="180"/></td>
</tr>
<tr>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/5.jpg" alt="Search" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/6.jpg" alt="Normal" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/7.jpg" alt="Full" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/8.jpg" alt="Gradient" width="180"/></td>
</tr>
<tr>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/9.jpg" alt="Plain" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/10.jpg" alt="M3" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/11.jpg" alt="Expressive" width="180"/></td>
<td align="center" width="25%"><img src="metadata/en-US/images/phoneScreenshots/12.jpg" alt="Peek" width="180"/></td>
</tr>
</table>
</div>

### 💻 Tech Stack

| Layer                   | Technology                                                     |
|:------------------------|:---------------------------------------------------------------|
| 🎧 Audio Engine         | [Media3 ExoPlayer](https://developer.android.com/media/media3) |
| 🧱 Architecture         | MVVM + Repository Pattern                                      |
| 💾 Persistence          | Room Database                                                  |
| ⚙️ Dependency Injection | [Koin](https://insert-koin.io/)                                |
| 🧵 Async                | Kotlin Coroutines & Flow                                       |
| 🧩 UI                   | Android Views + Jetpack Compose (hybrid)                       |
| 🖼️ Image Loading        | [Coil](https://coil-kt.github.io/coil/)                        |
| 🎨 Design               | Material 3 / Material You                                      |
| 🗣️ Language            | Kotlin                                                         |

## 🧩 Roadmap

- [ ] 📦 Independent library scanner (no MediaStore dependency)
- [ ] 🎨 Multi-artist support (split & index properly)
- [ ] 🎵 Improved genre handling
- [ ] 🔁 Last.fm integration (import/export playback data)
- [ ] 💿 Enhanced artist pages (separate albums and singles visually)
- [ ] 📺 Experimental Android TV support (if enough demand!)

## 🔗 Useful Links

- 🔐 **[Requested Permissions](https://github.com/mardous/BoomingMusic/wiki/Advanced-Info#-permissions)**  
  What the app needs and why

- 🚘 **[Android Auto Setup](https://github.com/mardous/BoomingMusic/wiki/Advanced-Info#-android-auto-setup)**  
  How to enable and troubleshoot

- 🎧 **[Supported Formats](https://github.com/mardous/BoomingMusic/wiki/Advanced-Info#-supported-formats)**  
  Compatible audio formats

- 💬 **[Community](https://github.com/mardous/BoomingMusic/wiki/Community)**  
  Users and contributors

- 🌐 **[Translations](https://crowdin.com/project/booming-music)**  
  Help us translate Booming Music into your language

- ❓ **[FAQ](https://github.com/mardous/BoomingMusic/wiki/FAQ)**  
  Common questions

## 🤝 Contributing

Booming Music is open-source — contributions are **always welcome!**
Check the [Contributing Guide](CONTRIBUTING.md) for details.

If you enjoy the app or want to support its development, give the repo a ⭐ — it really helps!
You can also:
- Open issues
- Submit pull requests
- Suggest new ideas

**Translations:** Managed on [Crowdin](https://crowdin.com/project/booming-music).

## 💖 Support Development

Booming Music is an open-source project developed and maintained with passion in my spare time.
If you enjoy the app and the free features it offers, please consider supporting me to help cover
development costs and dedicate more time to new features.

Your support is greatly appreciated and keeps me motivated to continue improving Booming Music!

<a href="https://ko-fi.com/christiaam" target="_blank">
<img src="https://storage.ko-fi.com/cdn/brandasset/v2/support_me_on_kofi_red.png" alt="Support me on Ko-fi" style="border: 0px; height: 40px;" />
</a>

## 🙌 Credits

Inspired by [Retro Music Player](https://github.com/RetroMusicPlayer/RetroMusicPlayer).
Also thanks to:

- [AMLV](https://github.com/dokar3/amlv)
- [LRCLib](https://lrclib.net/)
- [spotify-lyrics-api](https://github.com/akashrchandran/spotify-lyrics-api) by [Akash R Chandran](https://github.com/akashrchandran)
- [Alex](https://github.com/Paxsenix0) for access to various of his [APIs](https://api.paxsenix.org/)

## ⚖️ License

```
GNU General Public License - Version 3

Copyright (C) 2025 Christians Martínez Alvarado

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

---

<p align="center"><a href="#readme">⬆️ Back to top</a></p>
