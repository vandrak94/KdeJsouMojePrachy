import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class KdeJsouMojePrachyApp {

    private static final String APP_TITLE = "Kde jsou moje prachy?";
    private static final Path DATABASE_PATH = Path.of("data", "database.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();

    private JFrame mainFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            try {
                initializeDatabase();
                new KdeJsouMojePrachyApp().showMainWindow();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Aplikaci se nepodařilo spustit.\n" + exception.getMessage(),
                        "Chyba",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Výchozí vzhled Swingu je bezpečný fallback.
        }
    }

    private static void initializeDatabase() throws Exception {
        Files.createDirectories(DATABASE_PATH.getParent());

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS kategorie (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nazev TEXT NOT NULL,
                        barva TEXT
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vydaje (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        datum TEXT NOT NULL,
                        kategorie INTEGER,
                        castka NUMERIC NOT NULL,
                        poznamka TEXT,
                        FOREIGN KEY (kategorie) REFERENCES kategorie(id)
                    )
                    """);
        }
    }

    private static Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void showMainWindow() {
        mainFrame = new JFrame(APP_TITLE);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(800, 500));
        mainFrame.setLocationByPlatform(true);
        mainFrame.setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton settingsButton = createSettingsButton();
        toolbar.add(settingsButton);

        mainFrame.add(toolbar, BorderLayout.NORTH);
        mainFrame.add(new JPanel(), BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private JButton createSettingsButton() {
        JButton button = new JButton("⚙");
        button.setToolTipText("Nastavení");
        button.setFont(findSymbolFont(22));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(4, 10, 4, 10));
        button.setFocusable(false);
        button.getAccessibleContext().setAccessibleName("Nastavení");
        button.addActionListener(event -> showSettingsDialog());
        return button;
    }

    private static Font findSymbolFont(int size) {
        String[] candidates = {"Segoe UI Symbol", "Segoe UI Emoji", "Dialog"};
        for (String candidate : candidates) {
            Font font = new Font(candidate, Font.PLAIN, size);
            if (font.canDisplay('⚙')) {
                return font;
            }
        }
        return UIManager.getFont("Button.font").deriveFont((float) size);
    }

    private void showSettingsDialog() {
        JDialog dialog = new JDialog(mainFrame, "Nastavení", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(560, 380));
        dialog.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Databáze", createDatabaseTab(dialog));
        tabs.addTab("Kategorie", createCategoryTab());

        dialog.add(tabs, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private JPanel createDatabaseTab(Window owner) {
        JPanel tab = createTabPanel("Databáze");

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 10));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton clearExpensesButton = new JButton("Vyčistit tabulku Výdaje");
        clearExpensesButton.addActionListener(event -> confirmAndClearTable(owner, "vydaje"));

        JButton clearCategoriesButton = new JButton("Vyčistit tabulku Kategorie");
        clearCategoriesButton.addActionListener(event -> confirmAndClearTable(owner, "kategorie"));

        buttons.add(clearExpensesButton);
        buttons.add(clearCategoriesButton);
        tab.add(buttons, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createCategoryTab() {
        return createTabPanel("Kategorie");
    }

    private JPanel createTabPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel(title);
        Font baseFont = heading.getFont();
        heading.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 4));
        panel.add(heading, BorderLayout.NORTH);

        return panel;
    }

    private void confirmAndClearTable(Window owner, String tableName) {
        Object[] options = {"Ano", "Ne"};
        int result = JOptionPane.showOptionDialog(
                owner,
                "Opravdu vymazat?",
                "Potvrzení",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            clearTable(tableName);
            JOptionPane.showMessageDialog(
                    owner,
                    "Tabulka byla vyčištěna.",
                    "Hotovo",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    owner,
                    "Tabulku se nepodařilo vyčistit.\n" + exception.getMessage(),
                    "Chyba",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void clearTable(String tableName) throws SQLException {
        if (!"vydaje".equals(tableName) && !"kategorie".equals(tableName)) {
            throw new IllegalArgumentException("Nepovolený název tabulky: " + tableName);
        }

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {

            connection.setAutoCommit(false);
            try {
                statement.executeUpdate("DELETE FROM " + tableName);
                statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = '" + tableName + "'");
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
