import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class KdeJsouMojePrachyApp {
    private static final String APP_TITLE = "Kde jsou moje prachy?";
    private JFrame mainFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Použije se výchozí Swing vzhled.
            }
            try {
                DatabaseManipulator.initializeDatabase();
                new KdeJsouMojePrachyApp().showMainWindow();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Aplikaci se nepodařilo spustit.\n" + exception.getMessage(),
                        "Chyba", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showMainWindow() {
        mainFrame = new JFrame(APP_TITLE);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(800, 500));
        mainFrame.setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton settingsButton = new JButton("⚙");
        settingsButton.setToolTipText("Nastavení");
        settingsButton.setFont(findSymbolFont(22));
        settingsButton.setHorizontalAlignment(SwingConstants.CENTER);
        settingsButton.setMargin(new Insets(4, 10, 4, 10));
        settingsButton.setFocusable(false);
        settingsButton.getAccessibleContext().setAccessibleName("Nastavení");
        settingsButton.addActionListener(event -> new SettingsDialog(mainFrame).setVisible(true));
        toolbar.add(settingsButton);

        mainFrame.add(toolbar, BorderLayout.NORTH);
        mainFrame.add(new JPanel(), BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static Font findSymbolFont(int size) {
        for (String name : new String[] {"Segoe UI Symbol", "Segoe UI Emoji", "Dialog"}) {
            Font font = new Font(name, Font.PLAIN, size);
            if (font.canDisplay('⚙')) {
                return font;
            }
        }
        return UIManager.getFont("Button.font").deriveFont((float) size);
    }
}
