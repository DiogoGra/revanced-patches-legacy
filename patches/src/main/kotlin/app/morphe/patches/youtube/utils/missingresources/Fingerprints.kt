package app.morphe.patches.youtube.utils.missingresources

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val contextGetDrawableFingerprint = legacyFingerprint(
    name = "contextGetDrawableFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "Landroid/graphics/drawable/Drawable;",
    parameters = listOf("Landroid/content/Context;", "I"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "a"
    }
)

internal val legacyCaptionsControllerFingerprint = legacyFingerprint(
    name = "legacyCaptionsControllerFingerprint",
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("menu_item_captions"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lkkt;" && method.name == "g"
    },
)

internal val legacyCaptionsMenuClickFingerprint = legacyFingerprint(
    name = "legacyCaptionsMenuClickFingerprint",
    returnType = "V",
    parameters = listOf("Ltpl;"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lahqa;" && method.name == "aM"
    },
)
