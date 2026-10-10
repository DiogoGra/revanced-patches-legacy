package app.morphe.extension.youtube.patches.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MissingResourcesPatchTest {
    @Test
    public void cairoToolbarActivityUsesLegacyBell() {
        assertEquals(355, MissingResourcesPatch.getLegacyIconType(1156));
    }

    @Test
    public void cairoSearchUsesLegacySearch() {
        assertEquals(60, MissingResourcesPatch.getLegacyIconType(1160));
    }

    @Test
    public void cairoCreateUsesLegacyAddCircle() {
        assertEquals(734, MissingResourcesPatch.getLegacyIconType(1161));
    }

    @Test
    public void cairoNotificationsUseLegacyBell() {
        assertEquals(264, MissingResourcesPatch.getLegacyIconType(1185));
    }

    @Test
    public void cairoSettingsStillUsesLegacyGear() {
        assertEquals(44, MissingResourcesPatch.getLegacyIconType(1162));
    }

    @Test
    public void legacyAndShortsIconsKeepTheirIdentity() {
        for (int type : new int[]{44, 60, 264, 405, 650, 670, 730, 732, 734, 1045, 1114}) {
            assertEquals(type, MissingResourcesPatch.getLegacyIconType(type));
        }
    }

    @Test
    public void unrelatedIconsAreNotMappedToToolbarActions() {
        assertEquals(0, MissingResourcesPatch.getLegacyIconType(0));
        assertEquals(9999, MissingResourcesPatch.getLegacyIconType(9999));
    }
}
