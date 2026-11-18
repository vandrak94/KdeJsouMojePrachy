import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Hlavní okno aplikace včetně navigace mezi obdobími. */
public final class MainFrame extends JFrame {
    private static final String APP_TITLE = "Kde jsou moje prachy?";
    private static final LocalDate MINIMUM_DATE = LocalDate.of(1989, 11, 17);

    private final OverviewPanel overview = new OverviewPanel();
    private final JComboBox<String> rangeComboBox = new JComboBox<>(
            new String[] {"Den", "Týden", "Měsíc", "Rok"});
    private final JButton previousPeriodButton = new JButton("◀");
    private final JButton calendarButton = new JButton();
    private final JButton nextPeriodButton = new JButton("▶");
    private final JButton todayButton = new JButton("📅");
    private LocalDate selectedDate = LocalDate.now();

    public MainFrame() {
        super(APP_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setLayout(new BorderLayout());
        applyApplicationIcon();

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel rangeLabel = new JLabel("Rozsah:");
        rangeComboBox.setFocusable(false);
        rangeComboBox.setPreferredSize(new Dimension(105, 30));
        rangeComboBox.addActionListener(event -> rangeChanged());

        configurePeriodButton(previousPeriodButton, "Předchozí období", -1);
        configurePeriodButton(nextPeriodButton, "Následující období", 1);
        todayButton.setToolTipText("Přejít na aktuální období");
        todayButton.getAccessibleContext().setAccessibleName("Přejít na aktuální období");
        todayButton.setPreferredSize(new Dimension(40, 30));
        todayButton.setFocusable(false);
        todayButton.addActionListener(event -> {
            selectedDate = LocalDate.now();
            refreshOverview();
        });

        calendarButton.setToolTipText("Vybrat období z kalendáře");
        calendarButton.setPreferredSize(new Dimension(185, 30));
        calendarButton.setFocusable(false);
        calendarButton.addActionListener(event -> choosePeriod());

        JButton settingsButton = new JButton("⚙");
        settingsButton.setToolTipText("Nastavení");
        settingsButton.setFont(findSymbolFont(17));
        settingsButton.setHorizontalAlignment(SwingConstants.CENTER);
        settingsButton.setMargin(new Insets(2, 8, 2, 8));
        settingsButton.setFocusable(false);
        settingsButton.getAccessibleContext().setAccessibleName("Nastavení");
        settingsButton.addActionListener(event -> openSettings());
        settingsButton.setPreferredSize(new Dimension(40, 30));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftControls.add(rangeLabel);
        leftControls.add(rangeComboBox);
        leftControls.add(previousPeriodButton);
        leftControls.add(calendarButton);
        leftControls.add(nextPeriodButton);
        leftControls.add(todayButton);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightControls.add(settingsButton);
        toolbar.add(leftControls, BorderLayout.WEST);
        toolbar.add(rightControls, BorderLayout.EAST);

        overview.setAddExpenseAction(this::addExpense);
        refreshOverview();
        add(toolbar, BorderLayout.NORTH);
        add(overview, BorderLayout.CENTER);
        setSize(1100, 720);
        setLocationRelativeTo(null);
    }

    private void configurePeriodButton(JButton button, String tooltip, int direction) {
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(40, 30));
        button.setFocusable(false);
        button.addActionListener(event -> movePeriod(direction));
    }

    private void movePeriod(int direction) {
        LocalDate candidate = shiftPeriod(selectedDate, direction);
        if (direction < 0 && isBeforeMinimumPeriod(candidate)) return;
        if (direction > 0 && isFuturePeriod(candidate)) return;
        selectedDate = normalizeForRange(candidate);
        refreshOverview();
    }

    private LocalDate shiftPeriod(LocalDate date, int direction) {
        return switch (selectedRange()) {
            case "Týden" -> date.plusWeeks(direction);
            case "Měsíc" -> date.plusMonths(direction);
            case "Rok" -> date.plusYears(direction);
            default -> date.plusDays(direction);
        };
    }

