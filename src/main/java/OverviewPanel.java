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
import java.util.List;
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
    private final JPanel expenseList = new JPanel();
    private final PieChartPanel pieChart = new PieChartPanel();
    private final JButton addExpenseButton = new JButton("Vložit výdaj");
    private String currentRange = "Den";
    private LocalDate currentDate = LocalDate.now();

    public OverviewPanel() {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        total.setFont(total.getFont().deriveFont(Font.BOLD, 17f));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        total.setAlignmentX(LEFT_ALIGNMENT);
        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        header.add(total);
        add(header, BorderLayout.NORTH);

        expenseList.setLayout(new BoxLayout(expenseList, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(expenseList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel overviewContent = new JPanel(new BorderLayout(16, 0));
        overviewContent.add(scrollPane, BorderLayout.CENTER);
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        chartContainer.add(pieChart, BorderLayout.CENTER);
        overviewContent.add(chartContainer, BorderLayout.EAST);
        add(overviewContent, BorderLayout.CENTER);

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
                title = selectedDate.format(DateTimeFormatter.ofPattern("LLLL uuuu", new Locale("cs", "CZ")));
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
        heading.setText(title);
        expenseList.removeAll();
        try {
            List<DatabaseManipulator.ExpenseOverview> expenses =
                    DatabaseManipulator.getExpensesBetween(start, end);
            pieChart.setExpenses(expenses);
            BigDecimal sum = BigDecimal.ZERO;
            for (DatabaseManipulator.ExpenseOverview expense : expenses) {
                if (expense.amount() != null) sum = sum.add(expense.amount());
                expenseList.add(createExpenseRow(expense));
                expenseList.add(Box.createVerticalStrut(7));
            }
            total.setText("Celkem: " + formatCurrency(sum));
            if (expenses.isEmpty()) {
                JLabel empty = new JLabel("Pro vybrané období nejsou evidovány žádné výdaje.");
                empty.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
                expenseList.add(empty);
            }
        } catch (Exception exception) {
            pieChart.setExpenses(List.of());
            total.setText("Výdaje se nepodařilo načíst.");
            JOptionPane.showMessageDialog(this,
                    "Výdaje se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        expenseList.revalidate();
        expenseList.repaint();
    }

    private JPanel createExpenseRow(DatabaseManipulator.ExpenseOverview expense) {
        Color background = parseColor(expense.categoryColor());
        Color foreground = isDark(background) ? Color.WHITE : Color.BLACK;

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.setBackground(background);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

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
