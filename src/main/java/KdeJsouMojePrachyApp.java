import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Vstupní bod aplikace. Inicializuje prostředí a spustí hlavní okno. */
public final class KdeJsouMojePrachyApp {
    private KdeJsouMojePrachyApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 18);
            UIManager.put("Component.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ScrollBar.width", 12);
            try {
                DatabaseManipulator.initializeDatabase();
                new MainFrame().setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Aplikaci se nepodařilo spustit.\n" + exception.getMessage(),
                        "Chyba", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
