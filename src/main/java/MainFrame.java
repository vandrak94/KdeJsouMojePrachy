import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Hlavní okno aplikace včetně jeho ovládacích prvků a akcí. */
public final class MainFrame extends JFrame {
    private static final String APP_TITLE = "Kde jsou moje prachy?";

    public MainFrame() {
        super(APP_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton settingsButton = new JButton("⚙");
        settingsButton.setToolTipText("Nastavení");
        settingsButton.setFont(findSymbolFont(22));
        settingsButton.setHorizontalAlignment(SwingConstants.CENTER);
        settingsButton.setMargin(new Insets(4, 10, 4, 10));
        settingsButton.setFocusable(false);
        settingsButton.getAccessibleContext().setAccessibleName("Nastavení");
        settingsButton.addActionListener(event -> new SettingsDialog(this).setVisible(true));
        toolbar.add(settingsButton);

        add(toolbar, BorderLayout.NORTH);
        add(new JPanel(), BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
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
