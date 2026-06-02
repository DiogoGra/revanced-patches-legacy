package app.morphe.extension.youtube.patches.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
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
    private static final Set<Integer> CAPTIONS_ICON_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    50,  // CAPTIONS
                    180, // CLOSED_CAPTION
                    290, // SUBTITLES
                    765  // CLOSED_CAPTION_SELECTED
            ))
    );
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

    public static CharSequence getBottomSheetMenuItemTextFallback(Object host, Object iconMetadata, CharSequence text) {
        if (text != null && text.length() > 0) {
            return text;
        }

        int iconType = getIconType(iconMetadata);
        if (!CAPTIONS_ICON_TYPES.contains(iconType)) {
            return null;
        }

        Context context = getContext(host);
        CharSequence fallbackText = getString(context, "overflow_captions", "captions_key");
        if (fallbackText == null || fallbackText.length() == 0) {
            fallbackText = "Subtitles";
        }

        String key = "bottom_sheet_text|" + iconType + "|" + getCaller();
        if (loggedFallbacks.add(key)) {
            CharSequence finalFallbackText = fallbackText;
            Logger.printInfo(() -> "Missing bottom sheet menu text fallback: iconType="
                    + iconType + ", text=" + finalFallbackText + ", caller=" + getCaller());
        }

        return fallbackText;
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

    private static int getIconType(Object iconMetadata) {
        if (iconMetadata == null) {
            return -1;
        }

        try {
            Field bitField = findField(iconMetadata.getClass(), "b");
            Field iconTypeField = findField(iconMetadata.getClass(), "c");
            if (bitField == null || iconTypeField == null) {
                return -1;
            }

            bitField.setAccessible(true);
            iconTypeField.setAccessible(true);

            int flags = bitField.getInt(iconMetadata);
            if ((flags & 1) == 0) {
                return -1;
            }

            return iconTypeField.getInt(iconMetadata);
        } catch (Exception ex) {
            String key = "bottom_sheet_icon_type|" + iconMetadata.getClass().getName();
            if (loggedFallbacks.add(key)) {
                Logger.printInfo(() -> "Failed to read bottom sheet menu icon type: "
                        + iconMetadata.getClass().getName(), ex);
            }
            return -1;
        }
    }

    private static Context getContext(Object host) {
        if (host instanceof Context) {
            return (Context) host;
        }

        if (host == null) {
            return null;
        }

        Method method = findMethod(host.getClass(), "oy");
        if (method == null) {
            return null;
        }

        try {
            method.setAccessible(true);
            Object context = method.invoke(host);
            return context instanceof Context
                    ? (Context) context
                    : null;
        } catch (Exception ex) {
            String key = "bottom_sheet_context|" + host.getClass().getName();
            if (loggedFallbacks.add(key)) {
                Logger.printInfo(() -> "Failed to resolve bottom sheet menu context: "
                        + host.getClass().getName(), ex);
            }
            return null;
        }
    }

    private static CharSequence getString(Context context, String... names) {
        if (context == null) {
            return null;
        }

        Resources resources = context.getResources();
        String packageName = context.getPackageName();

        for (String name : names) {
            int id = resources.getIdentifier(name, "string", packageName);
            if (id == 0) {
                for (String resourcePackage : RESOURCE_PACKAGES) {
                    id = resources.getIdentifier(name, "string", resourcePackage);
                    if (id != 0) {
                        break;
                    }
                }
            }

            if (id == 0) {
                continue;
            }

            try {
                return resources.getString(id);
            } catch (Resources.NotFoundException ignored) {
                // Try the next fallback string.
            }
        }

        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private static Method findMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
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
