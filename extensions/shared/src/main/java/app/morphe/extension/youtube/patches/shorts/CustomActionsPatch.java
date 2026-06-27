package app.morphe.extension.youtube.patches.shorts;

import static app.morphe.extension.shared.utils.ResourceUtils.getString;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.patches.components.ShortsCustomActionsFilter.isShortsFlyoutMenuVisible;
import static app.morphe.extension.youtube.shared.RootView.isShortsActive;
import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.youtube.utils.GeminiManager;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.components.ShortsCustomActionsFilter;
import app.morphe.extension.youtube.patches.utils.PatchStatus;
import app.morphe.extension.youtube.patches.voiceovertranslation.VoiceOverTranslationPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ExtendedUtils;
import app.morphe.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class CustomActionsPatch {
    private static final boolean IS_SPOOFING_TO_YOUTUBE_2023 =
            isSpoofingToLessThan("19.00.00");
    private static final boolean SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED =
            !IS_SPOOFING_TO_YOUTUBE_2023 && Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU.get();
    private static final boolean SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED =
            Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_TOOLBAR.get();

    private static final int arrSize = CustomAction.values().length;
    private static final Map<CustomAction, Object> flyoutMenuMap = new LinkedHashMap<>(arrSize);
    private static WeakReference<Context> contextRef = new WeakReference<>(null);
    private static WeakReference<RecyclerView> recyclerViewRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void setToolbarMenu(String enumString, View toolbarView) {
        if (!SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED) {
            return;
        }
        if (!isShortsActive()) {
            return;
        }
        if (!isMoreButton(enumString)) {
            return;
        }
        if (!(toolbarView instanceof ViewGroup parentView)) {
            return;
        }

        setToolbarMenuOnLongClickListener(parentView);
    }

    private static void setToolbarMenuOnLongClickListener(ViewGroup parentView) {
        try {
            ImageView imageView = Utils.getChildView(parentView, v -> v instanceof ImageView);
            if (imageView == null) {
                return;
            }
            Context context = imageView.getContext();
            if (context == null) {
                return;
            }
            contextRef = new WeakReference<>(context);

            // Overriding is possible only after OnClickListener is assigned to the more button.
            Utils.runOnMainThreadDelayed(() -> imageView.setOnLongClickListener(button -> {
                showMoreButtonDialog(context);
                return true;
            }), 0);
        } catch (Exception ex) {
            Logger.printException(() -> "setToolbarMenuOnLongClickListener failed", ex);
        }
    }

    private static void showMoreButtonDialog(Context mContext) {
        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(mContext, false);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(arrSize);

        for (CustomAction customAction : CustomAction.values()) {
            if (customAction.settings.get()) {
                String title = customAction.getLabel();
                int iconId = customAction.getDrawableId();
                Runnable action = customAction.getOnClickAction();
                LinearLayout itemLayout = ExtendedUtils.createItemLayout(mContext, title, iconId);
                actionsMap.putIfAbsent(itemLayout, action);
                mainLayout.addView(itemLayout);
            }
        }

        ExtendedUtils.showBottomSheetDialog(mContext, mainLayout, actionsMap);
    }

    private static boolean isMoreButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "MORE_VERT",
                "MORE_VERT_BOLD"
        );
    }

    /**
     * Injection point.
     */
    public static void setFlyoutMenuObject(Object bottomSheetMenuObject) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        if (!isShortsActive()) {
            return;
        }
        if (bottomSheetMenuObject == null) {
            return;
        }
        for (CustomAction customAction : CustomAction.values()) {
            flyoutMenuMap.putIfAbsent(customAction, bottomSheetMenuObject);
        }
    }

    /**
     * Injection point.
     */
    public static void addFlyoutMenu(Object bottomSheetMenuClass, Object bottomSheetMenuList) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        if (!isShortsActive()) {
            return;
        }
        for (CustomAction customAction : CustomAction.values()) {
            if (customAction.settings.get()) {
                addFlyoutMenu(bottomSheetMenuClass, bottomSheetMenuList, customAction);
            }
        }
    }

    /**
     * Rest of the implementation added by patch.
     */
    private static void addFlyoutMenu(Object bottomSheetMenuClass, Object bottomSheetMenuList, CustomAction customAction) {
        Object bottomSheetMenuObject = flyoutMenuMap.get(customAction);
        // These instructions are ignored by patch.
        Logger.printInfo(() -> customAction.name() + bottomSheetMenuClass + bottomSheetMenuList + bottomSheetMenuObject);
    }

    /**
     * Injection point.
     */
    public static boolean onBottomSheetMenuItemClick(View view) {
        try {
            if (view instanceof ViewGroup viewGroup) {
                for (CustomAction customAction : CustomAction.values()) {
                    TextView labelView = Utils.getChildView(
                            viewGroup,
                            true,
                            child -> child instanceof TextView textView &&
                                    customAction.getLabel().contentEquals(textView.getText())
                    );
                    if (labelView != null) {
                        View.OnLongClickListener onLongClick = customAction.getOnLongClickListener();
                        if (onLongClick != null) {
                            view.setOnLongClickListener(onLongClick);
                        }
                        customAction.getOnClickActionWithFlyoutMenuDismiss().run();
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onBottomSheetMenuItemClick failed");
        }

        return false;
    }

    /**
     * Injection point.
     */
    public static void onFlyoutMenuCreate(final RecyclerView recyclerView) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        recyclerView.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (!isShortsActive()) {
                    return;
                }
                contextRef = new WeakReference<>(recyclerView.getContext());
                if (!isShortsFlyoutMenuVisible) {
                    return;
                }
                recyclerViewRef = new WeakReference<>(recyclerView);
                int enabledCustomActionsCount = getEnabledCustomActionsCount();
                int childCount = recyclerView.getChildCount();
                if (childCount < enabledCustomActionsCount) {
                    return;
                }

                int boundCustomActions = 0;
                for (int i = 0; i < childCount; i++) {
                    if (recyclerView.getChildAt(i) instanceof ViewGroup menuItem &&
                            bindCustomAction(menuItem)) {
                        boundCustomActions++;
                    }
                }

                // YouTube 19.16.39 nests the menu label deeper than newer versions.
                // Keep observing until every enabled custom action has its inherited
                // source-item click listener replaced.
                if (boundCustomActions >= enabledCustomActionsCount) {
                    isShortsFlyoutMenuVisible = false;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onFlyoutMenuCreate failure", ex);
            }
        });
    }

    private static boolean bindCustomAction(ViewGroup menuItem) {
        for (CustomAction customAction : CustomAction.values()) {
            if (!customAction.settings.get()) {
                continue;
            }

            TextView labelView = Utils.getChildView(
                    menuItem,
                    true,
                    view -> view instanceof TextView textView &&
                            customAction.getLabel().contentEquals(textView.getText())
            );
            if (labelView == null) {
                continue;
            }

            menuItem.setOnClickListener(customAction.getOnClickListener());
            View.OnLongClickListener onLongClick = customAction.getOnLongClickListener();
            if (onLongClick != null) {
                menuItem.setOnLongClickListener(onLongClick);
            }
            return true;
        }

        return false;
    }

    private static int getEnabledCustomActionsCount() {
        int count = 0;
        for (CustomAction customAction : CustomAction.values()) {
            if (customAction.settings.get()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Injection point.
     */
    public static void onLiveHeaderElementsContainerCreate(final View view) {
        if (!SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED) {
            return;
        }
        view.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (view instanceof ViewGroup viewGroup) {
                    setToolbarMenuOnLongClickListener(viewGroup);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onFlyoutMenuCreate failure", ex);
            }
        });
    }

    private static void hideFlyoutMenu() {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        RecyclerView recyclerView = recyclerViewRef.get();
        if (recyclerView == null) {
            return;
        }

        final int touchOutsideId = ResourceUtils.getIdentifier(
                "touch_outside",
                ResourceUtils.ResourceType.ID,
                recyclerView.getContext()
        );
        if (touchOutsideId != 0) {
            View touchOutsideView = recyclerView.getRootView().findViewById(touchOutsideId);
            if (touchOutsideView != null) {
                Utils.clickView(touchOutsideView);
                return;
            }
        }

        if (!(Utils.getParentView(recyclerView, 3) instanceof ViewGroup parentView3rd)) {
            return;
        }

        if (!(parentView3rd.getParent() instanceof ViewGroup parentView4th)) {
            return;
        }

        // Dismiss View [R.id.touch_outside] is the 1st ChildView of the 4th ParentView.
        // This only shows in phone layout.
        Utils.clickView(parentView4th.getChildAt(0));

        // In tablet layout there is no Dismiss View, instead we just hide all two parent views.
        parentView3rd.setVisibility(View.GONE);
        parentView4th.setVisibility(View.GONE);
    }

    public enum CustomAction {
        COPY_URL(
                Settings.SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL,
                "yt_outline_link_black_24",
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                false
                        ),
                        false
                ),
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                true
                        ),
                        true
                )
        ),
        COPY_URL_WITH_TIMESTAMP(
                Settings.SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL_TIMESTAMP,
                "yt_outline_arrow_time_black_24",
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                true
                        ),
                        true
                ),
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                false
                        ),
                        false
                )
        ),
        EXTERNAL_DOWNLOADER(
                Settings.SHORTS_CUSTOM_ACTIONS_EXTERNAL_DOWNLOADER,
                "yt_outline_download_black_24",
                () -> VideoUtils.launchVideoExternalDownloader(
                        ShortsCustomActionsFilter.getShortsVideoId()
                )
        ),
        OPEN_VIDEO(
                Settings.SHORTS_CUSTOM_ACTIONS_OPEN_VIDEO,
                "yt_outline_youtube_logo_icon_black_24",
                () -> VideoUtils.openVideo(
                        ShortsCustomActionsFilter.getShortsVideoId(),
                        true
                )
        ),
        SPEED_DIALOG(
                Settings.SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG,
                "yt_outline_play_arrow_half_circle_black_24",
                () -> VideoUtils.showPlaybackSpeedDialog(contextRef.get(), Settings.SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG_TYPE)
        ),
        GEMINI(
                Settings.SHORTS_CUSTOM_ACTIONS_GEMINI,
                "revanced_gemini_button",
                () -> {
                    Context context = contextRef.get();

                    String shortsVideoId = ShortsCustomActionsFilter.getShortsVideoId();
                    String videoUrl;
                    if (!TextUtils.isEmpty(shortsVideoId)) {
                        videoUrl = VideoUtils.getVideoUrl(shortsVideoId, false);
                    } else {
                        // Fallback to general video URL if shorts ID not found (might be less reliable in shorts)
                        videoUrl = VideoUtils.getVideoUrl(false);
                        Logger.printInfo(() -> "GEMINI CustomAction: Could not get Shorts specific Video ID, using general VideoUtils.");
                    }

                    if (TextUtils.isEmpty(videoUrl) || videoUrl.equals(VideoUtils.VIDEO_URL)) {
                        Utils.showToastShort(str("revanced_gemini_error_no_video"));
                        return;
                    }

                    GeminiManager.getInstance().startSummarization(context, videoUrl);
                }
        ),
        VOICE_OVER_TRANSLATION(
                Settings.SHORTS_CUSTOM_ACTIONS_VOICE_OVER_TRANSLATION,
                "revanced_vot_button_icon",
                () -> {
                    if (!PatchStatus.VoiceOverTranslation()) {
                        return;
                    }
                    Utils.runOnMainThreadDelayed(VoiceOverTranslationPatch::toggleTranslation, 300);
                },
                () -> {
                    if (!PatchStatus.VoiceOverTranslation()) {
                        return;
                    }
                    Context context = contextRef.get();
                    if (context != null) {
                        VideoUtils.showVotBottomSheetDialog(context);
                    }
                }
        ),
        REPEAT_STATE(
                Settings.SHORTS_CUSTOM_ACTIONS_REPEAT_STATE,
                "yt_outline_arrow_repeat_1_black_24",
                () -> VideoUtils.showShortsRepeatDialog(contextRef.get())
        );

        @NonNull
        private final BooleanSetting settings;

        @NonNull
        private final Drawable drawable;

        private final int drawableId;

        @NonNull
        private final String label;

        @NonNull
        private final Runnable onClickAction;

        @Nullable
        private final Runnable onLongClickAction;

        CustomAction(@NonNull BooleanSetting settings,
                     @NonNull String icon,
                     @NonNull Runnable onClickAction
        ) {
            this(settings, icon, onClickAction, null);
        }

        CustomAction(@NonNull BooleanSetting settings,
                     @NonNull String icon,
                     @NonNull Runnable onClickAction,
                     @Nullable Runnable onLongClickAction
        ) {
            this.drawable = Objects.requireNonNull(ResourceUtils.getDrawable(icon));
            this.drawableId = ResourceUtils.getDrawableIdentifier(icon);
            this.label = getString(settings.key + "_label");
            this.settings = settings;
            this.onClickAction = onClickAction;
            this.onLongClickAction = onLongClickAction;
        }

        @NonNull
        public Drawable getDrawable() {
            return drawable;
        }

        public int getDrawableId() {
            return drawableId;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        @NonNull
        public Runnable getOnClickAction() {
            return onClickAction;
        }

        @NonNull
        public Runnable getOnClickActionWithFlyoutMenuDismiss() {
            return () -> {
                hideFlyoutMenu();
                onClickAction.run();
            };
        }

        @NonNull
        public View.OnClickListener getOnClickListener() {
            return v -> getOnClickActionWithFlyoutMenuDismiss().run();
        }

        @Nullable
        public View.OnLongClickListener getOnLongClickListener() {
            if (onLongClickAction == null) {
                return null;
            } else {
                return v -> {
                    hideFlyoutMenu();
                    onLongClickAction.run();
                    return true;
                };
            }
        }
    }

}
