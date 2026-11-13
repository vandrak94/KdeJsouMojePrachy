import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/** Stores independent window dimensions between application launches. */
public final class DialogSizePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(DialogSizePreferences.class)
            .node("dialogSizes");

    private DialogSizePreferences() { }

    /** Call after pack() and before setVisible(true). */
    public static void restoreAndTrack(Window dialog, String key) {
        Dimension preferred = dialog.getSize();
        int width = PREFS.getInt(key + ".width", preferred.width);
        int height = PREFS.getInt(key + ".height", preferred.height);
        Dimension minimum = dialog.getMinimumSize();
        width = Math.max(width, minimum.width);
        height = Math.max(height, minimum.height);

        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        width = Math.min(width, available.width);
        height = Math.min(height, available.height);
        dialog.setSize(width, height);

        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                save(dialog, key);
            }
        });
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                save(dialog, key);
            }
            @Override
            public void windowClosed(WindowEvent event) {
                save(dialog, key);
            }
        });
    }

    private static void save(Window dialog, String key) {
        Dimension size = dialog.getSize();
        if (size.width > 0 && size.height > 0) {
            PREFS.putInt(key + ".width", size.width);
            PREFS.putInt(key + ".height", size.height);
        }
    }
}
