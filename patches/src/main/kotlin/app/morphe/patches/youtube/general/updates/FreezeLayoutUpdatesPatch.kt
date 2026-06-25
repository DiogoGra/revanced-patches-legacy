package app.morphe.patches.youtube.general.updates

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.FREEZE_LAYOUT_UPDATES
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/FreezeLayoutUpdatesPatch;"

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                val hotConfigGroupResultIndex = match.stringMatches!!.first().index - 1
                addInstructions(
                    hotConfigGroupResultIndex + 1,
                    """
                        invoke-static {v5}, $EXTENSION_CLASS_DESCRIPTOR->getHotConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v5
                    """
                )

                val hotStoredTimestampResultIndex =
                    indexOfFirstInstructionOrThrow(hotConfigGroupResultIndex, Opcode.MOVE_RESULT_WIDE)

                val hotHashDataResultIndex =
                    indexOfFirstInstructionOrThrow(hotStoredTimestampResultIndex, Opcode.INVOKE_INTERFACE) + 1
                addInstructions(
                    hotHashDataResultIndex + 1,
                    """
                        invoke-static {v2}, $EXTENSION_CLASS_DESCRIPTOR->getHotHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v2
                    """
                )
            }
        }

        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                val coldHashDataResultIndex = match.stringMatches!![2].index + 3
                addInstructions(
                    coldHashDataResultIndex + 1,
                    """
                        invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v0
                    """
                )

                val coldConfigGroupResultIndex = match.stringMatches!![0].index + 3
                addInstructions(
                    coldConfigGroupResultIndex + 1,
                    """
                        invoke-static {v1}, $EXTENSION_CLASS_DESCRIPTOR->getColdConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v1
                    """
                )
            }
        }

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
