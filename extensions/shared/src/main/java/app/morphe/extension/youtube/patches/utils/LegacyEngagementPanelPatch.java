package app.morphe.extension.youtube.patches.utils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.VideoInformation;

/** Adapts the consolidated panel command to 19.16.39's existing models, loader and renderers. */
@SuppressWarnings("unused")
public final class LegacyEngagementPanelPatch {
    public interface NativeCommand {
        byte[] rvxPanelUpdateData();
        String rvxPanelShowId();
    }
    public interface NativeModel {
        String rvxPanelIdentifier();
        int rvxPanelBodyType();
        NativeModel rvxPanelWithContent(NativeModel child);
        NativeModel rvxPanelWithIdentifier(String identifier);
    }
    public interface NativePanel {
        NativeModel rvxPanelModel();
        boolean rvxPanelIsTranscript();
        void rvxPanelUpdate(NativeModel model);
    }
    public interface NativeTranscript extends NativePanel {
        boolean rvxPanelWatchTranscriptEntities(NativeModel expected);
    }
    public interface NativeController {
        String rvxPanelActiveId();
        NativePanel rvxPanelGet(String identifier);
        void rvxPanelRegister(NativeModel model);
        void rvxPanelRebuild(NativeModel model, NativePanel previous, NativeCommand anchor);
    }
    public interface NativeLoader {
        void rvxPanelFetch(String identifier, String parameters, FetchCallback callback);
    }

    private static final ThreadLocal<Boolean> selectingRenderer = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<NativePanel> retainedTranscript = new ThreadLocal<>();
    private static volatile WeakReference<NativeLoader> loader = new WeakReference<>(null);
    private static volatile State state;

    private static final class State {
        final WeakReference<NativeController> owner;
        final NativeCommand anchor;
        final String video;
        final ConcurrentHashMap<String, FetchCallback> pending = new ConcurrentHashMap<>();
        volatile String selected;

        State(NativeController owner, NativeCommand anchor, String video) {
            this.owner = new WeakReference<>(owner);
            this.anchor = anchor;
            this.video = video;
        }
    }

    public static Object findUpdateValue(int count, int[] tags, Object[] values) {
        if (tags == null || values == null || count < 0 || count > 128
                || count > tags.length || count > values.length) return null;
        for (int i = 0; i < count; i++) if (tags[i] == ((22556 << 3) | 2)) return values[i];
        return null;
    }

    public static void onLoaderAvailable(Object value) {
        if (value instanceof NativeLoader nativeLoader) loader = new WeakReference<>(nativeLoader);
    }

    public static void onPanelOpening(Object owner, Object command) {
        if (!(owner instanceof NativeController controller) || !(command instanceof NativeCommand nativeCommand)) return;
        try {
            if (!LegacyEngagementPanelCommand.PARENT.equals(nativeCommand.rvxPanelShowId())) return;
            State current = state;
            if (selectingRenderer.get() && current != null && current.owner.get() == controller) return;
            state = new State(controller, nativeCommand, VideoInformation.getVideoId());
        } catch (Exception ex) {
            logFailure("opening", ex);
        }
    }

    public static boolean tryHandle(Object value) {
        if (!(value instanceof NativeCommand command)) return false;
        try {
            LegacyEngagementPanelCommand request = LegacyEngagementPanelCommand.parse(command.rvxPanelUpdateData());
            State current = state;
            if (request == null || !current(current)) return false;
            current.selected = request.child;
            if (apply(current, request.child)) return true;
            NativeLoader nativeLoader = loader.get();
            if (!"PAtimeline_view".equals(request.child) || request.parameters == null || nativeLoader == null) return false;
            FetchCallback previous = current.pending.get(request.child);
            if (previous != null && System.nanoTime() - previous.started < 60_000_000_000L) return true;
            FetchCallback callback = new FetchCallback(current, request.child);
            current.pending.put(request.child, callback);
            try {
                nativeLoader.rvxPanelFetch(request.child, request.parameters, callback);
                return true;
            } catch (Exception ex) {
                current.pending.remove(request.child, callback);
                logFailure("request", ex);
                return false;
            }
        } catch (Exception ex) {
            logFailure("command", ex);
            return false;
        }
    }

    private static boolean current(State candidate) {
        if (candidate == null || candidate != state || !candidate.video.equals(VideoInformation.getVideoId())) return false;
        NativeController owner = candidate.owner.get();
        return owner != null && LegacyEngagementPanelCommand.PARENT.equals(owner.rvxPanelActiveId());
    }

