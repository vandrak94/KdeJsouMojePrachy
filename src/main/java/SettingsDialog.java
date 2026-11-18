import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Set;
import javax.swing.*;

public final class SettingsDialog extends JDialog {
    private JPanel categoryLabels;
    private final JTabbedPane tabs = new JTabbedPane();
    private final boolean forcedCategoryRepair;
    private boolean duplicatesRemain;

    public SettingsDialog(Frame owner) {
        this(owner, false);
    }

    public SettingsDialog(Frame owner, boolean forcedCategoryRepair) {
        super(owner, "Nastavení", true);
        this.forcedCategoryRepair = forcedCategoryRepair;
        setDefaultCloseOperation(forcedCategoryRepair
                ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(650, 470));
        setLayout(new BorderLayout());
        tabs.addTab("Databáze", createDatabaseTab());
        tabs.addTab("Kategorie", createCategoryTab());
        add(tabs, BorderLayout.CENTER);
        if (forcedCategoryRepair) {
            tabs.setSelectedIndex(1);
            tabs.setEnabledAt(0, false);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    if (duplicatesRemain) {
                        JOptionPane.showMessageDialog(SettingsDialog.this,
                                "Nastavení nelze zavřít, dokud mají dvě nebo více kategorií stejnou barvu.",
                                "Duplicitní barvy kategorií", JOptionPane.WARNING_MESSAGE);
                    } else {
                        dispose();
                    }
                }
            });
        }
        pack();
        DialogSizePreferences.restoreAndTrack(this, "settings");
        setLocationRelativeTo(owner);
    }

    private JPanel createDatabaseTab() {
        JPanel panel = createTabPanel("Databáze");
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        JButton browser = new JButton("Prohlížeč databáze");
        browser.setAlignmentX(LEFT_ALIGNMENT);
        browser.addActionListener(e -> new DatabaseBrowser(this).setVisible(true));
        JButton expenses = new JButton("Vyčistit tabulku Výdaje");
        expenses.setAlignmentX(LEFT_ALIGNMENT);
        expenses.addActionListener(e -> confirmAndClear(true));
        JButton categories = new JButton("Vyčistit tabulku Kategorie");
        categories.setAlignmentX(LEFT_ALIGNMENT);
        categories.addActionListener(e -> confirmAndClear(false));
        controls.add(browser); controls.add(Box.createVerticalStrut(10));
        controls.add(expenses); controls.add(Box.createVerticalStrut(10)); controls.add(categories);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.add(controls); panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCategoryTab() {
        JPanel panel = createTabPanel("Kategorie");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        JButton add = new JButton("Přidat kategorii");
        add.addActionListener(e -> { new AddCategoryDialog(this).setVisible(true); refreshCategoryLabels(); });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.add(add); content.add(controls, BorderLayout.NORTH);
        categoryLabels = new JPanel();
        categoryLabels.setLayout(new BoxLayout(categoryLabels, BoxLayout.Y_AXIS));
        categoryLabels.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel top = new JPanel(new BorderLayout()); top.add(categoryLabels, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER); panel.add(content, BorderLayout.CENTER);
        refreshCategoryLabels();
        return panel;
    }

    private void refreshCategoryLabels() {
        if (categoryLabels == null) return;
        categoryLabels.removeAll();
        try {
            Set<Integer> duplicateIds = CategoryColorUniqueness.findDuplicateCategoryIds();
            duplicatesRemain = !duplicateIds.isEmpty();
            for (DatabaseManipulator.Category category : DatabaseManipulator.getCategories()) {
                categoryLabels.add(createCategoryRow(category, duplicateIds.contains(category.id())));
                categoryLabels.add(Box.createVerticalStrut(7));
            }
            if (forcedCategoryRepair && !duplicatesRemain) {
                setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                tabs.setEnabledAt(0, true);
            }
        } catch (SQLException exception) {
            duplicatesRemain = forcedCategoryRepair;
            JOptionPane.showMessageDialog(this, "Kategorie se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        categoryLabels.revalidate(); categoryLabels.repaint();
    }

    private JPanel createCategoryRow(DatabaseManipulator.Category category, boolean duplicate) {
        Color background = parseColor(category.color());
        double lum = .2126 * background.getRed() + .7152 * background.getGreen() + .0722 * background.getBlue();
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true); row.setBackground(background); row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        JLabel name = new JLabel(category.name()); name.setForeground(lum < 145 ? Color.WHITE : Color.BLACK);
        row.add(name, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); actions.setOpaque(false);
        if (duplicate) {
            JLabel warning = new JLabel("⚠");
            warning.setForeground(lum < 145 ? Color.WHITE : new Color(180, 45, 35));
            warning.setToolTipText("Tato barva je použita u více kategorií");
            warning.setFont(warning.getFont().deriveFont(Font.BOLD, 18f));
            actions.add(warning);
        }
        JButton edit = new JButton("✎"); edit.setToolTipText("Upravit kategorii"); edit.setFocusable(false);
        edit.addActionListener(e -> {
            EditCategoryDialog dialog = new EditCategoryDialog(this, category);
            dialog.setVisible(true);
            if (dialog.isSaved()) refreshCategoryLabels();
        });
        JButton delete = new JButton("🗑"); delete.setToolTipText("Odstranit kategorii"); delete.setFocusable(false);
        delete.addActionListener(e -> deleteCategory(category));
        actions.add(edit); actions.add(delete); row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void deleteCategory(DatabaseManipulator.Category category) {
        try {
            if (DatabaseManipulator.isCategoryUsed(category.id())) {
                JOptionPane.showMessageDialog(this, "Kategorii nelze odstranit, protože je použita u některého výdaje.",
                        "Kategorie je používána", JOptionPane.WARNING_MESSAGE); return;
            }
            int answer = JOptionPane.showOptionDialog(this, "Opravdu smazat kategorii?", "Potvrzení",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{"Ano", "Ne"}, "Ne");
            if (answer == JOptionPane.YES_OPTION) {
                DatabaseManipulator.deleteCategory(category.id()); refreshCategoryLabels();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Kategorii se nepodařilo odstranit.\n" + ex.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Color parseColor(String value) {
        try { return value != null && value.matches("#[0-9a-fA-F]{6}") ? Color.decode(value) : new Color(225,225,225); }
        catch (NumberFormatException ex) { return new Color(225,225,225); }
    }

    private static JPanel createTabPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JLabel heading = new JLabel(title); heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D()+4));
        panel.add(heading, BorderLayout.NORTH); return panel;
    }

    private void confirmAndClear(boolean expenses) {
        int answer = JOptionPane.showOptionDialog(this, "Opravdu vymazat?", "Potvrzení",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Ano","Ne"}, "Ne");
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            if (expenses) DatabaseManipulator.clearExpenses(); else DatabaseManipulator.clearCategories();
            refreshCategoryLabels();
            JOptionPane.showMessageDialog(this, "Tabulka byla vyčištěna.", "Hotovo", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Tabulku se nepodařilo vyčistit.\n" + ex.getMessage(), "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }
}
