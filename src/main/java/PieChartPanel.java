import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
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

/** Koláčový graf výdajů s interaktivními výsečemi a legendou. */
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
            @Override
            public void mouseMoved(MouseEvent event) {
                updateHoveredSlice(event.getX(), event.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                setHoveredSlice(-1);
            }
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

    @Override
    public String getToolTipText(MouseEvent event) {
        int index = findHoveredItem(event.getX(), event.getY());
        if (index < 0) return null;
        Slice slice = slices.get(index);
        BigDecimal total = getPositiveTotal();
        return formatSliceDescription(slice, total);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ChartLayout layout = calculateLayout();
            BigDecimal total = getPositiveTotal();
            if (total.signum() == 0) {
                paintEmptyMessage(g, layout);
                return;
            }

            paintPie(g, layout, total);
            paintLegend(g, layout, total);
        } finally {
            g.dispose();
        }
    }

    private void paintPie(Graphics2D g, ChartLayout layout, BigDecimal total) {
        double angle = 90.0;
        int visibleIndex = 0;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;

            double extent = percentageFraction(slice.amount(), total) * -360.0;
            double middleAngle = angle + extent / 2.0;
            boolean highlighted = index == hoveredSliceIndex;
            double offsetX = highlighted
                    ? Math.cos(Math.toRadians(middleAngle)) * EXPLODE_DISTANCE : 0;
            double offsetY = highlighted
                    ? -Math.sin(Math.toRadians(middleAngle)) * EXPLODE_DISTANCE : 0;

            Arc2D arc = new Arc2D.Double(
                    layout.x() + offsetX,
                    layout.y() + offsetY,
                    layout.diameter(), layout.diameter(),
                    angle, extent, Arc2D.PIE);

            g.setColor(highlighted ? brighten(slice.color(), 1.12) : slice.color());
            g.fill(arc);
            g.setColor(highlighted ? new Color(45, 45, 45, 220)
                    : new Color(255, 255, 255, 190));
            g.setStroke(new BasicStroke(highlighted ? 3.0f : 1.2f));
            g.draw(arc);

            angle += extent;
            visibleIndex++;
        }
    }

    private void paintLegend(Graphics2D g, ChartLayout layout, BigDecimal total) {
        int legendY = layout.y() + layout.diameter() + 18;
        Font normalFont = getFont();
        Font highlightedFont = normalFont.deriveFont(Font.BOLD);

        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;

            boolean highlighted = index == hoveredSliceIndex;
            if (highlighted) {
                g.setColor(new Color(70, 70, 70, 28));
                g.fillRoundRect(6, legendY - 15, getWidth() - 12, 22, 8, 8);
                g.setColor(slice.color().darker());
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(6, legendY - 15, getWidth() - 13, 22, 8, 8);
            }

            g.setColor(slice.color());
            g.fillRoundRect(12, legendY - 10, 14, 14, 4, 4);
            if (highlighted) {
                g.setColor(slice.color().darker());
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(12, legendY - 10, 14, 14, 4, 4);
            }

            g.setFont(highlighted ? highlightedFont : normalFont);
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            String text = formatSliceDescription(slice, total);
            g.drawString(ellipsize(text, metrics, getWidth() - 48), 34, legendY + 1);
            legendY += 23;
        }
        g.setFont(normalFont);
    }

    private void updateHoveredSlice(int mouseX, int mouseY) {
        setHoveredSlice(findHoveredItem(mouseX, mouseY));
    }

    private void setHoveredSlice(int index) {
        if (hoveredSliceIndex == index) return;
        hoveredSliceIndex = index;
        setCursor(index >= 0
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        repaint();
    }

    private int findHoveredItem(int mouseX, int mouseY) {
        int sliceIndex = findSliceAt(mouseX, mouseY);
        return sliceIndex >= 0 ? sliceIndex : findLegendItemAt(mouseX, mouseY);
    }

    private int findLegendItemAt(int mouseX, int mouseY) {
        if (mouseX < 6 || mouseX >= getWidth() - 6) return -1;
        ChartLayout layout = calculateLayout();
        int legendY = layout.y() + layout.diameter() + 18;
        for (int index = 0; index < slices.size(); index++) {
            if (slices.get(index).amount().signum() <= 0) continue;
            if (mouseY >= legendY - 15 && mouseY <= legendY + 7) {
                return index;
            }
            legendY += 23;
        }
        return -1;
    }

    private int findSliceAt(int mouseX, int mouseY) {
        BigDecimal total = getPositiveTotal();
        if (total.signum() == 0) return -1;

        ChartLayout layout = calculateLayout();
        double centerX = layout.x() + layout.diameter() / 2.0;
        double centerY = layout.y() + layout.diameter() / 2.0;
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        double radius = layout.diameter() / 2.0 + EXPLODE_DISTANCE;
        if (deltaX * deltaX + deltaY * deltaY > radius * radius) return -1;

        // Clockwise angle from twelve o'clock, in range <0, 360).
        double mouseAngle = Math.toDegrees(Math.atan2(deltaX, -deltaY));
        if (mouseAngle < 0) mouseAngle += 360.0;

        double cumulativeAngle = 0.0;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            if (slice.amount().signum() <= 0) continue;
            cumulativeAngle += percentageFraction(slice.amount(), total) * 360.0;
            if (mouseAngle <= cumulativeAngle || index == slices.size() - 1) {
                return index;
            }
        }
        return -1;
    }

    private ChartLayout calculateLayout() {
        int positiveSlices = (int) slices.stream()
                .filter(slice -> slice.amount().signum() > 0).count();
        int legendHeight = positiveSlices * 23 + 20;
        int diameter = Math.max(60, Math.min(getWidth() - 44, getHeight() - legendHeight - 20));
        int x = Math.max(12, (getWidth() - diameter) / 2);
        return new ChartLayout(x, 16, diameter);
    }

    private BigDecimal getPositiveTotal() {
        return slices.stream().map(Slice::amount)
                .filter(value -> value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void paintEmptyMessage(Graphics2D g, ChartLayout layout) {
        g.setColor(Color.GRAY);
        String message = "Žádná data pro graf";
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(message,
                (getWidth() - metrics.stringWidth(message)) / 2,
                layout.y() + layout.diameter() / 2);
    }

    private static String formatSliceDescription(Slice slice, BigDecimal total) {
        double percentage = percentageFraction(slice.amount(), total) * 100.0;
        return String.format(CZECH, "%s (%.1f %%) – %s",
                slice.name(), percentage, formatCurrency(slice.amount()));
    }

    private static double percentageFraction(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) return 0.0;
        return amount.divide(total, 12, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatCurrency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(CZECH);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " Kč";
    }

    private static Color brighten(Color color, double factor) {
        return new Color(
                Math.min(255, (int) Math.round(color.getRed() * factor)),
                Math.min(255, (int) Math.round(color.getGreen() * factor)),
                Math.min(255, (int) Math.round(color.getBlue() * factor)));
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

    private record ChartLayout(int x, int y, int diameter) { }
}
