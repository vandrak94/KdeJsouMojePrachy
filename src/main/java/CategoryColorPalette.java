import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Shared preset palette used by category create/edit dialogs. */
public final class CategoryColorPalette extends JPanel {
    private static final Preset[] PRESETS = {
        new Preset("Korálová červená", "#FF8A80"),
        new Preset("Meruňková", "#FFCC80"),
        new Preset("Pastelově žlutá", "#FFF59D"),
        new Preset("Limetková", "#DCE775"),
        new Preset("Mátová", "#A5D6A7"),
        new Preset("Tyrkysová", "#80CBC4"),
        new Preset("Nebesky modrá", "#81D4FA"),
        new Preset("Světle modrá", "#90CAF9"),
        new Preset("Levandulová", "#B39DDB"),
        new Preset("Růžová", "#F48FB1"),
        new Preset("Fialová", "#CE93D8"),
        new Preset("Růžový pudr", "#F8BBD0"),
        new Preset("Písková", "#D7CCC8"),
        new Preset("Modrošedá", "#B0BEC5"),
        new Preset("Indigová", "#9FA8DA"),
        new Preset("Pastelově tyrkysová", "#80DEEA"),
        new Preset("Jarní zelená", "#AED581"),
        new Preset("Lososová", "#FFAB91"),
        new Preset("Orchidejová", "#E1BEE7"),
        new Preset("Šalvějová", "#C5E1A5"),
        new Preset("Cihlová", "#DFA39B"),
        new Preset("Medová", "#E8B96A"),
        new Preset("Olivová", "#B7C17A"),
        new Preset("Mořská zelená", "#73BBA5"),
        new Preset("Petrolejová", "#72B4BC"),
        new Preset("Ocelově modrá", "#86A6C8"),
        new Preset("Břidlicová", "#9C9FBE"),
        new Preset("Švestková", "#B68DB6"),
        new Preset("Starorůžová", "#D69BAC"),
        new Preset("Karamelová", "#C8A483")
    };

    private final Set<String> usedColors;

    public CategoryColorPalette(Consumer<String> selectionHandler, Integer editedCategoryId) {
        usedColors = loadUsedColors(editedCategoryId);
        setLayout(new GridLayout(5, 6, 7, 7));
        setBorder(BorderFactory.createTitledBorder("Přednastavené barvy"));

        Preset[] sortedPresets = PRESETS.clone();
        Arrays.sort(sortedPresets, Comparator.comparingDouble(CategoryColorPalette::hue));
        for (Preset preset : sortedPresets) {
            JButton tile = new JButton(preset.code());
            tile.setBackground(Color.decode(preset.code()));
            tile.setForeground(isDark(Color.decode(preset.code())) ? Color.WHITE : Color.BLACK);
            tile.setOpaque(true);
            tile.setPreferredSize(new Dimension(90, 48));
            tile.setToolTipText(usedColors.contains(preset.code())
                    ? "Tato barva je již použita" : preset.code());
            tile.setEnabled(!usedColors.contains(preset.code()));
            tile.addActionListener(event -> selectionHandler.accept(preset.code()));
            add(tile);
        }
    }

    public boolean isUsed(String htmlColor) {
        return usedColors.contains(normalize(htmlColor));
    }

    private static Set<String> loadUsedColors(Integer editedCategoryId) {
        Set<String> colors = new HashSet<>();
        try {
            for (DatabaseManipulator.Category category : DatabaseManipulator.getCategories()) {
                if (editedCategoryId == null || category.id() != editedCategoryId) {
                    colors.add(normalize(category.color()));
                }
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(null,
                    "Použité barvy kategorií se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        return colors;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static double hue(Preset preset) {
        Color color = Color.decode(preset.code());
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
    }

    private static boolean isDark(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue() < 145;
    }

    private record Preset(String name, String code) { }
}
