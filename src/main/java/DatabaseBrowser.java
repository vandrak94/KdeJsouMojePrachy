import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

public final class DatabaseBrowser extends JDialog {

    public DatabaseBrowser(Window owner) {
        super(owner, "Prohlížeč databáze", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
        loadDatabase(tabs);

        pack();
        DialogSizePreferences.restoreAndTrack(this, "databaseBrowser");
        setLocationRelativeTo(owner);
    }

    private void loadDatabase(JTabbedPane tabs) {
        try (Connection connection = DatabaseManipulator.openConnection()) {
            List<String> tableNames = readTableNames(connection);
            if (tableNames.isEmpty()) {
                tabs.addTab("Databáze", createMessagePanel("Databáze neobsahuje žádné tabulky."));
                return;
            }

            for (String tableName : tableNames) {
                tabs.addTab(tableName, createTablePanel(connection, tableName));
            }
        } catch (SQLException exception) {
            tabs.addTab("Chyba", createMessagePanel("Databázi se nepodařilo načíst."));
            JOptionPane.showMessageDialog(this,
                    "Databázi se nepodařilo načíst.\n" + exception.getMessage(),
                    "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<String> readTableNames(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getTables(null, null, "%", new String[] {"TABLE"})) {
            while (result.next()) {
                String name = result.getString("TABLE_NAME");
                if (name != null && !name.startsWith("sqlite_")) {
                    tableNames.add(name);
                }
            }
        }
        tableNames.sort(String.CASE_INSENSITIVE_ORDER);
        return tableNames;
    }

    private static JPanel createTablePanel(Connection connection, String tableName) throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT * FROM " + quoteIdentifier(tableName);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            ResultSetMetaData metadata = result.getMetaData();
            int columnCount = metadata.getColumnCount();

            for (int column = 1; column <= columnCount; column++) {
                model.addColumn(metadata.getColumnLabel(column));
            }

            while (result.next()) {
                Object[] row = new Object[columnCount];
                for (int column = 1; column <= columnCount; column++) {
                    row[column - 1] = result.getObject(column);
                }
                model.addRow(row);
            }
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        resizeColumns(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(new JLabel("Počet záznamů: " + model.getRowCount()), BorderLayout.SOUTH);
        return panel;
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static void resizeColumns(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = Math.max(90,
                    table.getTableHeader().getDefaultRenderer()
                            .getTableCellRendererComponent(table,
                                    table.getColumnName(column), false, false, -1, column)
                            .getPreferredSize().width + 20);

            int sampleCount = Math.min(table.getRowCount(), 100);
            for (int row = 0; row < sampleCount; row++) {
                width = Math.max(width,
                        table.getCellRenderer(row, column)
                                .getTableCellRendererComponent(table,
                                        table.getValueAt(row, column), false, false, row, column)
                                .getPreferredSize().width + 20);
            }
            table.getColumnModel().getColumn(column).setPreferredWidth(Math.min(width, 400));
        }
    }

    private static JPanel createMessagePanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
