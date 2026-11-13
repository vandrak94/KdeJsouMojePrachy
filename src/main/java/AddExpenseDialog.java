import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/** Dialog pro vložení nového výdaje. */
public final class AddExpenseDialog extends JDialog {
    private static final DateTimeFormatter DATE_BUTTON_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JButton dateButton = new JButton();
    private final JComboBox<CategoryItem> categoryCombo = new JComboBox<>();
    private final JTextField amountField = new JTextField(14);
    private final JTextField noteField = new JTextField(26);
    private LocalDate selectedDate = LocalDate.now();
    private boolean saved;

    public AddExpenseDialog(Window owner, LocalDate initialDate) {
        super(owner, "Vložit výdaj", ModalityType.APPLICATION_MODAL);
        selectedDate = initialDate == null || initialDate.isAfter(LocalDate.now())
                ? LocalDate.now() : initialDate;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 12));
        setResizable(true);

        dateButton.addActionListener(event -> chooseDate());
        updateDateButton();
        configureCategories();
        add(createForm(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        pack();
        DialogSizePreferences.restoreAndTrack(this, "addExpense");
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() { return saved; }

    private JPanel createForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addRow(panel, c, 0, "Datum:", dateButton);
        addRow(panel, c, 1, "Kategorie:", categoryCombo);
        addRow(panel, c, 2, "Částka:", amountField);
        addRow(panel, c, 3, "Poznámka:", noteField);
        return panel;
    }

    private static void addRow(JPanel panel, GridBagConstraints c, int row,
            String label, Component component) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(component, c);
    }

    private JPanel createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        JButton saveButton = new JButton("Uložit");
        saveButton.addActionListener(event -> saveExpense());
        getRootPane().setDefaultButton(saveButton);
        buttons.add(saveButton);
        return buttons;
    }

    private void configureCategories() {
        categoryCombo.addItem(null);
        try {
            List<DatabaseManipulator.Category> categories = DatabaseManipulator.getCategories();
            for (DatabaseManipulator.Category category : categories) {
                categoryCombo.addItem(new CategoryItem(category.id(), category.name(), category.color()));
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Kategorie se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focus);
                if (value instanceof CategoryItem item) {
                    label.setText(item.name());
                    if (!selected) {
                        Color background = parseColor(item.color());
                        label.setBackground(background);
                        label.setForeground(isDark(background) ? Color.WHITE : Color.BLACK);
                    }
                } else {
                    label.setText("");
                }
                return label;
            }
        });
    }

    private void chooseDate() {
        RangeSelectionDialog dialog = new RangeSelectionDialog(this, "Den", selectedDate);
        dialog.setVisible(true);
        if (dialog.getSelectedDate() != null) {
            selectedDate = dialog.getSelectedDate();
            updateDateButton();
        }
    }

    private void updateDateButton() {
        dateButton.setText(selectedDate.format(DATE_BUTTON_FORMAT));
    }

    private void saveExpense() {
        CategoryItem category = (CategoryItem) categoryCombo.getSelectedItem();
        if (category == null) {
            warning("Vyberte kategorii.", categoryCombo);
            return;
        }
        BigDecimal amount;
        try {
            String normalized = amountField.getText().trim().replace(',', '.');
            if (!normalized.matches("\\d+(?:[.,]\\d{1,2})?") && !amountField.getText().trim().matches("\\d+(?:,\\d{1,2})?")) {
                throw new NumberFormatException();
            }
            amount = new BigDecimal(normalized).setScale(2);
        } catch (Exception exception) {
            warning("Částka musí být číslo ve formátu 0,00.", amountField);
            return;
        }
        if (amount.signum() <= 0) {
            warning("Částka musí být větší než 0.", amountField);
            return;
        }

        try {
            DatabaseManipulator.addExpense(selectedDate.toString(), category.id(), amount, noteField.getText());
            saved = true;
            dispose();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Výdaj se nepodařilo uložit.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warning(String message, Component component) {
        JOptionPane.showMessageDialog(this, message, "Neplatná hodnota", JOptionPane.WARNING_MESSAGE);
        component.requestFocusInWindow();
    }

    private static Color parseColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}")
                    ? Color.decode(value) : Color.LIGHT_GRAY;
        } catch (NumberFormatException exception) {
            return Color.LIGHT_GRAY;
        }
    }

    private static boolean isDark(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue() < 145;
    }

    private record CategoryItem(int id, String name, String color) {
        @Override public String toString() { return name; }
    }
}
