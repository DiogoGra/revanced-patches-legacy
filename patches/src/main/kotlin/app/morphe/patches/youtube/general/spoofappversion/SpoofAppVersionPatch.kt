package app.morphe.patches.youtube.general.spoofappversion

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.clientTypeFingerprint
import app.morphe.patches.shared.createPlayerRequestBodyFingerprint
import app.morphe.patches.shared.indexOfClientInfoInstruction
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.morphe.util.Utils.printWarn
import app.morphe.patches.youtube.utils.CAIRO_FRAGMENT_FEATURE_FLAG
import app.morphe.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.indexOfGetDrawableInstruction
import app.morphe.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.morphe.patches.youtube.utils.playservice.is_19_26_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.request.buildRequestPatch
import app.morphe.patches.youtube.utils.request.hookBuildRequest
import app.morphe.patches.youtube.utils.resourceid.settingsFragment
import app.morphe.patches.youtube.utils.resourceid.settingsFragmentCairo
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.cairoFragmentDisabled
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.morphe.patches.youtube.utils.toolBarButtonFingerprint
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.or
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

context(BytecodePatchContext)
private fun settingsLayoutUpdatesHook() {
    try {
        if (!is_19_34_or_greater) {
            try {
                cairoFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
                    CAIRO_FRAGMENT_FEATURE_FLAG,
                    "$GENERAL_CLASS_DESCRIPTOR->disableSettingsLayoutUpdates(Z)Z"
                )
            } catch (ex: Exception) {
                printWarn("Failed to disable legacy Settings layout flag: ${ex.message}")
            }
        }

        settingsFragmentSyntheticFingerprint.methodOrThrow().apply {
            listOf(settingsFragment, settingsFragmentCairo)
                .filter { it > 0 }
                .forEach { literal ->
                    val index = indexOfFirstLiteralInstruction(literal)
                    if (index < 0) {
                        return@forEach
                    }

                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    addInstructions(
                        index + 1, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->useLegacyFragment(I)I
                            move-result v$register
                            """
                    )
                }
        }
    } catch (ex: Exception) {
        printWarn("Failed to add Settings layout updates hook: ${ex.message}")
    }
}

context(BytecodePatchContext)
private fun restorePlayerAppVersionHook() {
    try {
        fun app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.getReference(index: Int) =
            getInstruction<ReferenceInstruction>(index).reference

        fun app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.getFieldReference(index: Int) =
            getReference(index) as FieldReference

        val (clientInfoClass, clientInfoReference, clientVersionReference) =
            clientTypeFingerprint.matchOrThrow().let {
                with(it.method) {
                    val clientInfoIndex = indexOfClientInfoInstruction(this)
                    val dummyClientVersionIndex = it.stringMatches!!.first().index
                    val dummyClientVersionRegister =
                        getInstruction<OneRegisterInstruction>(dummyClientVersionIndex).registerA
                    val clientVersionIndex =
                        indexOfFirstInstructionOrThrow(dummyClientVersionIndex) {
                            opcode == Opcode.IPUT_OBJECT &&
                                    getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                    (this as TwoRegisterInstruction).registerA == dummyClientVersionRegister
                        }

                    Triple(
                        getFieldReference(clientInfoIndex).type,
                        getFieldReference(clientInfoIndex),
                        getFieldReference(clientVersionIndex),
                    )
                }
            }

        createPlayerRequestBodyFingerprint.matchOrThrow().let {
            it.method.apply {
                val helperMethodName = "patch_restorePlayerAppVersion"
                val checkCastIndex = it.instructionMatches.first().index
                val checkCastInstruction = getInstruction<OneRegisterInstruction>(checkCastIndex)
                val requestMessageInstanceRegister = checkCastInstruction.registerA
                val clientInfoContainerClassName =
                    checkCastInstruction.getReference<TypeReference>()!!.type

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static { v$requestMessageInstanceRegister }, " +
                            "$definingClass->$helperMethodName($clientInfoContainerClassName)V",
                )

                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        helperMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                clientInfoContainerClassName,
                                annotations,
                                "clientInfoContainer",
                            ),
                        ),
                        "V",
                        AccessFlags.PRIVATE or AccessFlags.STATIC,
                        annotations,
                        null,
                        MutableMethodImplementation(4),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0, """
                                invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->restorePlayerAppVersion()Z
                                move-result v0
                                if-eqz v0, :disabled
                                iget-object v1, p0, $clientInfoReference
                                if-eqz v1, :disabled
                                invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->getPlayerVersionOverride()Ljava/lang/String;
                                move-result-object v0
                                iput-object v0, v1, $clientVersionReference
                                :disabled
                                return-void
                                """,
                        )
                    },
                )
            }
        }
    } catch (ex: Exception) {
        printWarn("Failed to add legacy player app version restore hook: ${ex.message}")
    }
}

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {

    dependsOn(
        settingsPatch,
        versionCheckPatch,
        buildRequestPatch,
    )

    execute {
        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersion"
        }.returnEarly(true)

        hookBuildRequest("$GENERAL_CLASS_DESCRIPTOR->spoofAppVersionPlayerRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
        settingsLayoutUpdatesHook()

        if (!is_19_26_or_greater) {
            return@execute
        }

        /**
         * When spoofing the app version to YouTube 19.20.xx or earlier via Spoof app version on YouTube 19.23.xx+, the Library tab will crash.
         * As a temporary workaround, do not set an image in the toolbar when the enum name is UNKNOWN.
         */
        toolBarButtonFingerprint.methodOrThrow().apply {
            val getDrawableIndex = indexOfGetDrawableInstruction(this)
            val enumOrdinalIndex = indexOfFirstInstructionReversedOrThrow(getDrawableIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.returnType == "I"
            }
            val insertIndex = enumOrdinalIndex + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA
            val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setImageDrawable"
            } + 1

            addInstructionsWithLabels(
                insertIndex, """
                    if-eqz v$insertRegister, :ignore
                    """, ExternalLabel("ignore", getInstruction(jumpIndex))
            )
        }

        /**
         * RVX does not use CairoFragment, and uses a different method to restore the 'Playback' setting.
         * If the app version is spoofed to 19.30 or earlier, the 'Playback' setting will be broken.
         * Add a setting to fix this.
         */
        if (is_19_34_or_greater && cairoFragmentDisabled) {
            cairoFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
                CAIRO_FRAGMENT_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->disableCairoFragment(Z)Z"
            )
        }
    }

}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(
        YOUTUBE_PACKAGE_NAME(
            "19.16.39",
        ),
    )

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        spoofAppVersionBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
            "SETTINGS: DISABLE_SETTINGS_LAYOUT_UPDATES",
            "SETTINGS: SPOOF_APP_VERSION"
        )

        if (is_19_34_or_greater && cairoFragmentDisabled) {
            settingArray += "SETTINGS: FIX_SPOOF_APP_VERSION_SIDE_EFFECT"
        }

        addPreference(
            settingArray,
            SPOOF_APP_VERSION
        )
    }
}
