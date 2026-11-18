import java.awt.Window;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/** Nastavení vzhledu, uložené pro příští spuštění aplikace. */
public final class AppearanceManager {
    public enum Mode { AUTOMATICKY, SVETLY, TMAVY }
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppearanceManager.class);
    private AppearanceManager() { }

    public static Mode getMode() {
        try { return Mode.valueOf(PREFS.get("appearance", Mode.AUTOMATICKY.name())); }
        catch (IllegalArgumentException ex) { return Mode.AUTOMATICKY; }
    }

    public static void setMode(Mode mode) {
        PREFS.put("appearance", mode.name());
        apply(mode);
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.repaint();
        }
    }

    public static void applySaved() { apply(getMode()); }

    private static void apply(Mode mode) {
        boolean dark = mode == Mode.TMAVY || (mode == Mode.AUTOMATICKY && windowsDarkMode());
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ScrollBar.width", 12);
    }

    private static boolean windowsDarkMode() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;
        try {
            Process process = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme").redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().reduce("", (a, b) -> a + " " + b);
            }
            return process.waitFor() == 0 && output.matches("(?is).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0(?:\\s.*)?");
        } catch (Exception ex) {
            return false;
        }
    }
}
