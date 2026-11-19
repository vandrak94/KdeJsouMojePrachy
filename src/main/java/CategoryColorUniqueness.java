import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Finds categories sharing the same normalized HTML color code. */
public final class CategoryColorUniqueness {
    private CategoryColorUniqueness() { }

    public static Set<Integer> findDuplicateCategoryIds() throws SQLException {
        List<DatabaseManipulator.Category> categories = DatabaseManipulator.getCategories();
        Map<String, Integer> firstIdByColor = new HashMap<>();
        Set<Integer> duplicates = new HashSet<>();
        for (DatabaseManipulator.Category category : categories) {
            String color = normalize(category.color());
            if (color.isEmpty()) continue;
            Integer firstId = firstIdByColor.putIfAbsent(color, category.id());
            if (firstId != null) {
                duplicates.add(firstId);
                duplicates.add(category.id());
            }
        }
        return duplicates;
    }

    public static boolean hasDuplicates() throws SQLException {
        return !findDuplicateCategoryIds().isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
