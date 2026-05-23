package app.morphe.extension.youtube.patches.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class MissingResourcesPatch {
    /*
     * Runtime fallback companion for the Add missing resources patch.
     * Inspired by kitadai31's transparent-resource crash workaround, with
     * Morphe/RVX-specific guards for Resources.getDrawable/getDrawableForDensity.
     */
    private static final String[] TOOLBAR_FALLBACK_DRAWABLES = {
            "yt_outline_more_vert_black_24",
            "ic_more_vert_black_24",
            "quantum_ic_more_vert_white_24",
            "yt_outline_search_black_24"
    };
    private static final String[] RESOURCE_PACKAGES = {
            "com.google.android.youtube",
            "anddea.youtube",
            null
    };
    private static final Set<String> loggedFallbacks =
            Collections.synchronizedSet(new HashSet<>());

    public static Drawable getTransparentDrawable(Context context) {
        return getFallbackDrawable(context.getResources(), "Context.getDrawable(0)", isToolbarMenuStack());
    }

    public static Drawable getDrawable(Context context, int id) {
        if (id == 0) {
            return getFallbackDrawable(context.getResources(), "Context.getDrawable(0)", isToolbarMenuStack());
        }

        try {
            return context.getDrawable(id);
        } catch (Resources.NotFoundException ex) {
            return getFallbackDrawable(context.getResources(), "Context.getDrawable missing id=" + id, isToolbarMenuStack(), ex);
        }
    }

    public static Drawable getDrawable(Resources resources, int id) {
        if (id == 0) {
            return getFallbackDrawable(resources, "Resources.getDrawable(0)", isToolbarMenuStack());
        }

        try {
            return resources.getDrawable(id);
        } catch (Resources.NotFoundException ex) {
            return getFallbackDrawable(resources, "Resources.getDrawable missing id=" + id, isToolbarMenuStack(), ex);
        }
    }

    public static Drawable getDrawable(Resources resources, int id, Resources.Theme theme) {
        if (id == 0) {
            return getFallbackDrawable(resources, "Resources.getDrawable(0, theme)", isToolbarMenuStack());
        }

        try {
            return resources.getDrawable(id, theme);
        } catch (Resources.NotFoundException ex) {
            return getFallbackDrawable(resources, "Resources.getDrawable missing id=" + id + ", theme", isToolbarMenuStack(), ex);
        }
    }

    public static Drawable getDrawableForDensity(Resources resources, int id, int density) {
        if (id == 0) {
            return getFallbackDrawableForDensity(resources, density, "Resources.getDrawableForDensity(0)", isToolbarMenuStack());
        }

        try {
            return resources.getDrawableForDensity(id, density);
        } catch (Resources.NotFoundException ex) {
            return getFallbackDrawableForDensity(resources, density, "Resources.getDrawableForDensity missing id=" + id, isToolbarMenuStack(), ex);
        }
    }

    public static Drawable getDrawableForDensity(Resources resources, int id, int density, Resources.Theme theme) {
        if (id == 0) {
            return getFallbackDrawableForDensity(resources, density, theme, "Resources.getDrawableForDensity(0, theme)", isToolbarMenuStack());
        }

        try {
            return resources.getDrawableForDensity(id, density, theme);
        } catch (Resources.NotFoundException ex) {
            return getFallbackDrawableForDensity(resources, density, theme, "Resources.getDrawableForDensity missing id=" + id + ", theme", isToolbarMenuStack(), ex);
        }
    }

    private static Drawable getFallbackDrawable(Resources resources, String reason, boolean preferToolbarIcon) {
        return getFallbackDrawable(resources, reason, preferToolbarIcon, null);
    }

    private static Drawable getFallbackDrawable(Resources resources, String reason, boolean preferToolbarIcon, Exception ex) {
        logFallback(reason, ex);

        if (preferToolbarIcon) {
            int fallbackId = findDrawableId(resources, TOOLBAR_FALLBACK_DRAWABLES);
            if (fallbackId != 0) {
                try {
                    return resources.getDrawable(fallbackId);
                } catch (Resources.NotFoundException ignored) {
                    // Fall through to transparent drawable.
                }
            }
        }

        return getTransparentDrawable();
    }

    private static Drawable getFallbackDrawableForDensity(Resources resources, int density, String reason, boolean preferToolbarIcon) {
        return getFallbackDrawableForDensity(resources, density, reason, preferToolbarIcon, null);
    }

    private static Drawable getFallbackDrawableForDensity(Resources resources, int density, String reason, boolean preferToolbarIcon, Exception ex) {
        logFallback(reason, ex);

        if (preferToolbarIcon) {
            int fallbackId = findDrawableId(resources, TOOLBAR_FALLBACK_DRAWABLES);
            if (fallbackId != 0) {
                try {
                    return resources.getDrawableForDensity(fallbackId, density);
                } catch (Resources.NotFoundException ignored) {
                    // Fall through to transparent drawable.
                }
            }
        }

        return getTransparentDrawable();
    }

    private static Drawable getFallbackDrawableForDensity(Resources resources, int density, Resources.Theme theme, String reason, boolean preferToolbarIcon) {
        return getFallbackDrawableForDensity(resources, density, theme, reason, preferToolbarIcon, null);
    }

    private static Drawable getFallbackDrawableForDensity(Resources resources, int density, Resources.Theme theme, String reason, boolean preferToolbarIcon, Exception ex) {
        logFallback(reason, ex);

        if (preferToolbarIcon) {
            int fallbackId = findDrawableId(resources, TOOLBAR_FALLBACK_DRAWABLES);
            if (fallbackId != 0) {
                try {
                    return resources.getDrawableForDensity(fallbackId, density, theme);
                } catch (Resources.NotFoundException ignored) {
                    // Fall through to transparent drawable.
                }
            }
        }

        return getTransparentDrawable();
    }

    private static int findDrawableId(Resources resources, String[] names) {
        for (String name : names) {
            for (String resourcePackage : RESOURCE_PACKAGES) {
                int id = resources.getIdentifier(name, "drawable", resourcePackage);
                if (id != 0) {
                    return id;
                }
            }
        }

        return 0;
    }

    private static boolean isToolbarMenuStack() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String methodName = element.getMethodName();
            if ("onCreateOptionsMenu".equals(methodName) || "onCreatePanelMenu".equals(methodName)) {
                return true;
            }
        }

        return false;
    }

    private static void logFallback(String reason, Exception ex) {
        String caller = getCaller();
        String key = reason + "|" + caller;
        if (!loggedFallbacks.add(key)) {
            return;
        }

        if (ex == null) {
            Logger.printInfo(() -> "Missing drawable fallback: " + reason + ", caller=" + caller);
        } else {
            Logger.printInfo(() -> "Missing drawable fallback: " + reason + ", caller=" + caller, ex);
        }
    }

    private static String getCaller() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.equals(Thread.class.getName())
                    || className.equals(MissingResourcesPatch.class.getName())
                    || className.startsWith("android.content.res.Resources")) {
                continue;
            }

            return className + "." + element.getMethodName() + ":" + element.getLineNumber();
        }

        return "unknown";
    }

    private static Drawable getTransparentDrawable() {
        return new ColorDrawable(Color.TRANSPARENT);
    }
}
