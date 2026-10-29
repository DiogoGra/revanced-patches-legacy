package app.morphe.extension.youtube.patches.utils;

import org.junit.Test;
import static org.junit.Assert.*;
import app.morphe.extension.youtube.patches.utils.LegacyEngagementPanelPatch.NativeModel;
import app.morphe.extension.youtube.patches.utils.LegacyEngagementPanelPatch.NativePanel;
import app.morphe.extension.youtube.patches.utils.LegacyEngagementPanelPatch.NativeCommand;
import app.morphe.extension.youtube.patches.utils.LegacyEngagementPanelPatch.NativeController;
import app.morphe.extension.youtube.patches.utils.LegacyEngagementPanelPatch.NativeTranscript;

public class LegacyTranscriptLifecycleTest {
    private static NativePanel panel(String id, int body, boolean transcript) {
        NativeModel model = new NativeModel() {
            public String rvxPanelIdentifier() { return id; }
            public int rvxPanelBodyType() { return body; }
            public NativeModel rvxPanelWithContent(NativeModel child) { throw new UnsupportedOperationException(); }
            public NativeModel rvxPanelWithIdentifier(String value) { throw new UnsupportedOperationException(); }
        };
        return new NativePanel() {
            public NativeModel rvxPanelModel() { return model; }
            public boolean rvxPanelIsTranscript() { return transcript; }
            public void rvxPanelUpdate(NativeModel value) { throw new UnsupportedOperationException(); }
        };
    }

    @Test public void skipsOnlyTheRetainedConsolidatedTranscriptInstance() {
        NativePanel parent = panel(LegacyEngagementPanelCommand.PARENT, 154115041, true);
        assertTrue(LegacyEngagementPanelPatch.skipTranscriptEntityCleanup(parent, parent));
        assertFalse(LegacyEngagementPanelPatch.skipTranscriptEntityCleanup(null, parent));
        assertFalse(LegacyEngagementPanelPatch.skipTranscriptEntityCleanup(parent,
                panel(LegacyEngagementPanelCommand.PARENT, 154115041, true)));
    }

    @Test public void retainsNativeCleanupForStandaloneTranscriptAndOtherPanels() {
        for (NativePanel other : new NativePanel[]{
                panel("engagement-panel-searchable-transcript", 154115041, true),
                panel(LegacyEngagementPanelCommand.PARENT, 49399797, false),
                panel(LegacyEngagementPanelCommand.PARENT, 154115041, false)}) {
            assertFalse(LegacyEngagementPanelPatch.skipTranscriptEntityCleanup(other, other));
        }
        assertFalse(LegacyEngagementPanelPatch.skipTranscriptEntityCleanup(null, null));
    }

    private static NativeController controller(NativePanel cached, Runnable rebuild) {
        return new NativeController() {
            public String rvxPanelActiveId() { throw new UnsupportedOperationException(); }
            public NativePanel rvxPanelGet(String id) {
                assertEquals("engagement-panel-searchable-transcript", id);
                return cached;
            }
            public void rvxPanelRegister(NativeModel model) { throw new UnsupportedOperationException(); }
            public void rvxPanelRebuild(NativeModel model, NativePanel previous, NativeCommand anchor) { rebuild.run(); }
        };
    }

    private static NativeTranscript cached(Runnable watching, boolean supported) {
        NativePanel delegate = panel("engagement-panel-searchable-transcript", 154115041, true);
        return new NativeTranscript() {
            public boolean rvxPanelWatchTranscriptEntities(NativeModel expected) { watching.run(); return supported; }
            public NativeModel rvxPanelModel() { return delegate.rvxPanelModel(); }
            public boolean rvxPanelIsTranscript() { return true; }
            public void rvxPanelUpdate(NativeModel model) { throw new UnsupportedOperationException(); }
        };
    }

    @Test public void nativeChildOwnsEntityObservationBeforeTheTransientRendererIsReplaced() {
        int[] observations = {0};
        NativePanel previous = panel(LegacyEngagementPanelCommand.PARENT, 154115041, true);
        NativeTranscript cached = cached(() -> observations[0]++, true);
        LegacyEngagementPanelPatch.rebuild(controller(cached, () -> {
            assertEquals(1, observations[0]);
            assertTrue(LegacyEngagementPanelPatch.shouldSkipTranscriptEntityCleanup(previous));
            assertFalse(LegacyEngagementPanelPatch.shouldSkipTranscriptEntityCleanup(cached));
            assertTrue(LegacyEngagementPanelPatch.shouldUseNativeFactory(previous.rvxPanelModel()));
        }), previous.rvxPanelModel(), previous, null);
        assertFalse(LegacyEngagementPanelPatch.shouldSkipTranscriptEntityCleanup(previous));
        assertFalse(LegacyEngagementPanelPatch.shouldUseNativeFactory(previous.rvxPanelModel()));
    }

    @Test public void clearsReplacementScopeEvenWhenTheNativeFactoryFails() {
        NativePanel previous = panel(LegacyEngagementPanelCommand.PARENT, 154115041, true);
        try {
            LegacyEngagementPanelPatch.rebuild(controller(cached(() -> {}, true), () -> { throw new IllegalStateException("fixture"); }),
                    previous.rvxPanelModel(), previous, null);
            fail("Expected the native failure");
        } catch (IllegalStateException expected) { }
        assertFalse(LegacyEngagementPanelPatch.shouldSkipTranscriptEntityCleanup(previous));
        assertFalse(LegacyEngagementPanelPatch.shouldUseNativeFactory(previous.rvxPanelModel()));
    }

    @Test public void doesNotSuppressCleanupWithoutACompatibleNativeEntityOwner() {
        NativePanel previous = panel(LegacyEngagementPanelCommand.PARENT, 154115041, true);
        for (NativePanel owner : new NativePanel[]{null, previous, cached(() -> {}, false)}) {
            LegacyEngagementPanelPatch.rebuild(controller(owner, () -> {
                assertFalse(LegacyEngagementPanelPatch.shouldSkipTranscriptEntityCleanup(previous));
            }), previous.rvxPanelModel(), previous, null);
        }
    }
}
