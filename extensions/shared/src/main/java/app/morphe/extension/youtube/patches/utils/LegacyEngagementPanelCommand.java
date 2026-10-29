package app.morphe.extension.youtube.patches.utils;

import com.google.protobuf.CodedInputStream;
import java.io.IOException;

/** Only the routing metadata needed by the legacy panel adapter. */
final class LegacyEngagementPanelCommand {
    static final String PARENT = "engagement-panel-timeline-view-consolidated";
    final String parent;
    final String child;
    final String parameters;
    final boolean replace;

    private LegacyEngagementPanelCommand(String parent, String child, String parameters, boolean replace) {
        this.parent = parent;
        this.child = child;
        this.parameters = parameters;
        this.replace = replace;
    }

    static boolean supportedChild(String child) {
        return "engagement-panel-macro-markers-description-chapters".equals(child)
                || "engagement-panel-macro-markers-auto-chapters".equals(child)
                || "engagement-panel-searchable-transcript".equals(child)
                || "PAtimeline_view".equals(child);
    }

    static LegacyEngagementPanelCommand parse(byte[] data) throws IOException {
        if (data == null || data.length == 0 || data.length > 65536) return null;
        CodedInputStream input = CodedInputStream.newInstance(data);
        input.setRecursionLimit(8);
        input.setSizeLimit(65536);
        String parent = null, child = null, parameters = null;
        boolean replace = false;
        for (int tag; (tag = input.readTag()) != 0;) {
            switch (tag) {
                case 10: parent = stringField3(input.readByteArray()); break;
                case 18: child = stringField3(input.readByteArray()); break;
                case 26: parameters = stringField3(input.readByteArray()); break;
                case 32: replace = input.readBool(); break;
                default:
                    if (!input.skipField(tag)) return null;
            }
        }
        if (!PARENT.equals(parent) || !supportedChild(child) || replace) return null;
        if (parameters != null && parameters.length() > 8192) return null;
        return new LegacyEngagementPanelCommand(parent, child, parameters, replace);
    }

    private static String stringField3(byte[] data) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(data);
        input.setRecursionLimit(8);
        String value = null;
        for (int tag; (tag = input.readTag()) != 0;) {
            if (tag == 26) value = input.readStringRequireUtf8();
            else if (!input.skipField(tag)) return null;
        }
        return value;
    }
}
