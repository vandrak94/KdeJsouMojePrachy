import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public final class SettingsDialog extends JDialog {
    private JPanel categoryLabels;
    public SettingsDialog(Frame owner) {
        super(owner, "Nastavení", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(560, 380));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Databáze", createDatabaseTab());
        tabs.addTab("Kategorie", createCategoryTab());
        add(tabs, BorderLayout.CENTER);

        pack();
        DialogSizePreferences.restoreAndTrack(this, "settings");
        setLocationRelativeTo(owner);
    }

    private JPanel createDatabaseTab() {
        JPanel panel = createTabPanel("Databáze");
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JButton browserButton = new JButton("Prohlížeč databáze");
        browserButton.setAlignmentX(LEFT_ALIGNMENT);
        browserButton.addActionListener(event -> new DatabaseBrowser(this).setVisible(true));

        JButton expensesButton = new JButton("Vyčistit tabulku Výdaje");
        expensesButton.setAlignmentX(LEFT_ALIGNMENT);
        expensesButton.addActionListener(event -> confirmAndClear(true));

        JButton categoriesButton = new JButton("Vyčistit tabulku Kategorie");
        categoriesButton.setAlignmentX(LEFT_ALIGNMENT);
        categoriesButton.addActionListener(event -> confirmAndClear(false));

        controls.add(browserButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(expensesButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(categoriesButton);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.add(controls);
        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCategoryTab() {
        JPanel panel = createTabPanel("Kategorie");
        JPanel content = new JPanel(new BorderLayout(0, 12));

        JButton addButton = new JButton("+");
        addButton.setToolTipText("Přidat kategorii");
        addButton.getAccessibleContext().setAccessibleName("Přidat kategorii");
        addButton.addActionListener(event -> {
            new AddCategoryDialog(this).setVisible(true);
            refreshCategoryLabels();
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.add(addButton);
        content.add(controls, BorderLayout.NORTH);

        categoryLabels = new JPanel();
        categoryLabels.setLayout(new BoxLayout(categoryLabels, BoxLayout.Y_AXIS));
        categoryLabels.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel labelsTop = new JPanel(new BorderLayout());
        labelsTop.add(categoryLabels, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(labelsTop);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scrollPane, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        refreshCategoryLabels();
        return panel;
    }

    private void refreshCategoryLabels() {
        if (categoryLabels == null) return;
        categoryLabels.removeAll();
        try {
            for (DatabaseManipulator.Category category : DatabaseManipulator.getCategories()) {
                categoryLabels.add(createCategoryRow(category));
                categoryLabels.add(Box.createVerticalStrut(8));
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "Kategorie se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        categoryLabels.revalidate();
        categoryLabels.repaint();
    }

    private JPanel createCategoryRow(DatabaseManipulator.Category category) {
        Color background = parseCategoryColor(category.color());
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel badge = new GlossLabel(category.name(), background);
        double luminance = 0.2126 * background.getRed()
                + 0.7152 * background.getGreen() + 0.0722 * background.getBlue();
        badge.setForeground(luminance < 145 ? Color.WHITE : Color.BLACK);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)));
        row.add(badge, BorderLayout.CENTER);

        JButton editButton = new JButton("Upravit");
        editButton.setToolTipText("Upravit kategorii");
        editButton.addActionListener(event -> {
            EditCategoryDialog dialog = new EditCategoryDialog(this, category);
            dialog.setVisible(true);
            if (dialog.isSaved()) refreshCategoryLabels();
        });

        JButton deleteButton = new JButton("-");
        deleteButton.setToolTipText("Odstranit kategorii");
        deleteButton.addActionListener(event -> deleteCategory(category));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 4));
        actions.add(editButton);
        actions.add(deleteButton);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void deleteCategory(DatabaseManipulator.Category category) {
        Object[] options = {"Ano", "Ne"};
        int answer = JOptionPane.showOptionDialog(this,
                "Opravdu smazat kategorii?", "Potvrzení",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            DatabaseManipulator.deleteCategory(category.id());
            refreshCategoryLabels();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "Kategorii se nepodařilo odstranit.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Color parseCategoryColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}")
                    ? Color.decode(value) : Color.GRAY;
        } catch (NumberFormatException exception) {
            return Color.GRAY;
        }
    }

    private static final class GlossLabel extends JLabel {
        private final Color baseColor;

        private GlossLabel(String text, Color baseColor) {
            super(text);
            this.baseColor = baseColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color top = new Color(
                    Math.min(255, baseColor.getRed() + 45),
                    Math.min(255, baseColor.getGreen() + 45),
                    Math.min(255, baseColor.getBlue() + 45));
            g.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(),
                    new float[] {0f, 0.48f, 1f},
                    new Color[] {top, baseColor, baseColor.darker()}));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static JPanel createTabPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel heading = new JLabel(title);
        Font font = heading.getFont();
        heading.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 4));
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private void confirmAndClear(boolean expenses) {
        Object[] options = {"Ano", "Ne"};
        int answer = JOptionPane.showOptionDialog(this, "Opravdu vymazat?", "Potvrzení",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (expenses) {
                DatabaseManipulator.clearExpenses();
            } else {
                DatabaseManipulator.clearCategories();
                refreshCategoryLabels();
            }
            JOptionPane.showMessageDialog(this, "Tabulka byla vyčištěna.",
                    "Hotovo", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "Tabulku se nepodařilo vyčistit.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }
}
