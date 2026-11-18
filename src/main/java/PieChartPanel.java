import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JPanel;

/** Koláčový graf výdajů s legendou: kategorie, procenta a součet částek. */
public final class PieChartPanel extends JPanel {
    public record Slice(String name, Color color, BigDecimal amount) { }

    private static final Locale CZECH = Locale.forLanguageTag("cs-CZ");
    private List<Slice> slices = List.of();

    public PieChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(340, 360));
        setMinimumSize(new Dimension(260, 260));
    }

    public void setExpenses(List<DatabaseManipulator.ExpenseOverview> expenses) {
        Map<String, Slice> aggregated = new LinkedHashMap<>();
        for (DatabaseManipulator.ExpenseOverview expense : expenses) {
            String name = expense.categoryName() == null || expense.categoryName().isBlank()
                    ? "Bez kategorie" : expense.categoryName();
            Color color = parseColor(expense.categoryColor());
            BigDecimal amount = expense.amount() == null ? BigDecimal.ZERO : expense.amount();
            Slice previous = aggregated.get(name);
            aggregated.put(name, new Slice(name, color,
                    previous == null ? amount : previous.amount().add(amount)));
        }
        slices = new ArrayList<>(aggregated.values());
        slices.sort(Comparator.comparing(Slice::amount).reversed()
                .thenComparing(Slice::name, String.CASE_INSENSITIVE_ORDER));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int legendEntries = (int) slices.stream().filter(s -> s.amount().signum() > 0).count();
            int legendHeight = legendEntries * 23 + 20;
            int diameter = Math.max(60, Math.min(width - 44, getHeight() - legendHeight - 20));
            int x = Math.max(12, (width - diameter) / 2);
            int y = 16;
            BigDecimal total = slices.stream().map(Slice::amount)
                    .filter(value -> value.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.signum() == 0) {
                g.setColor(Color.GRAY);
                String message = "Žádná data pro graf";
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(message, (width - metrics.stringWidth(message)) / 2, y + diameter / 2);
                return;
            }

            double angle = 90.0;
            for (Slice slice : slices) {
                if (slice.amount().signum() <= 0) continue;
                double extent = slice.amount().divide(total, 12, RoundingMode.HALF_UP)
                        .doubleValue() * -360.0;
                g.setColor(slice.color());
                g.fill(new Arc2D.Double(x, y, diameter, diameter, angle, extent, Arc2D.PIE));
                g.setColor(new Color(255, 255, 255, 180));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(x, y, diameter, diameter, angle, extent, Arc2D.PIE));
                angle += extent;
            }

            int legendY = y + diameter + 18;
            FontMetrics metrics = g.getFontMetrics();
            for (Slice slice : slices) {
                if (slice.amount().signum() <= 0) continue;
                g.setColor(slice.color());
                g.fillRoundRect(12, legendY - 10, 14, 14, 4, 4);
                g.setColor(getForeground());
                double percentage = slice.amount().divide(total, 4, RoundingMode.HALF_UP)
                        .doubleValue() * 100.0;
                String text = String.format(CZECH, "%s (%.1f %%) – %s",
                        slice.name(), percentage, formatCurrency(slice.amount()));
                g.drawString(ellipsize(text, metrics, width - 48), 34, legendY + 1);
                legendY += 23;
            }
        } finally {
            g.dispose();
        }
    }

    private static String formatCurrency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(CZECH);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " Kč";
    }

    private static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String value = text;
        while (!value.isEmpty() && metrics.stringWidth(value + "…") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "…";
    }

    private static Color parseColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}")
                    ? Color.decode(value) : new Color(160, 160, 160);
        } catch (NumberFormatException exception) {
            return new Color(160, 160, 160);
        }
    }
}
