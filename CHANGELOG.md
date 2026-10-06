# [1.3.0](https://github.com/DiogoGra/revanced-patches-legacy/releases/tag/1.3.0) (2026-10-06)

Changes accumulated from 1.2.1-dev.2 through 1.3.0. YouTube 19.16.39 remains the supported target.

### Features

- Align with RVX [v4.3.1-dev.1](https://github.com/anddea/revanced-patches/releases/tag/v4.3.1-dev.1). (https://github.com/DiogoGra/revanced-patches-legacy/commit/75bb5c92bb4c6d1e457fa8824e56e48de5255f26)
- Align with RVX [v4.2.0](https://github.com/anddea/revanced-patches/releases/tag/v4.2.0), including Custom DPI, Playback in feeds, Force original audio, queue override, Explore filters, Gemini subtitle chunking, additional ad filters, and Settings language fixes. Newer-version-only settings remain gated; the Settings menu filter patch is excluded. (https://github.com/DiogoGra/revanced-patches-legacy/commit/77850ad1931052194cfa7084265773c173155d2e)
- **YouTube - Settings**: Add Freeze layout updates setting, including Spanish translation. (https://github.com/DiogoGra/revanced-patches-legacy/commit/3bcd701117f208f669cf0265cc0d7c7dc802d10e)
- **YouTube - Shorts components**: Add Yandex VOT to custom flyout actions. (https://github.com/DiogoGra/revanced-patches-legacy/commit/b1f42c1a92a5ff8281dfa0a5255e844492870a65)
- **YouTube - Spoof video streams**: Add TV Simply and update default clients; remove obsolete Android Reel clients. (https://github.com/DiogoGra/revanced-patches-legacy/commit/c138aea6473dbc639030455241474b1df532dd80)
- **YouTube**: Update community translations across supported languages. (https://github.com/DiogoGra/revanced-patches-legacy/commit/df2ed293f4abb52f727f32119826f62ddc98ff4b)

### Bug Fixes

- **YouTube**: Fix crash when opening the app. (https://github.com/DiogoGra/revanced-patches-legacy/commit/ad5b3666a3be35e1d11033a041f860707a6b90c7)
- **YouTube - Add missing resources**: Restore the Settings gear icon, including global server-icon mapping. (https://github.com/DiogoGra/revanced-patches-legacy/commit/d30b216b0c7b6626ca1dbc7d7979528520ff5773) (https://github.com/DiogoGra/revanced-patches-legacy/commit/e23ceaea05c2137b4f866bee44d853df6a227333)
- **YouTube - Navigation bar components**: Restore server-delivered navigation icons across languages. (https://github.com/DiogoGra/revanced-patches-legacy/commit/b444b32180db855ac96f349141c251590aacf894) (https://github.com/DiogoGra/revanced-patches-legacy/commit/167d1abe58d0c8dbdfc514f4bff97521c3a5cbc2)
- **YouTube - Shorts components**: Fix custom actions opening the description and related flyout crashes. (https://github.com/DiogoGra/revanced-patches-legacy/commit/ade4a3cc4fdfabda8707bf47d0a71c12625d85db) (https://github.com/DiogoGra/revanced-patches-legacy/commit/8d25efc93b78b3e68aebc39d33af8267ade1de26)
- **YouTube - Shorts components**: Fix crashes when opening server-side flyout menu actions. (https://github.com/DiogoGra/revanced-patches-legacy/commit/baee079d659ac43c54b1a4b3805740fab037636b)
- **Morphe Manager**: Show this fork's version details instead of Anddea's. (https://github.com/DiogoGra/revanced-patches-legacy/commit/405140be2890f7b3bad25a2eca508b5038c7b8e0)
- **Morphe Manager**: Correct bundle changelog metadata and remove the JSON BOM. (https://github.com/DiogoGra/revanced-patches-legacy/commit/b35f0861acccedad25b1fd1afd75bf8519777983) (https://github.com/DiogoGra/revanced-patches-legacy/commit/f9e96a91d47bb79a991d31aa3e5c8e4d88b61d2f)
- **Morphe Manager**: Exclude the bundled Kotlin runtime and refresh Android bundle metadata to prevent sources showing zero patches. (https://github.com/DiogoGra/revanced-patches-legacy/commit/31dc72effd4f25fd055a5a5dd9e0299a8024e9f6) (https://github.com/DiogoGra/revanced-patches-legacy/commit/588126907456b51184b12a5e36bca641925d91cb)

- **YouTube - GmsCore support**: Preserve shared callback dispatch when bypassing device compliance checks. (https://github.com/DiogoGra/revanced-patches-legacy/commit/2a6318153a54202b0d774c02262f0bc6ab952eeb)
- **YouTube - Spoof app version**: Keep the 20.02.34 preset valid across restarts. (https://github.com/DiogoGra/revanced-patches-legacy/commit/88771873bc370de34f1e153145978ff9d9789194)
- **YouTube - Open channel of live avatar**: Hook the converted playback descriptor on 19.16.39 to prevent Shorts verification errors. (https://github.com/DiogoGra/revanced-patches-legacy/commit/2a6318153a54202b0d774c02262f0bc6ab952eeb)
- **YouTube - Hook download actions**: Preserve the custom Shorts menu's View register and handle missing icon metadata safely. (https://github.com/DiogoGra/revanced-patches-legacy/commit/2a6318153a54202b0d774c02262f0bc6ab952eeb)
- **YouTube - Navigation bar components**: Fix dark navigation icons on the dark Shorts bar in light theme. (https://github.com/DiogoGra/revanced-patches-legacy/commit/2a6318153a54202b0d774c02262f0bc6ab952eeb)
- **YouTube - Add missing resources**: Restore legacy Search and Notifications icons for server-delivered toolbar actions. (https://github.com/DiogoGra/revanced-patches-legacy/commit/88771873bc370de34f1e153145978ff9d9789194)
- **YouTube - Settings**: Remove the duplicate Restore old YouTube settings screen setting and use Disable Settings layout updates consistently. (https://github.com/DiogoGra/revanced-patches-legacy/commit/88771873bc370de34f1e153145978ff9d9789194)
- **YouTube - Seekbar components**: Restore native legacy seekbar thumbnails and remove the newer-version thumbnail renderer. (https://github.com/DiogoGra/revanced-patches-legacy/commit/88771873bc370de34f1e153145978ff9d9789194)
- **YouTube - Spoof app version**: Mark 20.05.46 as Recommended and update the corresponding translations. (https://github.com/DiogoGra/revanced-patches-legacy/commit/88771873bc370de34f1e153145978ff9d9789194)

> [!NOTE]
> My Telegram account, @personimm, was hacked. After I recovered it, I realized I had been banned from the [Telegram group](https://t.me/AnddeaChat). I hope someone from the group sees this and can add me back. Thank you.

# [1.2.1-dev.5](https://github.com/DiogoGra/revanced-patches-legacy/releases/tag/1.2.1-dev.5) (2026-06-26)

### Features

- **YouTube - Shorts components**: Add Yandex VOT to custom flyout actions and fix custom actions flyout menu. (https://github.com/DiogoGra/revanced-patches-legacy/commit/b1f42c1a9)

# [1.2.1-dev.3](https://github.com/DiogoGra/revanced-patches-legacy/releases/tag/1.2.1-dev.3) (2026-06-25)

### Bug Fixes

- **YouTube**: Crash when opening the app. (https://github.com/DiogoGra/revanced-patches-legacy/commit/ad5b3666a)

- **Morphe Manager**: The versions of Anddea were shown. (https://github.com/DiogoGra/revanced-patches-legacy/commit/405140be2)
