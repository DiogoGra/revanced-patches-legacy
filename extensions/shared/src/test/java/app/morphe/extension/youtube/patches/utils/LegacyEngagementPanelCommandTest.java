package app.morphe.extension.youtube.patches.utils;

import com.google.protobuf.CodedOutputStream;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.junit.Assert.*;

public class LegacyEngagementPanelCommandTest {
    private static byte[] text(String value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        out.writeString(3, value);
        out.flush();
        return bytes.toByteArray();
    }

    private static byte[] command(String parent, String child, String params, boolean replace) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        out.writeByteArray(1, text(parent));
        out.writeByteArray(2, text(child));
        if (params != null) out.writeByteArray(3, text(params));
        if (replace) out.writeBool(4, true);
        out.flush();
        return bytes.toByteArray();
    }

    @Test public void acceptsOnlyTheKnownConsolidatedPanelChildren() throws Exception {
        for (String child : new String[]{"engagement-panel-macro-markers-description-chapters",
                "engagement-panel-macro-markers-auto-chapters", "engagement-panel-searchable-transcript", "PAtimeline_view"}) {
            LegacyEngagementPanelCommand parsed = LegacyEngagementPanelCommand.parse(command(
                    LegacyEngagementPanelCommand.PARENT, child, null, false));
            assertNotNull(parsed);
            assertEquals(child, parsed.child);
            assertNull(parsed.parameters);
        }
    }

    @Test public void forwardsOpaqueFetchParametersWithoutDecodingTheirContents() throws Exception {
        LegacyEngagementPanelCommand parsed = LegacyEngagementPanelCommand.parse(command(
                LegacyEngagementPanelCommand.PARENT, "PAtimeline_view", "opaque-fixture-parameters", false));
        assertNotNull(parsed);
        assertEquals("opaque-fixture-parameters", parsed.parameters);
    }

    @Test public void doesNotHandleOtherPanelsUnknownChildrenOrUnverifiedReplaceMode() throws Exception {
        assertNull(LegacyEngagementPanelCommand.parse(command("video-description-ep-identifier", "PAtimeline_view", null, false)));
        assertNull(LegacyEngagementPanelCommand.parse(command(LegacyEngagementPanelCommand.PARENT, "unknown-child", null, false)));
        assertNull(LegacyEngagementPanelCommand.parse(command(LegacyEngagementPanelCommand.PARENT, "PAtimeline_view", null, true)));
    }

    @Test public void ignoresUnknownOptionalFields() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(command(LegacyEngagementPanelCommand.PARENT, "PAtimeline_view", null, false));
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        out.writeUInt32(50, 7);
        out.flush();
        assertNotNull(LegacyEngagementPanelCommand.parse(bytes.toByteArray()));
    }

    @Test public void rejectsEmptyOversizedOrMalformedMessages() throws Exception {
        assertNull(LegacyEngagementPanelCommand.parse(null));
        assertNull(LegacyEngagementPanelCommand.parse(new byte[0]));
        assertNull(LegacyEngagementPanelCommand.parse(new byte[65537]));
        assertNull(LegacyEngagementPanelCommand.parse(command(LegacyEngagementPanelCommand.PARENT, "PAtimeline_view", "x".repeat(8193), false)));
        try {
            LegacyEngagementPanelCommand.parse(new byte[]{10, 127, 1});
            fail("Truncated length-delimited message was accepted");
        } catch (IOException expected) { }
    }

    @Test public void unknownFieldReaderValidatesTheNativeArraysAndTag() {
        Object value = new Object();
        assertSame(value, LegacyEngagementPanelPatch.findUpdateValue(1, new int[]{180450}, new Object[]{value}));
        assertNull(LegacyEngagementPanelPatch.findUpdateValue(1, new int[]{180448}, new Object[]{value}));
        assertNull(LegacyEngagementPanelPatch.findUpdateValue(2, new int[]{180450}, new Object[]{value}));
        assertNull(LegacyEngagementPanelPatch.findUpdateValue(-1, new int[0], new Object[0]));
        assertNull(LegacyEngagementPanelPatch.findUpdateValue(0, null, null));
    }
}