    private static boolean supportedBody(int type) {
        return type == 49399797 || type == 154115041 || type == 449330433;
    }

    private static boolean apply(State current, String childId) {
        if (!current(current) || !childId.equals(current.selected)) return false;
        NativeController owner = current.owner.get();
        NativePanel parent = owner.rvxPanelGet(LegacyEngagementPanelCommand.PARENT);
        NativePanel child = owner.rvxPanelGet(childId);
        if (parent == null || child == null || parent.rvxPanelModel() == null || child.rvxPanelModel() == null) return false;
        NativeModel childModel = child.rvxPanelModel();
        if (!supportedBody(childModel.rvxPanelBodyType())) return false;
        NativeModel updated = parent.rvxPanelModel().rvxPanelWithContent(childModel);
        if (child.rvxPanelIsTranscript() || parent.rvxPanelIsTranscript() != child.rvxPanelIsTranscript()) {
            rebuild(owner, updated, parent, current.anchor);
        } else {
            parent.rvxPanelUpdate(updated);
        }
        return true;
    }

    static void rebuild(NativeController owner, NativeModel model, NativePanel previous, NativeCommand anchor) {
        // Let the cached native child own the shared entities and their eventual cleanup.
        // Transient consolidated renderers may release their views, not that child's data.
        NativePanel cached = owner.rvxPanelGet("engagement-panel-searchable-transcript");
        NativeModel expected = previous.rvxPanelIsTranscript() ? previous.rvxPanelModel() : model;
        if (cached != previous && cached instanceof NativeTranscript transcript && transcript.rvxPanelWatchTranscriptEntities(expected)
                && previous.rvxPanelIsTranscript()) {
            retainedTranscript.set(previous);
        }
        selectingRenderer.set(true);
        try { owner.rvxPanelRebuild(model, previous, anchor); }
        finally {
            retainedTranscript.remove();
            selectingRenderer.remove();
        }
    }

    public static boolean shouldUseNativeFactory(Object value) {
        return selectingRenderer.get() && value instanceof NativeModel model
                && model.rvxPanelBodyType() == 154115041
                && LegacyEngagementPanelCommand.PARENT.equals(model.rvxPanelIdentifier());
    }

    public static boolean shouldSkipTranscriptEntityCleanup(Object value) {
        return skipTranscriptEntityCleanup(retainedTranscript.get(), value);
    }

    static boolean skipTranscriptEntityCleanup(NativePanel retained, Object value) {
        if (retained == null || retained != value || !retained.rvxPanelIsTranscript()) return false;
        NativePanel panel = retained;
        NativeModel model = panel.rvxPanelModel();
        // Never suppress the cached child's normal cleanup or another panel's lifecycle.
        return model != null && model.rvxPanelBodyType() == 154115041
                && LegacyEngagementPanelCommand.PARENT.equals(model.rvxPanelIdentifier());
    }

    public static final class FetchCallback {
        private final State owner;
        private final String child;
        private final long started = System.nanoTime();

        private FetchCallback(State owner, String child) {
            this.owner = owner;
            this.child = child;
        }

        public void onFailure() {
            owner.pending.remove(child, this);
        }

        public void onSuccess(NativeModel model) {
            Utils.runOnMainThreadNowOrLater(() -> {
                try {
                    if (state != owner || !owner.video.equals(VideoInformation.getVideoId())) return;
                    String identifier = model.rvxPanelIdentifier();
                    if (identifier != null && !identifier.isEmpty() && !identifier.equals(child)
                            && !identifier.equals(LegacyEngagementPanelCommand.PARENT)) return;
                    // Match 20.05.46's callback: the request child target identifies the fetched content.
                    NativeModel identified = model.rvxPanelWithIdentifier(child);
                    NativeController controller = owner.owner.get();
                    if (controller == null) return;
                    controller.rvxPanelRegister(identified);
                    apply(owner, child);
                } catch (Exception ex) {
                    logFailure("response", ex);
                } finally {
                    owner.pending.remove(child, this);
                }
            });
        }
    }

    private static void logFailure(String phase, Exception ex) {
        // Native exceptions can include serialized messages. Retain only the exception type.
        Logger.printException(() -> "Legacy engagement panel " + phase + " failed: " + ex.getClass().getSimpleName());
    }
}
