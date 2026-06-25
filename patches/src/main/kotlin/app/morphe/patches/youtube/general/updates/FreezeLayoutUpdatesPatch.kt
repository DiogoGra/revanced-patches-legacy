package app.morphe.patches.youtube.general.updates

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.FREEZE_LAYOUT_UPDATES
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: FREEZE_LAYOUT_UPDATES"
            ),
            FREEZE_LAYOUT_UPDATES
        )
    }
}
