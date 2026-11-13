import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManipulator {
    private static final Path DATABASE_PATH = Path.of("data", "database.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();

    private DatabaseManipulator() { }

    public static void initializeDatabase() throws Exception {
        Files.createDirectories(DATABASE_PATH.getParent());
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS kategorie (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nazev TEXT NOT NULL,
                        barva TEXT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vydaje (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        datum TEXT NOT NULL,
                        kategorie INTEGER,
                        castka NUMERIC NOT NULL,
                        poznamka TEXT,
                        FOREIGN KEY (kategorie) REFERENCES kategorie(id)
                    )
                    """);
        }
    }

    public static Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    public record Category(int id, String name, String color) { }
    public record ExpenseOverview(int id, String date, int categoryId, String categoryName,
                                  String categoryColor, BigDecimal amount, String note) { }

    public static List<Category> getCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, nazev, barva FROM kategorie ORDER BY id");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                categories.add(new Category(results.getInt("id"),
                        results.getString("nazev"), results.getString("barva")));
            }
        }
        return categories;
    }

    public static void addCategory(String name, String color) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO kategorie (nazev, barva) VALUES (?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.executeUpdate();
        }
    }

    public static void updateCategory(int id, String name, String color) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE kategorie SET nazev = ?, barva = ? WHERE id = ?")) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.setInt(3, id);
            statement.executeUpdate();
        }
    }

    public static boolean isCategoryUsed(int categoryId) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT EXISTS(SELECT 1 FROM vydaje WHERE kategorie = ?)")) {
            statement.setInt(1, categoryId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) != 0;
            }
        }
    }

    public static boolean deleteCategory(int id) throws SQLException {
        // Atomic check + delete, protected against concurrent references by SQLite FK enforcement.
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement check = connection.prepareStatement(
                         "SELECT EXISTS(SELECT 1 FROM vydaje WHERE kategorie = ?)");
                 PreparedStatement delete = connection.prepareStatement(
                         "DELETE FROM kategorie WHERE id = ?")) {
                check.setInt(1, id);
                try (ResultSet result = check.executeQuery()) {
                    if (result.next() && result.getInt(1) != 0) {
                        connection.rollback();
                        return false;
                    }
                }
                delete.setInt(1, id);
                int affected = delete.executeUpdate();
                connection.commit();
                return affected > 0;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static List<ExpenseOverview> getExpensesForDate(LocalDate date) throws SQLException {
        return getExpensesBetween(date, date);
    }

    public static List<ExpenseOverview> getExpensesBetween(LocalDate start, LocalDate end)
            throws SQLException {
        List<ExpenseOverview> expenses = new ArrayList<>();
        String sql = """
                SELECT v.id, v.datum, v.kategorie, COALESCE(k.nazev, '') AS kategorie_nazev,
                       COALESCE(k.barva, '#E1E1E1') AS kategorie_barva, v.castka, v.poznamka
                FROM vydaje v
                LEFT JOIN kategorie k ON k.id = v.kategorie
                WHERE date(v.datum) BETWEEN ? AND ?
                ORDER BY datetime(v.datum) DESC, v.castka DESC, v.id DESC
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, start.toString());
            statement.setString(2, end.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    expenses.add(new ExpenseOverview(result.getInt("id"), result.getString("datum"),
                            result.getInt("kategorie"), result.getString("kategorie_nazev"),
                            result.getString("kategorie_barva"), result.getBigDecimal("castka"),
                            result.getString("poznamka")));
                }
            }
        }
        return expenses;
    }

    public static void addExpense(String date, int categoryId, BigDecimal amount, String note)
            throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO vydaje (datum, kategorie, castka, poznamka) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, date);
            statement.setInt(2, categoryId);
            statement.setBigDecimal(3, amount);
            statement.setString(4, note);
            statement.executeUpdate();
        }
    }

    public static void updateExpense(int id, String date, int categoryId, BigDecimal amount, String note)
            throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE vydaje SET datum = ?, kategorie = ?, castka = ?, poznamka = ? WHERE id = ?")) {
            statement.setString(1, date);
            statement.setInt(2, categoryId);
            statement.setBigDecimal(3, amount);
            statement.setString(4, note);
            statement.setInt(5, id);
            statement.executeUpdate();
        }
    }

    public static void deleteExpense(int id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM vydaje WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public static void clearExpenses() throws SQLException { clearTable("vydaje"); }
    public static void clearCategories() throws SQLException { clearTable("kategorie"); }

    private static void clearTable(String tableName) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + tableName);
                statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = '" + tableName + "'");
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
