import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;

public final class DatabaseManipulator {
    private static final Path DATABASE_PATH = Path.of("data", "database.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();

    private DatabaseManipulator() { }

    public static void initializeDatabase() throws Exception {
        Files.createDirectories(DATABASE_PATH.getParent());
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
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

    public static void addCategory(String name, String color) throws SQLException {
        String sql = "INSERT INTO kategorie (nazev, barva) VALUES (?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.executeUpdate();
        }
    }

    public record Category(int id, String name, String color) { }

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

    public static void updateCategory(int id, String name, String color) throws SQLException {
        String sql = "UPDATE kategorie SET nazev = ?, barva = ? WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.setInt(3, id);
            statement.executeUpdate();
        }
    }

    public static void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM kategorie WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public static void clearExpenses() throws SQLException {
        clearTable("vydaje");
    }

    public static void clearCategories() throws SQLException {
        clearTable("kategorie");
    }

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
