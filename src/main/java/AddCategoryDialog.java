import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class AddCategoryDialog extends JDialog {
    private static final Pattern NAME_PATTERN = Pattern.compile("\\p{L}+(?: \\p{L}+)*");
    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final JTextField nameField = new JTextField(24);
    private final JTextField colorField = new JTextField("#000000", 10);
    private final JButton colorPickerButton = new JButton();

    public AddCategoryDialog(Window owner) {
        super(owner, "Přidat kategorii", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout(0, 12));

        add(createForm(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        updateColorPreview();
        pack();
        DialogSizePreferences.restoreAndTrack(this, "addCategory");
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Název:"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        form.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        form.add(new JLabel("Barva:"), constraints);

        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        colorPanel.add(colorField);

        colorPickerButton.setPreferredSize(new Dimension(34, 24));
        colorPickerButton.setToolTipText("Vybrat barvu");
        colorPickerButton.getAccessibleContext().setAccessibleName("Vybrat barvu");
        colorPickerButton.addActionListener(event -> chooseColor());
        colorPanel.add(colorPickerButton);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        form.add(colorPanel, constraints);

        colorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateColorPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateColorPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateColorPreview();
            }
        });

        return form;
    }

    private JPanel createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));

        JButton saveButton = new JButton("Uložit");
        saveButton.addActionListener(event -> saveCategory());
        getRootPane().setDefaultButton(saveButton);
        buttons.add(saveButton);
        return buttons;
    }

    private void chooseColor() {
        Color initialColor = parseColor(colorField.getText());
        if (initialColor == null) {
            initialColor = Color.BLACK;
        }

        Color selectedColor = JColorChooser.showDialog(this, "Vybrat barvu", initialColor);
        if (selectedColor != null) {
            colorField.setText(String.format("#%02X%02X%02X",
                    selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()));
        }
    }

    private void updateColorPreview() {
        Color color = parseColor(colorField.getText());
        colorPickerButton.setBackground(color != null ? color : getBackground());
        colorPickerButton.setOpaque(true);
    }

    private static Color parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!COLOR_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return Color.decode(normalized);
    }

    private void saveCategory() {
        String name = nameField.getText().trim();
        String color = colorField.getText().trim().toUpperCase();

        if (!NAME_PATTERN.matcher(name).matches()) {
            showWarning("Název nesmí být prázdný a smí obsahovat pouze písmena a mezery mezi slovy.", nameField);
            return;
        }

        if (!COLOR_PATTERN.matcher(color).matches()) {
            showWarning("Barva nesmí být prázdná a musí mít formát #RRGGBB, například #1A73E8.", colorField);
            return;
        }

        try {
            DatabaseManipulator.addCategory(name, color);
            dispose();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "Kategorii se nepodařilo uložit.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showWarning(String message, Component field) {
        JOptionPane.showMessageDialog(this, message, "Neplatná hodnota",
                JOptionPane.WARNING_MESSAGE);
        field.requestFocusInWindow();
    }
}
