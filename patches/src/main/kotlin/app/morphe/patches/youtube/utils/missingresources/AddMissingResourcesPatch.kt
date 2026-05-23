package app.morphe.patches.youtube.utils.missingresources

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.transformation.IMethodCall
import app.morphe.patches.all.misc.transformation.filterMapInstruction35c
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.ADD_MISSING_RESOURCES
import app.morphe.patches.youtube.utils.playservice.is_19_16_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.updatePatchStatus
import app.morphe.util.Utils.printInfo

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/MissingResourcesPatch;"

/*
 * Derived from / inspired by kitadai31's revanced-patches-android6-7
 * "Add missing resources" patch (GPL-3.0), then adapted for Morphe/RVX and
 * narrowed for YouTube 19.16.39 so local resources are preserved when possible.
 */
private val addMissingResourcesBytecodePatch = bytecodePatch(
    description = "addMissingResourcesBytecodePatch",
) {
    dependsOn(
        sharedExtensionPatch,
        transformInstructionsPatch(
            filterMap = { classDef, _, instruction, instructionIndex ->
                filterMapInstruction35c<ResourcesMethodCall>(
                    "Lapp/morphe/extension",
                    classDef,
                    instruction,
                    instructionIndex,
                )
            },
            transform = { mutableMethod, entry ->
                val (methodCall, instruction, instructionIndex) = entry
                methodCall.replaceInvokeVirtualWithExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    mutableMethod,
                    instruction,
                    instructionIndex,
                )
            },
        ),
    )

    execute {
        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "AddMissingResources")

        contextGetDrawableFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0,
                """
                    if-nez p1, :original
                    invoke-static {p0, p1}, $EXTENSION_CLASS_DESCRIPTOR->getDrawable(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;
                    move-result-object p0
                    return-object p0
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }
    }
}

private enum class ResourcesMethodCall(
    override val definedClassName: String,
    override val methodName: String,
    override val methodParams: Array<String>,
    override val returnType: String,
) : IMethodCall {
    GetDrawable(
        "Landroid/content/res/Resources;",
        "getDrawable",
        arrayOf("I"),
        "Landroid/graphics/drawable/Drawable;",
    ),
    GetDrawableWithTheme(
        "Landroid/content/res/Resources;",
        "getDrawable",
        arrayOf("I", "Landroid/content/res/Resources\$Theme;"),
        "Landroid/graphics/drawable/Drawable;",
    ),
    GetDrawableForDensity(
        "Landroid/content/res/Resources;",
        "getDrawableForDensity",
        arrayOf("I", "I"),
        "Landroid/graphics/drawable/Drawable;",
    ),
    GetDrawableForDensityWithTheme(
        "Landroid/content/res/Resources;",
        "getDrawableForDensity",
        arrayOf("I", "I", "Landroid/content/res/Resources\$Theme;"),
        "Landroid/graphics/drawable/Drawable;",
    ),
}

@Suppress("unused")
val addMissingResourcesPatch = resourcePatch(
    ADD_MISSING_RESOURCES.title,
    ADD_MISSING_RESOURCES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        addMissingResourcesBytecodePatch,
        versionCheckPatch,
    )

    execute {
        fun existingDrawableNames(): MutableSet<String> {
            val names = mutableSetOf<String>()

            document("res/values/drawables.xml").use { document ->
                val drawableNodes = document.getElementsByTagName("drawable")
                for (i in 0 until drawableNodes.length) {
                    val node = drawableNodes.item(i)
                    if (node.attributes?.getNamedItem("name") != null) {
                        names.add(node.attributes.getNamedItem("name").nodeValue)
                    }
                }
            }

            get("res").listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("drawable") }
                ?.forEach { directory ->
                    directory.listFiles()
                        ?.filter { it.isFile }
                        ?.forEach { file ->
                            names.add(file.name.substringBeforeLast(".").removeSuffix(".9"))
                        }
                }

            return names
        }

        fun addDrawableAliases(
            aliases: Map<String, String>,
            requireExistingTarget: Boolean = true,
        ): Pair<Int, Int> {
            var addedAliases = 0
            var skippedAliases = 0
            val existingDrawableNames = existingDrawableNames()

            document("res/values/drawables.xml").use { document ->
                val rootNode = document.documentElement

                aliases.forEach { (name, value) ->
                    val targetName = value.removePrefix("@drawable/")
                    if (
                        name in existingDrawableNames ||
                        (requireExistingTarget && targetName !in existingDrawableNames)
                    ) {
                        skippedAliases++
                        return@forEach
                    }

                    val element = document.createElement("drawable")
                    element.setAttribute("name", name)
                    element.textContent = value
                    rootNode.appendChild(element)
                    existingDrawableNames.add(name)
                    addedAliases++
                }
            }

            return addedAliases to skippedAliases
        }

        fun replaceColorValues(colors: Map<String, String>): Int {
            var replacedColors = 0

            document("res/values/colors.xml").use { document ->
                val colorNodes = document.getElementsByTagName("color")
                for (i in 0 until colorNodes.length) {
                    val node = colorNodes.item(i)
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val value = colors[name] ?: continue

                    if (node.textContent != value) {
                        node.textContent = value
                        replacedColors++
                    }
                }
            }

            return replacedColors
        }

        if (is_19_16_or_greater) {
            val replacedColors = replaceColorValues(
                mapOf(
                    "yt_light_red_cairo" to "@color/yt_light_red",
                    "yt_medium_red_cairo" to "@color/yt_medium_red",
                    "yt_medium_red_opacity90_cairo" to "@color/yt_medium_red_opacity90",
                    "yt_youtube_red_cairo" to "@color/yt_youtube_red",
                )
            )
            printInfo(
                "Add missing resources: YouTube 19.16+ detected, " +
                        "skipped drawable alias injection to preserve local navigation resources, " +
                        "replaced $replacedColors Cairo red colors, using fallback hooks."
            )
            return@execute
        }

        val (addedAliases, skippedAliases) = addDrawableAliases(
            mapOf(
                // Shorts player.
                "ic_right_like_off_shadowed" to "@drawable/ic_right_like_off_32c",
                "ic_right_like_on_shadowed" to "@drawable/ic_right_like_on_32c",
                "ic_right_dislike_off_shadowed" to "@drawable/ic_right_dislike_off_32c",
                "ic_right_dislike_on_shadowed" to "@drawable/ic_right_dislike_on_32c",
                "ic_right_comment_shadowed" to "@drawable/ic_right_comment_32c",
                "ic_right_share_shadowed" to "@drawable/ic_right_share_32c",
                "ic_remix_filled_white_shadowed" to "@drawable/ic_remix_filled_white_24",

                // Comments.
                "yt_outline_thumb_up_black_18" to "@drawable/yt_outline_thumb_up_black_24",
                "yt_outline_thumb_down_black_18" to "@drawable/yt_outline_thumb_down_black_24",
                "yt_fill_thumb_up_black_18" to "@drawable/yt_fill_thumb_up_black_24",
                "yt_fill_thumb_down_black_18" to "@drawable/yt_fill_thumb_down_black_24",
                "yt_fill_spark_black_24" to "@drawable/yt_fill_sparkle_white_24",
            )
        )

        val waveformElements = get("res/drawable/ic_waveform_elements.xml", false)
        val copiedWaveform = !waveformElements.exists()
        if (copiedWaveform) {
            copyResources(
                "youtube/addmissingresources",
                ResourceGroup("drawable", "ic_waveform_elements.xml"),
            )
        }

        printInfo(
            "Add missing resources: added $addedAliases drawable aliases, " +
                    "skipped $skippedAliases existing aliases, " +
                    "copied waveform=$copiedWaveform."
        )
    }
}
