package app.morphe.patches.youtube.utils.compatibility

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBILITY_YOUTUBE = Compatibility(
        name = "YouTube",
        packageName = YOUTUBE_PACKAGE_NAME,
        targets = listOf(
            AppTarget(version = "19.16.39", minSdk = 26),
        )
    )

    val COMPATIBLE_PACKAGE = COMPATIBILITY_YOUTUBE

    val COMPATIBILITY_YOUTUBE_RELOAD_VIDEO = COMPATIBILITY_YOUTUBE.excluding(
        "19.43.41",
    )
}
