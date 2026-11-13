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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Calendar-style period picker; no third-party dependencies. */
public final class RangeSelectionDialog extends JDialog {
    private static final Locale CS = Locale.forLanguageTag("cs-CZ");
    private final String range;
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel();
    private final JButton next = new JButton("▶");
    private LocalDate view;
    private LocalDate selectedDate;

    public RangeSelectionDialog(Window owner, String range, LocalDate initialDate) {
        super(owner, "Vybrat období", ModalityType.APPLICATION_MODAL);
        this.range = range;
        this.view = initialDate.isAfter(LocalDate.now()) ? LocalDate.now() : initialDate;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 10));

        JPanel navigation = new JPanel(new BorderLayout(8, 0));
        JButton previous = new JButton("◀");
        previous.addActionListener(e -> navigate(-1));
        next.addActionListener(e -> navigate(1));
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(title, BorderLayout.CENTER);
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
        view = "Rok".equals(range) ? view.plusYears(step * 12L)
                : "Měsíc".equals(range) ? view.plusYears(step) : view.plusMonths(step);
        refresh();
    }

    private void refresh() {
        grid.removeAll();
        LocalDate today = LocalDate.now();
        if ("Den".equals(range) || "Týden".equals(range)) {
            YearMonth month = YearMonth.from(view);
            title.setText(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", CS)));
            grid.setLayout(new GridLayout(0, 7, 4, 4));
            for (String day : new String[] {"Po", "Út", "St", "Čt", "Pá", "So", "Ne"}) {
                grid.add(new JLabel(day, SwingConstants.CENTER));
            }
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) grid.add(new JLabel());
            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                addChoice(Integer.toString(day), date, !date.isAfter(today));
            }
            next.setEnabled(month.isBefore(YearMonth.from(today)));
        } else if ("Měsíc".equals(range)) {
            title.setText(Integer.toString(view.getYear()));
            grid.setLayout(new GridLayout(3, 4, 6, 6));
            for (int month = 1; month <= 12; month++) {
                LocalDate date = LocalDate.of(view.getYear(), month, 1);
                addChoice(date.format(DateTimeFormatter.ofPattern("MMM", CS)), date,
                        !YearMonth.from(date).isAfter(YearMonth.from(today)));
            }
            next.setEnabled(view.getYear() < today.getYear());
        } else {
            int start = Math.floorDiv(view.getYear(), 12) * 12;
            title.setText(start + " – " + (start + 11));
            grid.setLayout(new GridLayout(3, 4, 6, 6));
            for (int year = start; year < start + 12; year++) {
                LocalDate date = LocalDate.of(year, 1, 1);
                addChoice(Integer.toString(year), date, year <= today.getYear());
            }
            next.setEnabled(start + 12 <= today.getYear());
        }
        grid.revalidate();
        grid.repaint();
    }

    private void addChoice(String label, LocalDate date, boolean enabled) {
        JButton button = new JButton(label);
        button.setEnabled(enabled);
        LocalDate normalized = normalize(date);
        if (normalized.equals(normalize(view))) {
            button.setBackground(new Color(191, 219, 254));
        }
        button.addActionListener(e -> {
            selectedDate = normalized;
            dispose();
        });
        grid.add(button);
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
