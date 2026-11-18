import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class KdeJsouMojePrachyApp {
    private KdeJsouMojePrachyApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 14);
            UIManager.put("Component.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ScrollBar.width", 12);
            try {
                DatabaseManipulator.initializeDatabase();
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                if (CategoryColorUniqueness.hasDuplicates()) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Některé kategorie používají stejnou barvu.\n"
                                    + "Před pokračováním nastavte každé kategorii jedinečnou barvu.",
                            "Duplicitní barvy kategorií", JOptionPane.WARNING_MESSAGE);
                    new SettingsDialog(mainFrame, true).setVisible(true);
                }
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Aplikaci se nepodařilo spustit.\n" + exception.getMessage(),
                        "Chyba", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
