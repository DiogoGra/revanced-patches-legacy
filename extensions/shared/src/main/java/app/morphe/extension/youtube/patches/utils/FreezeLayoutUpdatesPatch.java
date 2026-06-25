package app.morphe.extension.youtube.patches.utils;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class FreezeLayoutUpdatesPatch {
    /*
     * Ported from kitadai31/revanced-patches-android6-7 Freeze Layout Updates,
     * adapted for Morphe package names and 19.16.39 legacy compatibility.
     */
    private static final boolean ENABLED = Settings.FREEZE_LAYOUT_UPDATES.get();
    private static final boolean DISABLE_LAYOUT_UPDATES = Settings.DISABLE_LAYOUT_UPDATES.get();

    public static String getHotConfigGroup(String original) {
        if (!ENABLED) {
            return original;
        }

        String savedValue = Settings.FROZEN_HOT_CONFIG_GROUP.get();
        if (DISABLE_LAYOUT_UPDATES || savedValue.isEmpty()) {
            return null;
        }
        return savedValue;
    }

    public static String getHotHashData(String original) {
        if (!ENABLED) {
            return original;
        }

        if (DISABLE_LAYOUT_UPDATES) {
            return "";
        }
        return Settings.FROZEN_HOT_HASH_DATA.get();
    }

    public static String getColdConfigGroup(String original) {
        if (!ENABLED) {
            return original;
        }

        String savedValue = Settings.FROZEN_COLD_CONFIG_GROUP.get();
        if (DISABLE_LAYOUT_UPDATES || savedValue.isEmpty()) {
            return null;
        }
        return savedValue;
    }

    public static String getColdHashData(String original) {
        if (!ENABLED) {
            return original;
        }

        if (DISABLE_LAYOUT_UPDATES) {
            return "";
        }
        return Settings.FROZEN_COLD_HASH_DATA.get();
    }
}
