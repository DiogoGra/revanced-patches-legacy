package app.morphe.extension.youtube.patches.utils;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class ShortsCaptionsPatchTest {
    private final List<String> events = new ArrayList<>();
    private final Object activity = new Object();
    private Controller controller;
    private Menu menu;

    private final class Row implements ShortsCaptionsPatch.NativeRow {
        int clicks;
        public void rvxClickCaptions() { clicks++; events.add("click"); }
    }

    private final class Controller implements ShortsCaptionsPatch.NativeController {
        Object activity;
        boolean available = true;
        Object row = new Row();
        Controller(Object activity) { this.activity = activity; }
        public Object rvxCaptionsActivity() { return activity; }
        public boolean rvxCaptionsAvailable() { return available; }
        public Object rvxCaptionsRow() { return row; }
        public boolean rvxCaptionsCurrentRow(Object item) { return row == item; }
    }

    private final class Menu implements ShortsCaptionsPatch.NativeMenu {
        Object activity;
        int dismissals;
        Menu(Object activity) { this.activity = activity; }
        public Object rvxCaptionsActivity() { return activity; }
        public void rvxDismissCaptionsMenu() { dismissals++; events.add("dismiss"); }
    }

    @Before
    public void setUp() {
        controller = new Controller(activity);
        menu = new Menu(activity);
        ShortsCaptionsPatch.onControllerAvailable(controller);
    }

    @Test
    public void restoresOnlyTheNativeRowForThisActivity() {
        assertSame(controller.row, ShortsCaptionsPatch.restoreMenuItem(true, true, menu));
    }

    @Test
    public void ignoresOtherMenuElements() {
        assertNull(ShortsCaptionsPatch.restoreMenuItem(true, false, menu));
    }

    @Test
    public void doesNotChangeNormalVideoMenus() {
        assertNull(ShortsCaptionsPatch.restoreMenuItem(false, true, menu));
    }

    @Test
    public void unavailableCaptionsAreNotRestored() {
        controller.available = false;
        assertNull(ShortsCaptionsPatch.restoreMenuItem(true, true, menu));
    }

    @Test
    public void refusesAControllerFromAnotherActivity() {
        menu.activity = new Object();
        assertNull(ShortsCaptionsPatch.restoreMenuItem(true, true, menu));
    }

    @Test
    public void refusesMissingActivity() {
        menu.activity = null;
        assertNull(ShortsCaptionsPatch.restoreMenuItem(true, true, menu));
    }

    @Test
    public void requiresTheNativeRowContract() {
        controller.row = new Object();
        assertNull(ShortsCaptionsPatch.restoreMenuItem(true, true, menu));
    }

    @Test
    public void dismissesTheMenuBeforeInvokingTheNativeCallback() {
        Row row = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(true, menu, row));
        assertEquals(List.of("dismiss", "click"), events);
        assertEquals(1, row.clicks);
    }

    @Test
    public void doesNotInterceptAnUnregisteredNativeRow() {
        Row row = new Row();
        assertFalse(ShortsCaptionsPatch.handleMenuItemClick(true, menu, row));
        assertEquals(0, row.clicks);
        assertEquals(0, menu.dismissals);
    }

    @Test
    public void unavailableControllerCannotHandleAStaleClick() {
        Row row = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        controller.available = false;
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(true, menu, row));
        assertEquals(0, row.clicks);
        assertEquals(1, menu.dismissals);
    }

    @Test
    public void changingNativeRowsInvalidatesTheOldRow() {
        Row row = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        controller.row = new Row();
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(true, menu, row));
        assertEquals(0, row.clicks);
    }

    @Test
    public void changingActivitiesInvalidatesTheClick() {
        Row row = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        menu.activity = new Object();
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(true, menu, row));
        assertEquals(0, row.clicks);
    }

    @Test
    public void leavingShortsDoesNotOpenTheSelector() {
        Row row = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(false, menu, row));
        assertEquals(0, row.clicks);
    }

    @Test
    public void resetReleasesTheOldRowAndTheNewRowCanBeRestored() {
        Row old = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        ShortsCaptionsPatch.onControllerReset(controller);
        controller.row = new Row();
        assertFalse(ShortsCaptionsPatch.handleMenuItemClick(true, menu, old));
        Row current = (Row) ShortsCaptionsPatch.restoreMenuItem(true, true, menu);
        assertNotSame(old, current);
        assertTrue(ShortsCaptionsPatch.handleMenuItemClick(true, menu, current));
        assertEquals(0, old.clicks);
        assertEquals(1, current.clicks);
    }
}
