import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
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

/** Hlavní okno aplikace včetně jeho ovládacích prvků a akcí. */
public final class MainFrame extends JFrame {
    private static final String APP_TITLE = "Kde jsou moje prachy?";
    private final OverviewPanel overview = new OverviewPanel();
    private final JComboBox<String> rangeComboBox = new JComboBox<>(
            new String[] {"Den", "Týden", "Měsíc", "Rok"});
    private LocalDate selectedDate = LocalDate.now();
    private final JButton calendarButton = new JButton();

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
        updateCalendarButton();
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
        leftControls.add(calendarButton);
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightControls.add(settingsButton);
        toolbar.add(leftControls, BorderLayout.WEST);
        toolbar.add(rightControls, BorderLayout.EAST);

        overview.setAddExpenseAction(this::addExpense);
        overview.showPeriod("Den", selectedDate);
        add(toolbar, BorderLayout.NORTH);
        add(overview, BorderLayout.CENTER);
        setSize(1100, 720);
        setLocationRelativeTo(null);
    }

    private void applyApplicationIcon() {
        URL resource = MainFrame.class.getResource("/app-icon.png");
        if (resource != null) {
            Image image = new ImageIcon(resource).getImage();
            setIconImage(image);
        }
    }

    private void openSettings() {
        // SettingsDialog is modal: execution resumes after the dialog closes.
        new SettingsDialog(this).setVisible(true);
        // Reload category names, colors, totals, and the chart for the same period.
        overview.showPeriod((String) rangeComboBox.getSelectedItem(), selectedDate);
    }

    private void addExpense() {
        AddExpenseDialog dialog = new AddExpenseDialog(this,
                "Den".equals(rangeComboBox.getSelectedItem()) ? selectedDate : LocalDate.now());
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            overview.showPeriod((String) rangeComboBox.getSelectedItem(), selectedDate);
        }
    }

    private void rangeChanged() {
        selectedDate = LocalDate.now();
        updateCalendarButton();
        overview.showPeriod((String) rangeComboBox.getSelectedItem(), selectedDate);
    }

    private void choosePeriod() {
        String range = (String) rangeComboBox.getSelectedItem();
        RangeSelectionDialog dialog = new RangeSelectionDialog(this, range, selectedDate);
        dialog.setVisible(true);
        if (dialog.getSelectedDate() != null) {
            selectedDate = dialog.getSelectedDate();
            updateCalendarButton();
            overview.showPeriod(range, selectedDate);
        }
    }

    private void updateCalendarButton() {
        String range = (String) rangeComboBox.getSelectedItem();
        String text = switch (range) {
            case "Týden" -> {
                LocalDate monday = selectedDate.with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
