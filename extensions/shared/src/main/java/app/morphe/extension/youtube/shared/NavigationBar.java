package app.morphe.extension.youtube.shared;

import static app.morphe.extension.youtube.shared.NavigationBar.NavigationButton.CREATE;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class NavigationBar {
    private static final String NAVIGATION_ICON_DIAGNOSTIC_PREFIX = "RVX_NAV_DIAG";
    private static final long[] LEGACY_NAVIGATION_ICON_RESTORE_DELAYS_MS = {
            250,
            1000,
            2500,
            5000,
            10000
    };

    private static final String[] HOME_OUTLINE_DRAWABLES = {
            "yt_outline_home_black_24",
            "ic_tab_home",
            "quantum_ic_home_grey600_24"
    };
    private static final String[] HOME_SELECTED_DRAWABLES = {
            "yt_fill_home_black_24",
            "ic_tab_home",
            "quantum_ic_home_grey600_24"
    };
    private static final String[] SHORTS_OUTLINE_DRAWABLES = {
            "yt_outline_youtube_shorts_black_24",
            "ic_youtube_shorts_24",
            "ic_shortcut_shorts"
    };
    private static final String[] SHORTS_SELECTED_DRAWABLES = {
            "yt_fill_youtube_shorts_black_24",
            "yt_fill_youtube_shorts_no_triangle_black_18",
            "ic_youtube_shorts_24"
    };
    private static final String[] SUBSCRIPTIONS_OUTLINE_DRAWABLES = {
            "yt_outline_subscriptions_black_24",
            "ic_tab_subscriptions",
            "ic_shortcut_subscriptions"
    };
    private static final String[] SUBSCRIPTIONS_SELECTED_DRAWABLES = {
            "yt_fill_subscriptions_black_24",
            "yt_fill_subscriptions_grey600_24",
            "ic_tab_subscriptions"
    };
    private static final String[] NOTIFICATIONS_OUTLINE_DRAWABLES = {
            "yt_outline_bell_black_24",
            "yt_outline_bell_off_black_24",
            "quantum_ic_notifications_grey600_24",
            "quantum_gm_ic_notifications_grey600_24",
            "ic_drawer_notifications_inbox_normal"
    };
    private static final String[] NOTIFICATIONS_SELECTED_DRAWABLES = {
            "yt_fill_bell_black_24",
            "yt_fill_bell_on_black_24",
            "quantum_ic_notifications_active_grey600_24",
            "quantum_gm_ic_notifications_active_grey600_24",
            "ic_drawer_notifications_inbox_normal"
    };
    private static final Set<String> loggedLegacyIconRestores =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<View> hookedBottomBarContainers =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<View> pendingBottomBarIconRestores =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<ImageView, Integer> restoredNavigationIconIds =
            new WeakHashMap<>();
    private static final Map<ImageView, Drawable> restoredNavigationIconDrawables =
            new WeakHashMap<>();


    /**
     * How long to wait for the set nav button latch to be released.  Maximum wait time must
     * be as small as possible while still allowing enough time for the nav bar to update.
     * <p>
     * YT calls it's back button handlers out of order, and litho starts filtering before the
     * navigation bar is updated. Fixing this situation and not needlessly waiting requires
     * somehow detecting if a back button key/gesture will not change the active tab.
     * <p>
     * On average the time between pressing the back button and the first litho event is
     * about 10-20ms.  Waiting up to 75-150ms should be enough time to handle normal use cases
     * and not be noticeable, since YT typically takes 100-200ms (or more) to update the view.
     * <p>
     * This delay is only noticeable when the device back button/gesture will not
     * change the current navigation tab, such as backing out of the watch history.
     * <p>
     * This issue can also be avoided on a patch by patch basis, by avoiding calls to
     * {@link NavigationButton#getSelectedNavigationButton()} unless absolutely necessary.
     */
    private static final long LATCH_AWAIT_TIMEOUT_MILLISECONDS = 120;

    /**
     * Used as a workaround to fix the issue of YT calling back button handlers out of order.
     * Used to hold calls to {@link NavigationButton#getSelectedNavigationButton()}
     * until the current navigation button can be determined.
     * <p>
     * Only used when the hardware back button is pressed.
     */
    @Nullable
    private static volatile CountDownLatch navButtonLatch;

    /**
     * Map of nav button layout views to Enum type.
     * No synchronization is needed, and this is always accessed from the main thread.
     */
    private static final Map<View, NavigationButton> viewToButtonMap = new WeakHashMap<>();

    static {
        // On app startup litho can start before the navigation bar is initialized.
        // Force it to wait until the nav bar is updated.
        createNavButtonLatch();
    }

    private static void createNavButtonLatch() {
        navButtonLatch = new CountDownLatch(1);
    }

    private static void releaseNavButtonLatch() {
        CountDownLatch latch = navButtonLatch;
        if (latch != null) {
            navButtonLatch = null;
            latch.countDown();
        }
    }

    private static void waitForNavButtonLatchIfNeeded() {
        CountDownLatch latch = navButtonLatch;
        if (latch == null) {
            return;
        }

        if (Utils.isCurrentlyOnMainThread()) {
            // The latch is released from the main thread, and waiting from the main thread will always timeout.
            // This situation has only been observed when navigating out of a submenu and not changing tabs.
            // and for that use case the nav bar does not change so it's safe to return here.
            Logger.printDebug(() -> "Cannot block main thread waiting for nav button. " +
                    "Using last known navbar button status.");
            return;
        }

        try {
            Logger.printDebug(() -> "Latch wait started");
            if (latch.await(LATCH_AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)) {
                // Back button changed the navigation tab.
                Logger.printDebug(() -> "Latch wait complete");
                return;
            }

            // Timeout occurred, and a normal event when pressing the physical back button
            // does not change navigation tabs.
            releaseNavButtonLatch(); // Prevent other threads from waiting for no reason.
            Logger.printDebug(() -> "Latch wait timed out");

        } catch (InterruptedException ex) {
            // Calling YouTube thread was interrupted.
            Logger.printException(() -> "Latch wait interrupted", ex);
            Thread.currentThread().interrupt(); // Restore interrupt status flag.
        }
    }

    /**
     * Last YT navigation enum loaded.  Not necessarily the active navigation tab.
     * Always accessed from the main thread.
     */
    @Nullable
    private static String lastYTNavigationEnumName;

    public static String getLastAppNavigationEnum() {
        return lastYTNavigationEnumName;
    }

    /**
     * Injection point.
     */
    public static void setLastAppNavigationEnum(@Nullable Enum<?> ytNavigationEnumName) {
        if (ytNavigationEnumName != null) {
            lastYTNavigationEnumName = ytNavigationEnumName.name();
        }
    }

    /**
     * Injection point.
     */
    public static void navigationTabLoaded(final View navigationButtonGroup) {
        try {
            String lastEnumName = lastYTNavigationEnumName;

            for (NavigationButton buttonType : NavigationButton.values()) {
                if (buttonType.ytEnumNames.contains(lastEnumName)) {
                    Logger.printDebug(() -> "navigationTabLoaded: " + lastEnumName);
                    viewToButtonMap.put(navigationButtonGroup, buttonType);
                    logNavigationTabDiagnostic("loaded", buttonType, navigationButtonGroup);
                    navigationTabCreatedCallback(buttonType, navigationButtonGroup);
                    scheduleLegacyNavigationIconRestore(buttonType, navigationButtonGroup);
                    return;
                }
            }

            // Log the unknown tab as exception level, only if debug is enabled.
            // This is because unknown tabs do no harm, and it's only relevant to developers.
            if (Settings.DEBUG.get()) {
                Logger.printException(() -> "Unknown tab: " + lastEnumName
                        + " view: " + navigationButtonGroup.getClass());
            }
        } catch (Exception ex) {
            Logger.printException(() -> "navigationTabLoaded failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * Unique hook just for the 'Create' and 'You' tab.
     */
    public static void navigationImageResourceTabLoaded(View view) {
        // 'You' tab has no YT enum name and the enum hook is not called for it.
        // Compare the last enum to figure out which tab this actually is.
        if (CREATE.ytEnumNames.contains(lastYTNavigationEnumName)) {
            navigationTabLoaded(view);
        } else {
            lastYTNavigationEnumName = NavigationButton.LIBRARY.ytEnumNames.get(0);
            navigationTabLoaded(view);
        }
    }

    /**
     * Injection point.
     */
    public static void navigationTabSelected(View navButtonImageView, boolean isSelected) {
        try {
            if (!isSelected) {
                return;
            }

            NavigationButton button = viewToButtonMap.get(navButtonImageView);

            if (button == null) { // An unknown tab was selected.
                // Show a toast only if debug mode is enabled.
                if (Settings.DEBUG.get()) {
                    Logger.printException(() -> "Unknown navigation view selected: " + navButtonImageView);
                }

                NavigationButton.selectedNavigationButton = null;
                return;
            }

            NavigationButton.selectedNavigationButton = button;
            Logger.printDebug(() -> "Changed to navigation button: " + button);
            logNavigationTabDiagnostic("selected", button, navButtonImageView);
            scheduleLegacyNavigationIconRestore(button, navButtonImageView);

            // Release any threads waiting for the selected nav button.
            releaseNavButtonLatch();
        } catch (Exception ex) {
            Logger.printException(() -> "navigationTabSelected failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void onBackPressed(Activity activity) {
        Logger.printDebug(() -> "Back button pressed");
        createNavButtonLatch();
    }

    /**
     * @noinspection EmptyMethod
     */
    private static void navigationTabCreatedCallback(NavigationButton button, View tabView) {
        // Code is added during patching.
    }

    public static void fixServerSideNavigationIcons(View bottomBarContainer) {
        try {
            addServerSideNavigationIconListener(bottomBarContainer);
            scheduleServerSideNavigationIconRestore(bottomBarContainer);
        } catch (Exception ex) {
            Logger.printException(() -> "fixServerSideNavigationIcons failure", ex);
        }
    }

    private static void addServerSideNavigationIconListener(View bottomBarContainer) {
        if (!hookedBottomBarContainers.add(bottomBarContainer)) {
            return;
        }

        bottomBarContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> requestServerSideNavigationIconRestore(bottomBarContainer, "bottomBarContainer+globalLayout")
        );
        bottomBarContainer.getViewTreeObserver().addOnPreDrawListener(() -> {
            restoreServerSideNavigationIcons(bottomBarContainer, "bottomBarContainer+preDraw");
            return true;
        });
        bottomBarContainer.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                requestServerSideNavigationIconRestore(view, "bottomBarContainer+layoutChange")
        );
    }

    private static void requestServerSideNavigationIconRestore(View bottomBarContainer, String reason) {
        if (!pendingBottomBarIconRestores.add(bottomBarContainer)) {
            return;
        }

        bottomBarContainer.postDelayed(() -> {
            pendingBottomBarIconRestores.remove(bottomBarContainer);
            restoreServerSideNavigationIcons(bottomBarContainer, reason);
        }, 50);
    }

    private static void scheduleServerSideNavigationIconRestore(View bottomBarContainer) {
        restoreServerSideNavigationIcons(bottomBarContainer, "bottomBarContainer");
        bottomBarContainer.post(() -> restoreServerSideNavigationIcons(bottomBarContainer, "bottomBarContainer+post"));

        for (long delay : LEGACY_NAVIGATION_ICON_RESTORE_DELAYS_MS) {
            bottomBarContainer.postDelayed(
                    () -> restoreServerSideNavigationIcons(bottomBarContainer, "bottomBarContainer+" + delay + "ms"),
                    delay
            );
        }
    }

    private static void restoreServerSideNavigationIcons(View view, String reason) {
        int restored = restoreServerSideNavigationIconsByPosition(view)
                + restoreServerSideNavigationIconsRecursive(view);
        if (restored > 0 && Settings.DEBUG.get()) {
            Logger.printInfo(() -> NAVIGATION_ICON_DIAGNOSTIC_PREFIX
                    + " reason=" + reason
                    + " restoredServerSideIcons=" + restored
                    + " root=" + describeView(view));
        }
    }

    /**
     * Server-delivered layouts can replace the tab views after the enum hooks ran. In that
     * case the replacement views are not present in {@link #viewToButtonMap}. Use the stable
     * bottom-bar order as a language-independent fallback instead of matching localized labels.
     */
    private static int restoreServerSideNavigationIconsByPosition(View bottomBarContainer) {
        if (!(bottomBarContainer instanceof ViewGroup)) {
            return 0;
        }

        List<ViewGroup> tabGroups = new ArrayList<>();
        collectNavigationTabGroups(bottomBarContainer, bottomBarContainer, tabGroups);
        int tabCount = tabGroups.size();
        if (tabCount < 4 || tabCount > 5) {
            return 0;
        }

        tabGroups.sort((left, right) -> Integer.compare(getViewScreenX(left), getViewScreenX(right)));
        if (bottomBarContainer.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            Collections.reverse(tabGroups);
        }

        NavigationButton[] buttons;
        if (tabCount == 4) {
            buttons = new NavigationButton[] {
                    NavigationButton.HOME,
                    NavigationButton.SHORTS,
                    NavigationButton.SUBSCRIPTIONS,
                    NavigationButton.LIBRARY
            };
        } else if (Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get()) {
            buttons = new NavigationButton[] {
                    NavigationButton.HOME,
                    NavigationButton.SHORTS,
                    NavigationButton.SUBSCRIPTIONS,
                    NavigationButton.NOTIFICATIONS,
                    NavigationButton.LIBRARY
            };
        } else {
            buttons = new NavigationButton[] {
                    NavigationButton.HOME,
                    NavigationButton.SHORTS,
                    NavigationButton.CREATE,
                    NavigationButton.SUBSCRIPTIONS,
                    NavigationButton.LIBRARY
            };
        }

        int restored = 0;
        for (int i = 0; i < buttons.length; i++) {
            NavigationButton button = buttons[i];
            if (button == NavigationButton.CREATE || button == NavigationButton.LIBRARY) {
                continue;
            }
            restored += restoreServerSideNavigationIcon(tabGroups.get(i), button);
        }
        return restored;
    }

    private static void collectNavigationTabGroups(
            View root,
            View view,
            List<ViewGroup> tabGroups
    ) {
        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;
        if (view != root
                && countNavigationIconViews(group, 2) == 1
                && containsNonEmptyTextView(group)) {
            tabGroups.add(group);
            return;
        }

        for (int i = 0; i < group.getChildCount(); i++) {
            collectNavigationTabGroups(root, group.getChildAt(i), tabGroups);
        }
    }

    private static boolean containsNonEmptyTextView(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            return text != null && text.length() > 0;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (containsNonEmptyTextView(group.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static int getViewScreenX(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[0];
    }

    private static int restoreServerSideNavigationIcon(ViewGroup group, NavigationButton button) {
        ImageView imageView = findNavigationIconView(group);
        int drawableId = getLegacyNavigationDrawableId(group, button);
        if (imageView == null || drawableId == 0) {
            return 0;
        }

        Drawable currentDrawable = imageView.getDrawable();
        Integer restoredDrawableId = restoredNavigationIconIds.get(imageView);
        Drawable restoredDrawable = restoredNavigationIconDrawables.get(imageView);
        boolean alreadyRestored = restoredDrawableId != null
                && restoredDrawableId == drawableId
                && restoredDrawable == currentDrawable
                && currentDrawable != null
                && !(currentDrawable instanceof ColorDrawable)
                && imageView.getImageAlpha() == 255
                && imageView.getColorFilter() != null
                && imageView.getVisibility() == View.VISIBLE;

        if (alreadyRestored) {
            return 0;
        }

        imageView.setImageResource(drawableId);
        imageView.setImageAlpha(255);
        imageView.setAlpha(1.0f);
        imageView.setColorFilter(getLegacyNavigationIconColor(group), PorterDuff.Mode.SRC_IN);
        imageView.setVisibility(View.VISIBLE);
        restoredNavigationIconIds.put(imageView, drawableId);
        restoredNavigationIconDrawables.put(imageView, imageView.getDrawable());
        return 1;
    }

    private static int restoreServerSideNavigationIconsRecursive(View view) {
        if (!(view instanceof ViewGroup)) {
            return 0;
        }

        ViewGroup group = (ViewGroup) view;
        int restored = 0;

        NavigationButton button = getNavigationButtonFromViewGroup(group);
        if (button != null) {
            restored += restoreServerSideNavigationIcon(group, button);
        }

        for (int i = 0; i < group.getChildCount(); i++) {
            restored += restoreServerSideNavigationIconsRecursive(group.getChildAt(i));
        }

        return restored;
    }

    @Nullable
    private static NavigationButton getNavigationButtonFromViewGroup(ViewGroup group) {
        NavigationButton mappedButton = viewToButtonMap.get(group);
        if (mappedButton != null) {
            return mappedButton;
        }

        // A bottom bar contains multiple tab icons. Classifying that parent from the first
        // recognized descendant label can apply one tab's icon to a different tab, especially
        // when most labels are localized but "Shorts" is not.
        if (countNavigationIconViews(group, 2) != 1) {
            return null;
        }

        CharSequence contentDescription = group.getContentDescription();
        NavigationButton button = getNavigationButtonFromText(contentDescription);
        if (button != null) {
            return button;
        }

        TextView textView = findNavigationLabelView(group);
        return textView == null
                ? null
                : getNavigationButtonFromText(textView.getText());
    }

    private static int countNavigationIconViews(View view, int limit) {
        if (view instanceof ImageView) {
            return 1;
        }
        if (!(view instanceof ViewGroup)) {
            return 0;
        }

        ViewGroup group = (ViewGroup) view;
        int count = 0;
        for (int i = 0; i < group.getChildCount() && count < limit; i++) {
            count += countNavigationIconViews(group.getChildAt(i), limit - count);
        }
        return count;
    }

    @Nullable
    private static TextView findNavigationLabelView(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (getNavigationButtonFromText(text) != null) {
                return (TextView) view;
            }
        }

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView textView = findNavigationLabelView(group.getChildAt(i));
            if (textView != null) {
                return textView;
            }
        }

        return null;
    }

    @Nullable
    private static NavigationButton getNavigationButtonFromText(@Nullable CharSequence text) {
        if (text == null) {
            return null;
        }

        String normalized = text.toString().toLowerCase();
        if (normalized.contains("principal") || normalized.contains("home")) {
            return NavigationButton.HOME;
        }
        if (normalized.contains("shorts")) {
            return NavigationButton.SHORTS;
        }
        if (normalized.contains("suscripciones") || normalized.contains("subscriptions")) {
            return NavigationButton.SUBSCRIPTIONS;
        }
        if (normalized.contains("notificaciones") || normalized.contains("notifications")) {
            return NavigationButton.NOTIFICATIONS;
        }

        return null;
    }

    private static void scheduleLegacyNavigationIconRestore(NavigationButton button, View tabView) {
        restoreLegacyNavigationIcon(button, tabView);
        tabView.post(() -> restoreLegacyNavigationIcon(button, tabView));

        for (long delay : LEGACY_NAVIGATION_ICON_RESTORE_DELAYS_MS) {
            tabView.postDelayed(() -> restoreLegacyNavigationIcon(button, tabView), delay);
        }
    }

    private static void restoreLegacyNavigationIcon(NavigationButton button, View tabView) {
        try {
            ImageView imageView = findNavigationIconView(tabView);
            if (imageView == null) {
                return;
            }

            int drawableId = getLegacyNavigationDrawableId(tabView, button);
            if (drawableId == 0) {
                return;
            }

            imageView.setImageResource(drawableId);
            imageView.setImageAlpha(255);
            imageView.setAlpha(1.0f);
            imageView.setColorFilter(getLegacyNavigationIconColor(tabView), PorterDuff.Mode.SRC_IN);
            imageView.setVisibility(View.VISIBLE);

            String logKey = button + "|" + drawableId;
            if (loggedLegacyIconRestores.add(logKey)) {
                Logger.printInfo(() -> "Restored legacy navigation icon: " + button
                        + ", drawable=" + getResourceName(tabView, drawableId));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to restore legacy navigation icon: " + button, ex);
        }
    }

    @Nullable
    private static ImageView findNavigationIconView(View view) {
        if (view instanceof ImageView) {
            return (ImageView) view;
        }

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ImageView imageView = findNavigationIconView(group.getChildAt(i));
            if (imageView != null) {
                return imageView;
            }
        }

        return null;
    }

    private static int getLegacyNavigationDrawableId(View tabView, NavigationButton button) {
        boolean selected = tabView.isSelected();

        switch (button) {
            case HOME:
                return findDrawableId(tabView, selected ? HOME_SELECTED_DRAWABLES : HOME_OUTLINE_DRAWABLES);
            case SHORTS:
                return findDrawableId(tabView, selected ? SHORTS_SELECTED_DRAWABLES : SHORTS_OUTLINE_DRAWABLES);
            case SUBSCRIPTIONS:
                return findDrawableId(tabView, selected
                        ? SUBSCRIPTIONS_SELECTED_DRAWABLES
                        : SUBSCRIPTIONS_OUTLINE_DRAWABLES);
            case NOTIFICATIONS:
                return findDrawableId(tabView, selected
                        ? NOTIFICATIONS_SELECTED_DRAWABLES
                        : NOTIFICATIONS_OUTLINE_DRAWABLES);
            default:
                return 0;
        }
    }

    private static int findDrawableId(View view, String[] drawableNames) {
        Resources resources = view.getResources();
        String packageName = view.getContext().getPackageName();

        for (String drawableName : drawableNames) {
            int id = resources.getIdentifier(drawableName, "drawable", packageName);
            if (id != 0) {
                return id;
            }
        }

        return 0;
    }

    private static int getLegacyNavigationIconColor(View view) {
        int uiMode = view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES
                ? Color.WHITE
                : Color.BLACK;
    }

    private static void logNavigationTabDiagnostic(String reason, NavigationButton button, View view) {
        if (!Settings.DEBUG.get()) {
            return;
        }

        logNavigationTabDiagnosticNow(reason, button, view);
        view.postDelayed(() -> logNavigationTabDiagnosticNow(reason + "+500ms", button, view), 500);
        view.postDelayed(() -> logNavigationTabDiagnosticNow(reason + "+2000ms", button, view), 2000);
        view.postDelayed(() -> logNavigationTabDiagnosticNow(reason + "+5000ms", button, view), 5000);
    }

    private static void logNavigationTabDiagnosticNow(String reason, NavigationButton button, View view) {
        try {
            Logger.printInfo(() -> NAVIGATION_ICON_DIAGNOSTIC_PREFIX
                    + " reason=" + reason
                    + " button=" + button
                    + " lastEnum=" + lastYTNavigationEnumName
                    + " root=" + describeView(view)
                    + "\n" + describeViewTree(view, 0));
        } catch (Exception ex) {
            Logger.printException(() -> NAVIGATION_ICON_DIAGNOSTIC_PREFIX + " failure", ex);
        }
    }

    private static String describeViewTree(View view, int depth) {
        if (view == null || depth > 5) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        appendIndent(builder, depth);
        builder.append(describeView(view));

        if (view instanceof ImageView) {
            builder.append(" image={").append(describeImageView((ImageView) view)).append("}");
        } else if (view instanceof TextView) {
            builder.append(" text=\"").append(((TextView) view).getText()).append("\"");
        }
        builder.append('\n');

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                builder.append(describeViewTree(group.getChildAt(i), depth + 1));
            }
        }

        return builder.toString();
    }

    private static void appendIndent(StringBuilder builder, int depth) {
        for (int i = 0; i < depth; i++) {
            builder.append("  ");
        }
    }

    private static String describeView(View view) {
        if (view == null) {
            return "null";
        }

        return view.getClass().getName()
                + " id=" + getResourceName(view, view.getId())
                + " visibility=" + visibilityToString(view.getVisibility())
                + " alpha=" + view.getAlpha()
                + " selected=" + view.isSelected()
                + " enabled=" + view.isEnabled()
                + " size=" + view.getWidth() + "x" + view.getHeight()
                + " contentDescription=\"" + view.getContentDescription() + "\"";
    }

    private static String describeImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        ColorStateList tintList = imageView.getImageTintList();
        PorterDuff.Mode tintMode = imageView.getImageTintMode();

        return "drawable=" + describeDrawable(drawable)
                + ", imageAlpha=" + imageView.getImageAlpha()
                + ", tintList=" + tintList
                + ", tintMode=" + tintMode
                + ", colorFilter=" + imageView.getColorFilter()
                + ", scaleType=" + imageView.getScaleType();
    }

    private static String describeDrawable(@Nullable Drawable drawable) {
        if (drawable == null) {
            return "null";
        }

        String description = drawable.getClass().getName()
                + " alpha=" + drawable.getAlpha()
                + " bounds=" + drawable.getBounds();

        if (drawable instanceof ColorDrawable) {
            description += " color=#" + Integer.toHexString(((ColorDrawable) drawable).getColor());
        }

        return description;
    }

    private static String getResourceName(View view, int id) {
        if (id == View.NO_ID) {
            return "NO_ID";
        }

        try {
            return view.getResources().getResourceEntryName(id) + "(" + id + ")";
        } catch (Resources.NotFoundException ex) {
            return "unknown(" + id + ")";
        }
    }

    private static String visibilityToString(int visibility) {
        switch (visibility) {
            case View.VISIBLE:
                return "VISIBLE";
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return String.valueOf(visibility);
        }
    }

    public enum NavigationButton {
        HOME("PIVOT_HOME", "TAB_HOME_CAIRO"),
        SHORTS("TAB_SHORTS", "TAB_SHORTS_CAIRO"),
        /**
         * Create new video tab.
         * This tab will never be in a selected state, even if the create video UI is on screen.
         */
        CREATE("CREATION_TAB_LARGE", "CREATION_TAB_LARGE_CAIRO"),
        /**
         * Only shown to automotive layout.
         */
        EXPLORE("TAB_EXPLORE"),
        SUBSCRIPTIONS("PIVOT_SUBSCRIPTIONS", "TAB_SUBSCRIPTIONS_CAIRO"),
        /**
         * Notifications tab.  Only present when
         * {@link Settings#SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON} is active.
         */
        NOTIFICATIONS("TAB_ACTIVITY", "TAB_ACTIVITY_CAIRO"),
        /**
         * Library tab, including if the user is in incognito mode or when logged out.
         */
        LIBRARY(
                // Modern library tab with 'You' layout.
                // The hooked YT code does not use an enum, and a dummy name is used here.
                "YOU_LIBRARY_DUMMY_PLACEHOLDER_NAME",
                // User is logged out.
                "ACCOUNT_CIRCLE",
                "ACCOUNT_CIRCLE_CAIRO",
                // User is logged in with incognito mode enabled.
                "INCOGNITO_CIRCLE",
                "INCOGNITO_CAIRO",
                // Old library tab (pre 'You' layout), only present when version spoofing.
                "VIDEO_LIBRARY_WHITE",
                // 'You' library tab that is sometimes momentarily loaded.
                // This might be a temporary tab while the user profile photo is loading,
                // but its exact purpose is not entirely clear.
                "PIVOT_LIBRARY"
        );

        @Nullable
        private static volatile NavigationButton selectedNavigationButton;

        /**
         * This will return null only if the currently selected tab is unknown.
         * This scenario will only happen if the UI has different tabs due to an A/B user test
         * or YT abruptly changes the navigation layout for some other reason.
         * <p>
         * All code calling this method should handle a null return value.
         * <p>
         * <b>Due to issues with how YT processes physical back button/gesture events,
         * this patch uses workarounds that can cause this method to take up to 120ms
         * if the device back button was recently pressed.</b>
         *
         * @return The active navigation tab.
         *         If the user is in the upload video UI, this returns tab that is still visually
         *         selected on screen (whatever tab the user was on before tapping the upload button).
         */
        @Nullable
        public static NavigationButton getSelectedNavigationButton() {
            waitForNavButtonLatchIfNeeded();
            return selectedNavigationButton;
        }

        /**
         * YouTube enum name for this tab.
         */
        public final List<String> ytEnumNames;

        NavigationButton(String... ytEnumNames) {
            this.ytEnumNames = Arrays.asList(ytEnumNames);
        }
    }
}
