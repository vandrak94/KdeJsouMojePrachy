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
    private final JTextField colorField = new JTextField(10);
    private final JButton colorPickerButton = new JButton();
    private final CategoryColorPalette palette;

    public AddCategoryDialog(Window owner) {
        super(owner, "Přidat kategorii", ModalityType.APPLICATION_MODAL);
        palette = new CategoryColorPalette(colorField::setText, null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout(0, 10));
        add(createForm(), BorderLayout.NORTH);
        add(palette, BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);
        updateColorPreview();
        pack();
        setMinimumSize(new Dimension(Math.max(700, getWidth()), Math.max(430, getHeight())));
        DialogSizePreferences.restoreAndTrack(this, "addCategory");
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Název:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; form.add(nameField, c);
        c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0; form.add(new JLabel("Barva:"), c);
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        colorPanel.add(colorField);
        colorPickerButton.setPreferredSize(new Dimension(34, 24));
        colorPickerButton.setToolTipText("Vybrat libovolnou barvu");
        colorPickerButton.addActionListener(event -> chooseColor());
        colorPanel.add(colorPickerButton);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; form.add(colorPanel, c);
        colorField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateColorPreview(); }
            public void removeUpdate(DocumentEvent e) { updateColorPreview(); }
            public void changedUpdate(DocumentEvent e) { updateColorPreview(); }
        });
        return form;
    }

    private JPanel createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        JButton save = new JButton("Uložit");
        save.addActionListener(event -> saveCategory());
        getRootPane().setDefaultButton(save);
        buttons.add(save);
        return buttons;
    }

    private void chooseColor() {
        Color initial = parseColor(colorField.getText());
        Color selected = JColorChooser.showDialog(this, "Vybrat barvu", initial != null ? initial : Color.BLACK);
        if (selected != null) colorField.setText(String.format("#%02X%02X%02X", selected.getRed(), selected.getGreen(), selected.getBlue()));
    }

    private void updateColorPreview() {
        Color color = parseColor(colorField.getText());
        colorPickerButton.setBackground(color != null ? color : getBackground());
        colorPickerButton.setOpaque(true);
    }

    private static Color parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        return COLOR_PATTERN.matcher(normalized).matches() ? Color.decode(normalized) : null;
    }

    private void saveCategory() {
        String name = nameField.getText().trim();
        String color = colorField.getText().trim().toUpperCase();
        if (!NAME_PATTERN.matcher(name).matches()) {
            warning("Název smí obsahovat pouze písmena a mezery mezi slovy.", nameField); return;
        }
        if (!COLOR_PATTERN.matcher(color).matches()) {
            warning("Barva musí mít formát #RRGGBB, například #FF8A80.", colorField); return;
        }
        if (palette.isUsed(color)) {
            warning("Tato barva je již použita jinou kategorií. Vyberte jinou barvu.", colorField); return;
        }
        try {
            DatabaseManipulator.addCategory(name, color);
            dispose();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Kategorii se nepodařilo uložit.\n" + exception.getMessage(), "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warning(String message, Component component) {
        JOptionPane.showMessageDialog(this, message, "Neplatná hodnota", JOptionPane.WARNING_MESSAGE);
        component.requestFocusInWindow();
    }
}
