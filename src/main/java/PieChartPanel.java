import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.ToolTipManager;

/** Interaktivní koláčový graf: tooltip pouze nad výsečí, zvýraznění také z legendy. */
public final class PieChartPanel extends JPanel {
    public record Slice(String name, Color color, BigDecimal amount) { }
    private static final Locale CZECH = Locale.forLanguageTag("cs-CZ");
    private static final int EXPLODE_DISTANCE = 7;
    private List<Slice> slices = List.of();
    private int hoveredSliceIndex = -1;

    public PieChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(340, 360));
        setMinimumSize(new Dimension(260, 260));
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setHoveredSlice(findHoveredItem(e.getX(), e.getY()));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { setHoveredSlice(-1); }
        });
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
        setHoveredSlice(-1);
        repaint();
    }

    @Override public String getToolTipText(MouseEvent event) {
        int index = findSliceAt(event.getX(), event.getY());
        if (index < 0) return null;
        return formatSliceDescription(slices.get(index), getPositiveTotal());
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ChartLayout layout = calculateLayout();
            BigDecimal total = getPositiveTotal();
            if (total.signum() == 0) {
                g.setColor(getForeground());
                String message = "Žádná data pro graf";
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(message, (getWidth() - metrics.stringWidth(message)) / 2,
                        layout.y() + layout.diameter() / 2);
                return;
            }
            paintPie(g, layout, total);
            paintLegend(g, layout, total);
        } finally { g.dispose(); }
    }

    private void paintPie(Graphics2D g, ChartLayout layout, BigDecimal total) {
        double angle = 90;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;
            double extent = percentageFraction(slice.amount(), total) * -360;
            double middle = angle + extent / 2;
            boolean highlighted = index == hoveredSliceIndex;
            double dx = highlighted ? Math.cos(Math.toRadians(middle)) * EXPLODE_DISTANCE : 0;
            double dy = highlighted ? -Math.sin(Math.toRadians(middle)) * EXPLODE_DISTANCE : 0;
            Arc2D arc = new Arc2D.Double(layout.x() + dx, layout.y() + dy,
                    layout.diameter(), layout.diameter(), angle, extent, Arc2D.PIE);
            g.setColor(highlighted ? brighten(slice.color(), 1.12) : slice.color());
            g.fill(arc);
            g.setColor(highlighted ? new Color(45, 45, 45, 220) : new Color(255, 255, 255, 190));
            g.setStroke(new BasicStroke(highlighted ? 3f : 1.2f));
            g.draw(arc);
            angle += extent;
        }
    }

    private void paintLegend(Graphics2D g, ChartLayout layout, BigDecimal total) {
        int y = layout.y() + layout.diameter() + 18;
        Font normal = getFont();
        Font bold = normal.deriveFont(Font.BOLD);
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;
            boolean highlighted = index == hoveredSliceIndex;
            if (highlighted) {
                g.setColor(new Color(70, 70, 70, 28));
                g.fillRoundRect(6, y - 15, getWidth() - 12, 22, 8, 8);
                g.setColor(slice.color().darker());
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(6, y - 15, getWidth() - 13, 22, 8, 8);
            }
            g.setColor(slice.color());
            g.fillRoundRect(12, y - 10, 14, 14, 4, 4);
            if (highlighted) {
                g.setColor(slice.color().darker());
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(12, y - 10, 14, 14, 4, 4);
            }
            g.setFont(highlighted ? bold : normal);
            g.setColor(getForeground());
            g.drawString(ellipsize(formatSliceDescription(slice, total),
                    g.getFontMetrics(), getWidth() - 48), 34, y + 1);
            y += 23;
        }
        g.setFont(normal);
    }

    private void setHoveredSlice(int index) {
        if (hoveredSliceIndex == index) return;
        hoveredSliceIndex = index;
        repaint();
    }

    private int findHoveredItem(int x, int y) {
        int slice = findSliceAt(x, y);
        return slice >= 0 ? slice : findLegendItemAt(x, y);
    }

    private int findLegendItemAt(int x, int y) {
        if (x < 6 || x >= getWidth() - 6) return -1;
        ChartLayout layout = calculateLayout();
        int legendY = layout.y() + layout.diameter() + 18;
        for (int index = 0; index < slices.size(); index++) {
            if (slices.get(index).amount().signum() <= 0) continue;
            if (y >= legendY - 15 && y <= legendY + 7) return index;
            legendY += 23;
        }
        return -1;
    }

    private int findSliceAt(int mouseX, int mouseY) {
        BigDecimal total = getPositiveTotal();
        if (total.signum() == 0) return -1;
        ChartLayout layout = calculateLayout();
        double cx = layout.x() + layout.diameter() / 2.0;
        double cy = layout.y() + layout.diameter() / 2.0;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double radius = layout.diameter() / 2.0 + EXPLODE_DISTANCE;
        if (dx * dx + dy * dy > radius * radius) return -1;
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) angle += 360;
        double cumulative = 0;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;
            cumulative += percentageFraction(slice.amount(), total) * 360;
            if (angle <= cumulative || index == slices.size() - 1) return index;
        }
        return -1;
    }

    private ChartLayout calculateLayout() {
        int count = (int) slices.stream().filter(s -> s.amount().signum() > 0).count();
        int legendHeight = count * 23 + 20;
        int diameter = Math.max(60, Math.min(getWidth() - 44, getHeight() - legendHeight - 20));
        return new ChartLayout(Math.max(12, (getWidth() - diameter) / 2), 16, diameter);
    }

    private BigDecimal getPositiveTotal() {
        return slices.stream().map(Slice::amount).filter(v -> v.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String formatSliceDescription(Slice slice, BigDecimal total) {
        return String.format(CZECH, "%s (%.1f %%) – %s", slice.name(),
                percentageFraction(slice.amount(), total) * 100, formatCurrency(slice.amount()));
    }

    private static double percentageFraction(BigDecimal amount, BigDecimal total) {
        return total.signum() == 0 ? 0 : amount.divide(total, 12, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatCurrency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(CZECH);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " Kč";
    }

    private static Color brighten(Color color, double factor) {
        return new Color(Math.min(255, (int) Math.round(color.getRed() * factor)),
                Math.min(255, (int) Math.round(color.getGreen() * factor)),
                Math.min(255, (int) Math.round(color.getBlue() * factor)));
    }

    private static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String value = text;
        while (!value.isEmpty() && metrics.stringWidth(value + "…") > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    private static Color parseColor(String value) {
        try {
            return value != null && value.matches("#[0-9a-fA-F]{6}")
                    ? Color.decode(value) : new Color(160, 160, 160);
        } catch (NumberFormatException ex) { return new Color(160, 160, 160); }
    }

    private record ChartLayout(int x, int y, int diameter) { }
}
