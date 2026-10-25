package app.morphe.extension.youtube.patches.utils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class ShortsCaptionsPatch {
    public interface NativeController {
        Object rvxCaptionsActivity();
        boolean rvxCaptionsAvailable();
        Object rvxCaptionsRow();
        boolean rvxCaptionsCurrentRow(Object row);
    }

    public interface NativeMenu {
        Object rvxCaptionsActivity();
        void rvxDismissCaptionsMenu();
    }

    public interface NativeRow {
        void rvxClickCaptions();
    }

    private static final ByteArrayFilterGroup captionsBinding =
            new ByteArrayFilterGroup(null, "menu_item_captions");
    private static volatile WeakReference<NativeController> controller = new WeakReference<>(null);
    private static final Map<NativeRow, WeakReference<NativeController>> rowOwners =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void onControllerAvailable(Object owner) {
        if (owner instanceof NativeController nativeController) {
            controller = new WeakReference<>(nativeController);
        }
    }

    public static void onControllerReset(Object owner) {
        synchronized (rowOwners) {
            rowOwners.entrySet().removeIf(entry -> entry.getValue().get() == owner);
        }
    }

    public static Object getMenuItem(Object host, byte[] element) {
        if (!(host instanceof NativeMenu menu) || element == null || element.length > 2 * 1024 * 1024) {
            return null;
        }
        try {
            return restoreMenuItem(RootView.isShortsActive(), captionsBinding.check(element).isFiltered(), menu);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not restore the native Shorts captions menu item", ex);
            return null;
        }
    }

    static NativeRow restoreMenuItem(boolean shortsActive, boolean captionsElement, NativeMenu menu) {
        NativeController owner = controller.get();
        if (!shortsActive || !captionsElement || owner == null || !owner.rvxCaptionsAvailable()) {
            return null;
        }
        Object activity = menu.rvxCaptionsActivity();
        if (activity == null || activity != owner.rvxCaptionsActivity()) {
            return null;
        }
        Object item = owner.rvxCaptionsRow();
        if (!(item instanceof NativeRow row) || !owner.rvxCaptionsCurrentRow(row)) {
            return null;
        }
        rowOwners.put(row, new WeakReference<>(owner));
        return row;
    }

    public static boolean onMenuItemClick(Object host, Object item) {
        if (!(host instanceof NativeMenu menu) || !(item instanceof NativeRow row)) {
            return false;
        }
        try {
            return handleMenuItemClick(RootView.isShortsActive(), menu, row);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not open the native Shorts captions selector", ex);
            return true;
        }
    }

    static boolean handleMenuItemClick(boolean shortsActive, NativeMenu menu, NativeRow row) {
        WeakReference<NativeController> reference = rowOwners.get(row);
        if (reference == null) {
            return false;
        }
        NativeController owner = reference.get();
        boolean current = shortsActive && owner != null && owner.rvxCaptionsAvailable()
                && menu.rvxCaptionsActivity() == owner.rvxCaptionsActivity()
                && owner.rvxCaptionsCurrentRow(row);
        // Match the native captions-specific menu: dismiss the sheet, then invoke its row callback.
        menu.rvxDismissCaptionsMenu();
        if (current) {
            row.rvxClickCaptions();
        }
        return true;
    }
}
