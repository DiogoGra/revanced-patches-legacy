package app.morphe.patches.youtube.layout.livering

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.shorts.components.shortsComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.OPEN_CHANNEL_OF_LIVE_AVATAR
import app.morphe.patches.youtube.utils.playservice.is_21_20_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_25_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.playbackstart.playbackStartDescriptorPatch
import app.morphe.patches.youtube.video.playbackstart.PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.video.playbackstart.playbackStartVideoIdReference
import app.morphe.patches.youtube.video.playbackstart.shortsPlaybackStartIntentLegacyFingerprint
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/OpenChannelOfLiveAvatarPatch;"

@Suppress("unused")
val openChannelOfLiveAvatarPatch = bytecodePatch(
    OPEN_CHANNEL_OF_LIVE_AVATAR.title,
    OPEN_CHANNEL_OF_LIVE_AVATAR.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
        playbackStartDescriptorPatch,
        shortsComponentPatch,
    )

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: OPEN_CHANNEL_OF_LIVE_AVATAR",
            ),
            OPEN_CHANNEL_OF_LIVE_AVATAR,
        )

        // Activity is used as the context to launch an Intent.
        YouTubeActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->" +
                    "setMainActivity(Landroid/app/Activity;)V",
        )

        val playbackStartVideoIdMethod = playbackStartVideoIdReference as MethodReference
        fun patchLogic(mapRegister: String, playerDescriptorClassRegister: String, free1: String, free2: String): String {
            val methodParameter = playerDescriptorClassRegister.startsWith("p")

            return """
                move-object/from16 $free1, $mapRegister
                ${
                    if (methodParameter) "move-object/from16 $free2, $playerDescriptorClassRegister"
                    else ""
                }
                invoke-virtual { ${
                    if (methodParameter) free2
                    else playerDescriptorClassRegister
                } }, ${playbackStartVideoIdMethod.definingClass}->${playbackStartVideoIdMethod.name}()Ljava/lang/String;
                move-result-object $free2
                invoke-static { $free1, $free2 }, $EXTENSION_CLASS->openChannel(Ljava/util/Map;Ljava/lang/String;)Z
                move-result $free1
                if-eqz $free1, :ignore
                return-void
                :ignore
                nop
            """
        }

        clientSettingEndpointFingerprint.let {
            it.method.apply {
                val match = it.instructionMatches[1]
                val moveResultRegister = match.getInstruction<OneRegisterInstruction>().registerA
                val insertIndex = match.index + 1
                val registerProvider = getFreeRegisterProvider(insertIndex, 2, moveResultRegister)
                val free1 = registerProvider.getFreeRegister()
                val free2 = registerProvider.getFreeRegister()

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    patchLogic("p2", "v$moveResultRegister", "v$free1", "v$free2")
                )
            }
        }

        // Same method is modified by shortsComponentPatch (open Shorts in regular player),
        // and by dependsOn that patch runs before this patch which is critical so this patch
        // takes priority.
        val shortsPlaybackIntentMethod = when {
            !is_19_25_or_greater -> shortsPlaybackStartIntentLegacyFingerprint.methodOrThrow()
            is_21_20_or_greater -> ShortsPlaybackIntentFingerprint.method
            else -> ShortsPlaybackIntentFingerprintLegacy.method
        }
        if (!is_19_25_or_greater) {
            shortsPlaybackIntentMethod.apply {
                // Legacy p1 is a proto. Use the descriptor returned by its conversion.
                val conversionIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.returnType == PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
                }
                val descriptorRegister = getInstruction<OneRegisterInstruction>(conversionIndex + 1).registerA
                val insertIndex = conversionIndex + 2
                val registers = getFreeRegisterProvider(insertIndex, 2, descriptorRegister)
                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    patchLogic("p2", "v$descriptorRegister", "v${registers.getFreeRegister()}", "v${registers.getFreeRegister()}")
                )
            }
        } else {
            shortsPlaybackIntentMethod.addInstructionsWithLabels(
                0,
                patchLogic("p2", "p1", "v0", "v1")
            )
        }
    }
}