    private boolean isFuturePeriod(LocalDate date) {
        LocalDate today = LocalDate.now();
        return switch (selectedRange()) {
            case "Týden" -> startOfWeek(date).isAfter(startOfWeek(today));
            case "Měsíc" -> YearMonth.from(date).isAfter(YearMonth.from(today));
            case "Rok" -> date.getYear() > today.getYear();
            default -> date.isAfter(today);
        };
    }

    private boolean isBeforeMinimumPeriod(LocalDate date) {
        return switch (selectedRange()) {
            case "Týden" -> startOfWeek(date).plusDays(6).isBefore(MINIMUM_DATE);
            case "Měsíc" -> YearMonth.from(date).isBefore(YearMonth.from(MINIMUM_DATE));
            case "Rok" -> date.getYear() < MINIMUM_DATE.getYear();
            default -> date.isBefore(MINIMUM_DATE);
        };
    }

    private LocalDate normalizeForRange(LocalDate date) {
        return switch (selectedRange()) {
            case "Týden" -> startOfWeek(date);
            case "Měsíc" -> date.withDayOfMonth(1);
            case "Rok" -> date.withDayOfYear(1);
            default -> date;
        };
    }

    private static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void updateNavigationButtons() {
        LocalDate previous = shiftPeriod(selectedDate, -1);
        LocalDate next = shiftPeriod(selectedDate, 1);
        previousPeriodButton.setEnabled(!isBeforeMinimumPeriod(previous));
        nextPeriodButton.setEnabled(!isFuturePeriod(next));
        todayButton.setEnabled(!normalizeForRange(selectedDate)
                .equals(normalizeForRange(LocalDate.now())));
    }

    private void refreshOverview() {
        selectedDate = normalizeForRange(selectedDate);
        updateCalendarButton();
        updateNavigationButtons();
        overview.showPeriod(selectedRange(), selectedDate);
    }

    private String selectedRange() {
        return (String) rangeComboBox.getSelectedItem();
    }

    private void applyApplicationIcon() {
        URL resource = MainFrame.class.getResource("/app-icon.png");
        if (resource != null) {
            Image image = new ImageIcon(resource).getImage();
            setIconImage(image);
        }
    }

    private void openSettings() {
        new SettingsDialog(this).setVisible(true);
        refreshOverview();
    }

    private void addExpense() {
        AddExpenseDialog dialog = new AddExpenseDialog(this,
                "Den".equals(selectedRange()) ? selectedDate : LocalDate.now());
        dialog.setVisible(true);
        if (dialog.isSaved()) refreshOverview();
    }

    private void rangeChanged() {
        selectedDate = LocalDate.now();
        refreshOverview();
    }

    private void choosePeriod() {
        RangeSelectionDialog dialog = new RangeSelectionDialog(this, selectedRange(), selectedDate);
        dialog.setVisible(true);
        if (dialog.getSelectedDate() != null) {
            selectedDate = dialog.getSelectedDate();
            refreshOverview();
        }
    }

    private void updateCalendarButton() {
        String text = switch (selectedRange()) {
            case "Týden" -> {
                LocalDate monday = startOfWeek(selectedDate);
                yield monday.format(DateTimeFormatter.ofPattern("dd.MM.")) + " – "
                        + monday.plusDays(6).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
            case "Měsíc" -> selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            case "Rok" -> Integer.toString(selectedDate.getYear());
            default -> selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        };
        calendarButton.setText("▦ " + text);
    }

    private static Font findSymbolFont(int size) {
        for (String name : new String[] {"Segoe UI Symbol", "Segoe UI Emoji", "Dialog"}) {
            Font font = new Font(name, Font.PLAIN, size);
            if (font.canDisplay('⚙')) return font;
        }
        return UIManager.getFont("Button.font").deriveFont((float) size);
    }
}
