import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Přehled výdajů v hlavním okně. */
public final class OverviewPanel extends JPanel {
    private static final DateTimeFormatter DATE_HEADING_FORMAT =
            DateTimeFormatter.ofPattern("d. M. uuuu");
    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("cs", "CZ"));

    private final JLabel heading = new JLabel();
    private final JLabel total = new JLabel();
    private final JLabel comparison = new JLabel();
    private final JPanel expenseList = new JPanel();
    private final PieChartPanel pieChart = new PieChartPanel();
    private final JButton addExpenseButton = new JButton("Vložit výdaj");
    private final JButton categoryFilter = new JButton("Vše ▾");
    private final JPopupMenu categoryPopup = new JPopupMenu();
    private final JTextField noteFilter = new JTextField(18);
    private final Set<Integer> selectedCategoryIds = new LinkedHashSet<>();
    private List<DatabaseManipulator.ExpenseOverview> currentExpenses = List.of();
    private boolean updatingFilter;
    private LocalDate previousStart;
    private LocalDate previousEnd;
    private String previousPeriod;
    private String currentRange = "Den";
    private LocalDate currentDate = LocalDate.now();

    public OverviewPanel() {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        total.setFont(total.getFont().deriveFont(Font.BOLD, 17f));
        comparison.setFont(comparison.getFont().deriveFont(Font.BOLD, 14f));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        total.setAlignmentX(LEFT_ALIGNMENT);
        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        header.add(total);
        header.add(Box.createVerticalStrut(3));
        comparison.setAlignmentX(LEFT_ALIGNMENT);
        header.add(comparison);
        add(header, BorderLayout.NORTH);

        expenseList.setLayout(new BoxLayout(expenseList, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(expenseList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        expenseList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel overviewContent = new JPanel(new BorderLayout(16, 0));
        overviewContent.add(scrollPane, BorderLayout.CENTER);
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        setupFilter();
        JPanel filterHeader = new JPanel(new BorderLayout(10, 0));
        JPanel categoryControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        categoryControls.add(new JLabel("Kategorie: "));
        categoryControls.add(categoryFilter);
        filterHeader.add(categoryControls, BorderLayout.WEST);
        JPanel noteContainer = new JPanel(new BorderLayout(0, 0));
        noteContainer.add(noteFilter, BorderLayout.CENTER);
        JButton clearNoteButton = new JButton("×");
        clearNoteButton.setToolTipText("Vymazat filtr poznámky");
        clearNoteButton.setFocusable(false);
        clearNoteButton.setMargin(new java.awt.Insets(0, 6, 0, 6));
        clearNoteButton.addActionListener(e -> noteFilter.setText(""));
        noteContainer.add(clearNoteButton, BorderLayout.EAST);
        JPanel noteControls = new JPanel(new BorderLayout(6, 0));
        noteControls.add(new JLabel("Poznámka:"), BorderLayout.WEST);
        noteControls.add(noteContainer, BorderLayout.CENTER);
        filterHeader.add(noteControls, BorderLayout.CENTER);
        noteFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { renderFilteredExpenses(); }
            public void removeUpdate(DocumentEvent e) { renderFilteredExpenses(); }
            public void changedUpdate(DocumentEvent e) { renderFilteredExpenses(); }
        });
        JPanel listContainer = new JPanel(new BorderLayout(0, 8));
        listContainer.add(filterHeader, BorderLayout.NORTH);
        listContainer.add(scrollPane, BorderLayout.CENTER);
        overviewContent.remove(scrollPane);
        overviewContent.add(listContainer, BorderLayout.CENTER);
        chartContainer.add(pieChart, BorderLayout.CENTER);
        overviewContent.add(chartContainer, BorderLayout.EAST);
        add(overviewContent, BorderLayout.CENTER);
        setFocusable(true);
        java.awt.event.MouseAdapter clearNoteFocus = new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent event) {
                requestFocusInWindow();
            }
        };
        addMouseListener(clearNoteFocus);
        header.addMouseListener(clearNoteFocus);
        overviewContent.addMouseListener(clearNoteFocus);
        listContainer.addMouseListener(clearNoteFocus);
        chartContainer.addMouseListener(clearNoteFocus);
        scrollPane.getViewport().addMouseListener(clearNoteFocus);

        addExpenseButton.setToolTipText("Vložit výdaj");
        addExpenseButton.setFocusable(false);
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        footer.add(addExpenseButton);
        add(footer, BorderLayout.SOUTH);
    }


    public void setAddExpenseAction(Runnable action) {
        for (java.awt.event.ActionListener listener : addExpenseButton.getActionListeners()) {
            addExpenseButton.removeActionListener(listener);
        }
        addExpenseButton.addActionListener(event -> action.run());
    }

    public void showPeriod(String range, LocalDate selectedDate) {
        currentRange = range;
        currentDate = selectedDate;
        LocalDate start;
        LocalDate end;
        String title;
        switch (range) {
            case "Týden" -> {
                start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
                title = "Týden " + start.format(DATE_HEADING_FORMAT)
                        + " – " + end.format(DATE_HEADING_FORMAT);
            }
            case "Měsíc" -> {
                start = selectedDate.withDayOfMonth(1);
                end = start.plusMonths(1).minusDays(1);
                title = MonthNameFormatter.format(selectedDate, "LLLL uuuu");
            }
            case "Rok" -> {
                start = selectedDate.withDayOfYear(1);
                end = start.plusYears(1).minusDays(1);
                title = Integer.toString(selectedDate.getYear());
            }
            default -> {
                start = selectedDate;
                end = selectedDate;
                title = selectedDate.format(DATE_HEADING_FORMAT);
            }
        }
        previousStart = switch (range) {
            case "Týden" -> start.minusWeeks(1);
            case "Měsíc" -> start.minusMonths(1);
            case "Rok" -> start.minusYears(1);
            default -> start.minusDays(1);
        };
        previousEnd = start.minusDays(1);
        previousPeriod = switch (range) {
            case "Týden" -> "předchozímu týdnu";
            case "Měsíc" -> "předchozímu měsíci";
            case "Rok" -> "předchozímu roku";
            default -> "předchozímu dni";
        };
        heading.setText(title);
        try {
            currentExpenses = DatabaseManipulator.getExpensesBetween(start, end);
            reloadFilterCategories();
            renderFilteredExpenses();
        } catch (Exception exception) {
            currentExpenses = List.of();
            comparison.setText("");
            total.setText("Výdaje se nepodařilo načíst.");
            pieChart.setExpenses(List.of());
            expenseList.removeAll();
            JOptionPane.showMessageDialog(this,
                    "Výdaje se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        expenseList.revalidate();
        expenseList.repaint();
    }

    private void setupFilter() {
        categoryFilter.setPreferredSize(new Dimension(170, 25));
        categoryFilter.setMargin(new java.awt.Insets(1, 7, 1, 7));
        categoryFilter.setFocusable(false);
        categoryFilter.addActionListener(e ->
                categoryPopup.show(categoryFilter, 0, categoryFilter.getHeight()));
        categoryPopup.setLayout(new BoxLayout(categoryPopup, BoxLayout.Y_AXIS));
    }

    private void reloadFilterCategories() throws java.sql.SQLException {
        List<DatabaseManipulator.Category> categories = DatabaseManipulator.getCategories();
        Set<Integer> validIds = new LinkedHashSet<>();
        for (DatabaseManipulator.Category category : categories) validIds.add(category.id());
        selectedCategoryIds.retainAll(validIds);
        categoryPopup.removeAll();

        JCheckBox all = new JCheckBox("Vše", selectedCategoryIds.isEmpty());
        all.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        all.addActionListener(e -> {
            selectedCategoryIds.clear();
            renderFilteredExpenses();
            categoryPopup.setVisible(false);
            reloadCheckboxStates();
        });
        categoryPopup.add(all);

        for (DatabaseManipulator.Category category : categories) {
            JCheckBox box = new JCheckBox(category.name(), selectedCategoryIds.contains(category.id()));
            box.putClientProperty("categoryId", category.id());
            box.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            box.addActionListener(e -> {
                if (!selectedCategoryIds.add(category.id())) selectedCategoryIds.remove(category.id());
                reloadCheckboxStates();
                renderFilteredExpenses();
                // JCheckBox in a popup is not a menu item: popup remains open.
            });
            categoryPopup.add(box);
        }
        reloadCheckboxStates();
    }

    private void reloadCheckboxStates() {
        for (Component component : categoryPopup.getComponents()) {
            if (component instanceof JCheckBox box) {
                Object id = box.getClientProperty("categoryId");
                box.setSelected(id == null ? selectedCategoryIds.isEmpty()
                        : selectedCategoryIds.contains((Integer) id));
            }
        }
        categoryFilter.setText(selectedCategoryIds.isEmpty()
                ? "Vše ▾" : "Vybráno: " + selectedCategoryIds.size() + " ▾");
    }

    private void renderFilteredExpenses() {
        String noteQuery = noteFilter.getText().trim().toLowerCase(Locale.ROOT);
        List<DatabaseManipulator.ExpenseOverview> filtered = currentExpenses.stream()
                .filter(e -> selectedCategoryIds.isEmpty()
                        || selectedCategoryIds.contains(e.categoryId()))
                .filter(e -> noteQuery.isEmpty() || (e.note() != null
                        && e.note().toLowerCase(Locale.ROOT).contains(noteQuery)))
                .toList();
        expenseList.removeAll();
        BigDecimal sum = BigDecimal.ZERO;
        for (DatabaseManipulator.ExpenseOverview expense : filtered) {
            if (expense.amount() != null) sum = sum.add(expense.amount());
            expenseList.add(createExpenseRow(expense));
            expenseList.add(Box.createVerticalStrut(7));
        }
        total.setText("Celkem: " + formatCurrency(sum));
        pieChart.setExpenses(filtered);
        try {
            List<DatabaseManipulator.ExpenseOverview> previous =
                    DatabaseManipulator.getExpensesBetween(previousStart, previousEnd);
            BigDecimal previousSum = BigDecimal.ZERO;
            for (DatabaseManipulator.ExpenseOverview expense : previous) {
                if ((selectedCategoryIds.isEmpty() || selectedCategoryIds.contains(expense.categoryId()))
                        && (noteQuery.isEmpty() || (expense.note() != null
                                && expense.note().toLowerCase(Locale.ROOT).contains(noteQuery)))
                        && expense.amount() != null) previousSum = previousSum.add(expense.amount());
            }
            updateComparison(sum, previousSum);
        } catch (Exception ex) {
            comparison.setText("Porovnání není dostupné");
        }
        if (filtered.isEmpty()) {
            expenseList.add(new JLabel("Pro vybrané období a kategorie nejsou evidovány žádné výdaje."));
        }
        expenseList.revalidate();
        expenseList.repaint();
    }

    private void updateComparison(BigDecimal current, BigDecimal previous) {
        BigDecimal difference = current.subtract(previous);
        int sign = difference.signum();
        if (sign == 0) {
            comparison.setForeground(new Color(108, 117, 125));
            comparison.setText("● Beze změny (0,00 %) oproti " + previousPeriod);
            return;
        }
        comparison.setForeground(sign > 0 ? new Color(220, 53, 69) : new Color(25, 135, 84));
        String text = (sign > 0 ? "▲ +" : "▼ -") + formatCurrency(difference.abs());
        if (previous.signum() != 0) {
            java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(new Locale("cs", "CZ"));
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            BigDecimal percent = difference.multiply(BigDecimal.valueOf(100))
                    .divide(previous.abs(), 2, java.math.RoundingMode.HALF_UP);
            text += " (" + (sign > 0 ? "+" : "") + format.format(percent) + " %)";
        }
        comparison.setText(text + " oproti " + previousPeriod);
    }

    private record FilterItem(int id, String name, boolean all) {
        @Override public String toString() { return name; }
    }

    private JPanel createExpenseRow(DatabaseManipulator.ExpenseOverview expense) {
        Color background = parseColor(expense.categoryColor());
        Color foreground = isDark(background) ? Color.WHITE : Color.BLACK;

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.setBackground(background);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel date = createLabel(formatExpenseDate(expense.date()), SwingConstants.LEFT, foreground);
        JLabel category = createLabel(expense.categoryName(), SwingConstants.LEFT, foreground);
        JLabel amount = createLabel(formatCurrency(expense.amount()), SwingConstants.RIGHT, foreground);
        amount.setFont(amount.getFont().deriveFont(Font.BOLD));
        String tooltip = expense.note();
        if (tooltip != null && !tooltip.isBlank()) {
            String safe = tooltip.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;")
                    .replace("\n", "<br>");
            tooltip = "<html>" + safe + "</html>";
        }
        row.setToolTipText(tooltip);
        date.setToolTipText(tooltip);
        category.setToolTipText(tooltip);
        amount.setToolTipText(tooltip);
        JPanel values = new JPanel(new GridLayout(1, 3, 12, 0));
        values.setOpaque(false);
        values.add(date);
        values.add(category);
        values.add(amount);
        row.add(values, BorderLayout.CENTER);

        JButton editButton = new JButton("✎");
        editButton.setToolTipText("Upravit výdaj");
        editButton.setFocusable(false);
        editButton.addActionListener(event -> editExpense(expense));
        JButton deleteButton = new JButton("🗑");
        deleteButton.setToolTipText("Smazat výdaj");
        deleteButton.setFocusable(false);
        deleteButton.addActionListener(event -> deleteExpense(expense));
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(deleteButton);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void editExpense(DatabaseManipulator.ExpenseOverview expense) {
        EditExpenseDialog dialog = new EditExpenseDialog(
                SwingUtilities.getWindowAncestor(this), expense);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            showPeriod(currentRange, currentDate);
        }
    }

    private void deleteExpense(DatabaseManipulator.ExpenseOverview expense) {
        Object[] options = {"Ano", "Ne"};
        int answer = JOptionPane.showOptionDialog(this,
                "Opravdu smazat výdaj?", "Potvrzení",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            DatabaseManipulator.deleteExpense(expense.id());
            showPeriod(currentRange, currentDate);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Výdaj se nepodařilo smazat.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatExpenseDate(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            // The database can contain both yyyy-MM-dd and legacy datetime values.
            return LocalDate.parse(value.substring(0, 10))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (RuntimeException exception) {
            return value;
        }
    }

    private static JLabel createLabel(String text, int alignment, Color foreground) {
        JLabel label = new JLabel(text == null ? "" : text, alignment);
        label.setForeground(foreground);
        return label;
    }

    private static String formatCurrency(BigDecimal amount) {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        return CURRENCY_FORMAT.format(amount == null ? BigDecimal.ZERO : amount) + " Kč";
    }

    private static Color parseColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}")
                    ? Color.decode(value) : new Color(225, 225, 225);
        } catch (NumberFormatException exception) {
            return new Color(225, 225, 225);
        }
    }

    private static boolean isDark(Color color) {
        double luminance = 0.2126 * color.getRed()
                + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
        return luminance < 145;
    }
}
