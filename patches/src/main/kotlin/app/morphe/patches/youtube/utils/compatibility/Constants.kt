package app.morphe.patches.youtube.utils.compatibility

import app.morphe.patcher.patch.PackageName
import app.morphe.patcher.patch.VersionName

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        YOUTUBE_PACKAGE_NAME,
        setOf(
            "19.16.39", // This is the last version where the 'Restore old seekbar thumbnails' setting works.
        )
    )
}
