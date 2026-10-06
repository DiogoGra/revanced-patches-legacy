<div align="center"> 
<img src="assets/rvx-logo.png" alt="RVX logo" width="128">

    
## 🧩 ReVanced Extended Patches
[![Static Badge](https://img.shields.io/badge/RVX_Wiki-gray?logo=github)](https://github.com/anddea/revanced-patches/wiki)   [![Static Badge](https://img.shields.io/badge/Translations-gray?logo=crowdin)](https://rvxtranslate.netlify.app/)
<br>
[![Static Badge](https://img.shields.io/badge/Telegram-Community-gray?logo=telegram&color=%2326A5E4)](https://t.me/AnddeaChat)   [![Static Badge](https://img.shields.io/badge/Reddit-RVX-gray?logo=reddit&color=red)](https://reddit.com/r/revancedextended)   [![Static Badge](https://img.shields.io/badge/Reddit-YTAdvanced-gray?logo=reddit&color=yellow)](https://www.reddit.com/r/YTadvanced)
</div>

## Documentation

Check the [wiki](https://github.com/anddea/revanced-patches/wiki) for resources on patching, customization, and debugging.

Report issues [here](https://github.com/DiogoGra/revanced-patches-legacy/issues).

[Credits](https://github.com/anddea/revanced-patches/wiki/Credits)

## 📋 List of patches in this repository

### [📦 `com.google.android.youtube`](https://play.google.com/store/apps/details?id=com.google.android.youtube)
<details>

| 💊 Patch | 📜 Description | 🏹 Target Version |
|:--------:|:--------------:|:-----------------:|
| `Add missing resources` | Adds fallback resources for old YouTube clients and replaces zero drawable ids with a transparent drawable to prevent crashes. | 19.16.39 |
| `Alternative thumbnails` | Adds options to replace video thumbnails using the DeArrow API or image captures from the video. | 19.16.39 |
| `Ambient mode control` | Adds options to disable Ambient mode and to bypass Ambient mode restrictions. | 19.16.39 |
| `App refresh rate` | Adds an option to change the app refresh rate. | 19.16.39 |
| `Bypass URL redirects` | Adds an option to bypass URL redirects and open the original URL directly. | 19.16.39 |
| `Bypass image region restrictions` | Adds an option to use a different host for static images, so that images blocked in some countries can be received. | 19.16.39 |
| `Change form factor` | Adds an option to change the UI appearance to a phone, tablet, or automotive device. | 19.16.39 |
| `Change player flyout menu toggles` | Adds an option to use text toggles instead of switch toggles within the additional settings menu. | 19.16.39 |
| `Change share sheet` | Adds an option to change the in-app share sheet to the system share sheet. | 19.16.39 |
| `Change start page` | Adds an option to set which page the app opens in instead of the homepage. | 19.16.39 |
| `Custom DPI` | Forces a higher display density for this app only, so the whole UI scales up without changing system density. | 19.16.39 |
| `Custom Shorts action buttons` | Changes, at compile time, the icon of the action buttons of the Shorts player. | 19.16.39 |
| `Custom branding for YouTube` | Adds in-app app-name, launcher-icon, header, splash, and settings-icon selection. | 19.16.39 |
| `Custom double tap length` | Adds Double-tap to seek values that are specified in patch options. | 19.16.39 |
| `Description components` | Adds options to hide and disable description components. | 19.16.39 |
| `Disable QUIC protocol` | Adds an option to disable CronetEngine's QUIC protocol. | 19.16.39 |
| `Disable forced auto captions` | Adds an option to disable captions from being automatically enabled. | 19.16.39 |
| `Disable haptic feedback` | Adds options to disable haptic feedback when swiping in the video player. | 19.16.39 |
| `Disable layout updates` | Adds an option to disable layout updates by server. | 19.16.39 |
| `Disable playlist autoplay` | Adds an option to stop a playlist from automatically advancing to the next video. | 19.16.39 |
| `Disable resuming Miniplayer on startup` | Adds an option to disable the Miniplayer 'Continue watching' from resuming on app startup. | 19.16.39 |
| `Disable resuming Shorts on startup` | Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched. | 19.16.39 |
| `Disable scrolling speed limit` | Adds an option to remove limits of how fast the home and subscription feed can be scrolled. | 19.16.39 |
| `Disable sign in to TV popup` | Adds an option to disable the popup asking to sign into a TV on the same local network. | 19.16.39 |
| `Enable debug logging` | Adds an option for debugging and exporting RVX logs to the clipboard. | 19.16.39 |
| `Enable gradient loading screen` | Adds an option to enable the gradient loading screen. | 19.16.39 |
| `Force original audio` | Adds an option to disable audio tracks from being automatically enabled. | 19.16.39 |
| `Freeze layout updates` | Adds an option to freeze YouTube layout update configuration. | 19.16.39 |
| `Fullscreen components` | Adds options to hide or change components related to fullscreen. | 19.16.39 |
| `Fullscreen video scale` | Adds options to stretch or zoom videos to fill the screen in fullscreen mode. | 19.16.39 |
| `Gemini` | Adds options to use Gemini for video summaries, transcription, and settings search. | 19.16.39 |
| `GmsCore support` | Allows the app to work without root by using a different package name when patched using a GmsCore instead of Google Play Services. | 19.16.39 |
| `Hide Shorts dimming` | Removes, at compile time, the dimming effect at the top and bottom of Shorts videos. | 19.16.39 |
| `Hide accessibility controls dialog` | Removes, at compile time, accessibility controls dialog 'Turn on accessibility controls for the video player?'. | 19.16.39 |
| `Hide action buttons` | Adds options to hide action buttons under videos. | 19.16.39 |
| `Hide ads` | Adds options to hide ads. | 19.16.39 |
| `Hide comments components` | Adds options to hide components related to comments. | 19.16.39 |
| `Hide feed components` | Adds options to hide components related to feeds. | 19.16.39 |
| `Hide feed flyout menu` | Adds the ability to hide feed flyout menu components using a custom filter. | 19.16.39 |
| `Hide layout components` | Adds options to hide general layout components. | 19.16.39 |
| `Hide player buttons` | Adds options to hide buttons in the video player, and to hide or change the opacity of the player control buttons background. | 19.16.39 |
| `Hide player flyout menu` | Adds options to hide player flyout menu components. | 19.16.39 |
| `Hide shortcuts` | Remove, at compile time, the app shortcuts that appears when the app icon is long pressed. | 19.16.39 |
| `Hook download actions` | Adds support to download videos with an external downloader app using the in-app download button. | 19.16.39 |
| `Miniplayer` | Adds options to change the in-app minimized player, and if patching target 19.16+ adds options to use modern miniplayers. | 19.16.39 |
| `Navigation bar components` | Adds options to hide or change components related to the navigation bar. | 19.16.39 |
| `Open channel of live avatar` | Adds an option to prevent a channel's current live video from opening when tapping its avatar. | 19.16.39 |
| `Open links externally` | Adds an option to always open links in your browser instead of the in-app browser. | 19.16.39 |
| `Overlay buttons` | Adds options to display useful overlay buttons in the video player. | 19.16.39 |
| `Override YouTube Music buttons` | Overrides YouTube Music buttons to open RVX Music or any compatible third-party client. | 19.16.39 |
| `Playback in feeds` | Adds the 'Playback in feeds' setting of YouTube to the Morphe settings, where it is always available even if YouTube hides it. | 19.16.39 |
| `Player components` | Adds options to hide or change components related to the video player. | 19.16.39 |
| `PoToken provider` | Adds option to get PoToken using the built-in PoToken provider. | 19.16.39 |
| `Reload video` | Adds an option to display a button in the video player to reload the current video. | 19.16.39 |
| `Remember livestream playback position` | Adds an option to remember the playback position of ongoing livestreams and resume from there when reopening a livestream. | 19.16.39 |
| `Remove background playback restrictions` | Removes restrictions on background playback, including for music and kids videos. | 19.16.39 |
| `Remove viewer discretion dialog` | Adds an option to remove the dialog that appears when opening a video that has been age-restricted by accepting it automatically. This does not bypass the age restriction. | 19.16.39 |
| `Return YouTube Dislike` | Adds an option to show the dislike count of videos using the Return YouTube Dislike API. | 19.16.39 |
| `Return YouTube Username` | Adds an option to replace YouTube handles with usernames in comments using YouTube Data API v3. | 19.16.39 |
| `Sanitize sharing links` | Adds an option to sanitize sharing links by removing tracking query parameters. | 19.16.39 |
| `Save to Watch later` | Adds options to save videos to Watch later from the video player or feed flyout menu. | 19.16.39 |
| `Seekbar components` | Adds options to hide or change components related to the seekbar. | 19.16.39 |
| `Set transcript cookies` | Adds an option to set Cookies in YouTube Transcript API requests. | 19.16.39 |
| `Settings for YouTube` | Applies mandatory patches to implement ReVanced Extended settings into the application. | 19.16.39 |
| `Shorts components` | Adds options to hide or change components related to YouTube Shorts. | 19.16.39 |
| `Snack bar components` | Adds options to hide or change components related to the snack bar. | 19.16.39 |
| `SponsorBlock` | Adds options to enable and configure SponsorBlock, which can skip undesired video segments, such as sponsored content. | 19.16.39 |
| `Spoof app version` | Adds options to spoof the YouTube client version. This can be used to restore old UI elements and features. | 19.16.39 |
| `Spoof video streams` | Adds options to spoof the client video streams to fix playback. | 19.16.39 |
| `Spoof watch history` | Adds an option to change the domain of the watch history or check its status. | 19.16.39 |
| `Swipe controls` | Adds options for controlling volume and brightness with swiping, and whether to enter fullscreen when swiping down below the player. | 19.16.39 |
| `Theme` | Adds options to change the app's themes and splash screen style. | 19.16.39 |
| `Toolbar components` | Adds options to hide or change components located on the toolbar, such as the search bar, header, and toolbar buttons. | 19.16.39 |
| `Translations for YouTube` | Add translations or remove string resources. | 19.16.39 |
| `Video playback` | Adds options to customize settings related to video playback, such as default video quality and playback speed. | 19.16.39 |
| `Visual preferences icons for YouTube` | Adds icons to specific preferences in the settings. | 19.16.39 |
| `Voice Over Translation` | Adds an option to enable Yandex voice-over translation of video audio tracks. | 19.16.39 |
| `Wide search bar` | Adds a wide search bar to the top of the home and subscription feed. | 19.16.39 |
</details>



## 📝 JSON Format

Example:

```json
[
  {
    "name": "Alternative thumbnails",
    "description": "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
    "use":true,
    "compatiblePackages": {
      "com.google.android.youtube": [
        "19.16.39"
      ]
    },
    "options": []
  }
]
```

## Credits

Thanks to [anddea](https://github.com/anddea/revanced-patches),
[Morphe](https://github.com/MorpheApp/morphe-patches), and
[kitadai31](https://github.com/kitadai31/revanced-patches-android6-7)
for making this project possible.
