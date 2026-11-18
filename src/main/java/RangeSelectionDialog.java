import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/** Kalendářový výběr období omezený na 17. 11. 1989 až současnost. */
public final class RangeSelectionDialog extends JDialog {
    public static final LocalDate MINIMUM_DATE = LocalDate.of(1989, 11, 17);
    private static final Locale CS = Locale.forLanguageTag("cs-CZ");

    private final String range;
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel();
    private final JButton previous = new JButton("◀");
    private final JButton next = new JButton("▶");
    private final JComboBox<String> monthSelector = new JComboBox<>();
    private final JComboBox<Integer> yearSelector = new JComboBox<>();
    private boolean updatingSelectors;
    private LocalDate view;
    private LocalDate selectedDate;

    public RangeSelectionDialog(Window owner, String range, LocalDate initialDate) {
        super(owner, "Vybrat období", ModalityType.APPLICATION_MODAL);
        this.range = range;
        LocalDate validInitial = initialDate == null ? LocalDate.now() : initialDate;
        if (validInitial.isAfter(LocalDate.now())) validInitial = LocalDate.now();
        if (validInitial.isBefore(MINIMUM_DATE)) validInitial = MINIMUM_DATE;
        this.view = validInitial;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 10));
        JPanel navigation = new JPanel(new BorderLayout(8, 0));
        previous.addActionListener(e -> navigate(-1));
        next.addActionListener(e -> navigate(1));
        navigation.add(previous, BorderLayout.WEST);
        {
            JPanel selectors = new JPanel();
            selectors.setLayout(new BoxLayout(selectors, BoxLayout.X_AXIS));
            String[] monthNames = new String[12];
            for (int i = 1; i <= 12; i++) {
                monthNames[i - 1] = LocalDate.of(2000, i, 1)
                        .format(DateTimeFormatter.ofPattern("LLLL", CS));
                monthNames[i - 1] = MonthNameFormatter.format(LocalDate.of(2000, i, 1), "LLLL");
            }
            monthSelector.setModel(new DefaultComboBoxModel<>(monthNames));
            for (int year = MINIMUM_DATE.getYear(); year <= LocalDate.now().getYear(); year++) {
                yearSelector.addItem(year);
            }
            monthSelector.setToolTipText("Vybrat měsíc");
            yearSelector.setToolTipText("Vybrat rok");
            monthSelector.addActionListener(e -> selectMonthAndYear());
            yearSelector.addActionListener(e -> selectMonthAndYear());
            if ("Den".equals(range) || "Týden".equals(range)) selectors.add(monthSelector);
            if (!"Rok".equals(range)) selectors.add(yearSelector);
            if ("Rok".equals(range)) {
                navigation.add(title, BorderLayout.CENTER);
            } else {
                navigation.add(selectors, BorderLayout.CENTER);
            }
        }
        navigation.add(next, BorderLayout.EAST);
        navigation.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(navigation, BorderLayout.NORTH);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        add(grid, BorderLayout.CENTER);
        refresh();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    public LocalDate getSelectedDate() { return selectedDate; }

    private void navigate(int step) {
        LocalDate candidate = "Rok".equals(range) ? view.plusYears(step * 12L)
                : "Měsíc".equals(range) ? view.plusYears(step) : view.plusMonths(step);
        if (canNavigateTo(candidate)) {
            view = candidate;
            refresh();
        }
    }

    private boolean canNavigateTo(LocalDate candidate) {
        LocalDate today = LocalDate.now();
        if ("Rok".equals(range)) {
            int blockStart = Math.floorDiv(candidate.getYear(), 12) * 12;
            return blockStart + 11 >= MINIMUM_DATE.getYear() && blockStart <= today.getYear();
        }
        if ("Měsíc".equals(range)) {
            return candidate.getYear() >= MINIMUM_DATE.getYear()
                    && candidate.getYear() <= today.getYear();
        }
        YearMonth month = YearMonth.from(candidate);
        return !month.isBefore(YearMonth.from(MINIMUM_DATE))
                && !month.isAfter(YearMonth.from(today));
    }

    private void selectMonthAndYear() {
        if (updatingSelectors || yearSelector.getSelectedItem() == null) return;
        int year = (Integer) yearSelector.getSelectedItem();
        int month = "Rok".equals(range) ? view.getMonthValue()
                : monthSelector.getSelectedIndex() + 1;
        YearMonth requested = YearMonth.of(year, month);
        YearMonth min = YearMonth.from(MINIMUM_DATE);
        YearMonth max = YearMonth.from(LocalDate.now());
        if (requested.isBefore(min)) requested = min;
        if (requested.isAfter(max)) requested = max;
        view = requested.atDay(Math.min(view.getDayOfMonth(), requested.lengthOfMonth()));
        refresh();
    }

    private void syncSelectors() {
        updatingSelectors = true;
        try {
            yearSelector.setSelectedItem(view.getYear());
            if (!"Rok".equals(range)) monthSelector.setSelectedIndex(view.getMonthValue() - 1);
        } finally {
            updatingSelectors = false;
        }
    }

    private void refresh() {
        grid.removeAll();
        syncSelectors();
        LocalDate today = LocalDate.now();
        if ("Den".equals(range) || "Týden".equals(range)) {
            YearMonth month = YearMonth.from(view);
            title.setText(MonthNameFormatter.format(month.atDay(1), "LLLL yyyy"));
            grid.setLayout(new GridLayout(0, 7, 4, 4));
            for (String day : new String[] {"Po", "Út", "St", "Čt", "Pá", "So", "Ne"})
                grid.add(new JLabel(day, SwingConstants.CENTER));
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) grid.add(new JLabel());
            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                boolean valid = !date.isBefore(MINIMUM_DATE) && !date.isAfter(today);
                addChoice(Integer.toString(day), date, valid);
            }
        } else if ("Měsíc".equals(range)) {
            title.setText(Integer.toString(view.getYear()));
            grid.setLayout(new GridLayout(3, 4, 6, 6));
            for (int month = 1; month <= 12; month++) {
                LocalDate date = LocalDate.of(view.getYear(), month, 1);
                boolean valid = !YearMonth.from(date).isBefore(YearMonth.from(MINIMUM_DATE))
                        && !YearMonth.from(date).isAfter(YearMonth.from(today));
                addChoice(MonthNameFormatter.format(date, "MMM"), date, valid);
            }
        } else {
            int start = Math.floorDiv(view.getYear(), 12) * 12;
            title.setText(start + " – " + (start + 11));
            grid.setLayout(new GridLayout(3, 4, 6, 6));
            for (int year = start; year < start + 12; year++) {
                LocalDate date = LocalDate.of(year, 1, 1);
                addChoice(Integer.toString(year), date,
                        year >= MINIMUM_DATE.getYear() && year <= today.getYear());
            }
        }
        updateNavigationVisibility();
        grid.revalidate();
        grid.repaint();
        pack();
    }

    private void updateNavigationVisibility() {
        LocalDate previousView = "Rok".equals(range) ? view.minusYears(12)
                : "Měsíc".equals(range) ? view.minusYears(1) : view.minusMonths(1);
        LocalDate nextView = "Rok".equals(range) ? view.plusYears(12)
                : "Měsíc".equals(range) ? view.plusYears(1) : view.plusMonths(1);
        previous.setEnabled(canNavigateTo(previousView));
        next.setEnabled(canNavigateTo(nextView));
    }

    private void addChoice(String label, LocalDate date, boolean enabled) {
        JButton button = new JButton(label);
        button.setEnabled(enabled);
        LocalDate normalized = normalize(date);
        if (normalized.equals(normalize(view))) {
            button.setBackground(UIManager.getColor("List.selectionBackground"));
            button.setForeground(UIManager.getColor("List.selectionForeground"));
        }
        button.addActionListener(e -> {
            selectedDate = normalized;
            dispose();
        });
        grid.add(button);
    }

    private static String capitalizeMonth(String text) {
        if (text == null || text.isEmpty()) return text;
        int first = text.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toTitleCase(first))
                .append(text.substring(Character.charCount(first))).toString();
    }

    private LocalDate normalize(LocalDate date) {
        return switch (range) {
            case "Týden" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "Měsíc" -> date.withDayOfMonth(1);
            case "Rok" -> date.withDayOfYear(1);
            default -> date;
        };
    }
}
