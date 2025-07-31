/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.plugins.mysql.exporter;

import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.table.TableCell;
import de.schnippsche.solarreader.backend.table.TableColumn;
import de.schnippsche.solarreader.backend.table.TableRow;
import de.schnippsche.solarreader.backend.util.Setting;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import org.tinylog.Logger;

/**
 * A simple implementation of the {@link MySQLConnection} interface, designed to interact with a
 * MySQL database using connection settings.
 *
 * <p>The {@link SimpleMySQLConnection} class provides methods for:
 *
 * <ul>
 *   <li>Retrieving the database version.
 *   <li>Writing data from a {@link Table} to the database.
 * </ul>
 *
 * <p>The connection details (host, port, user, password, database name) are configured via a {@link
 * Setting} object provided during construction.
 *
 * @see MySQLConnection
 * @see Setting
 * @see Table
 */
public class SimpleMySQLConnection implements MySQLConnection {
  private final String host;
  private final int port;
  private final String user;
  private final String password;
  private final String dbName;

  /**
   * Constructs a {@code SimpleMySQLConnection} using the provided {@link Setting}.
   *
   * @param setting the {@link Setting} object containing the connection details
   */
  public SimpleMySQLConnection(Setting setting) {
    this.host = setting.getProviderHost();
    this.port = setting.getProviderPort();
    this.user = setting.getOptionalUser();
    this.password = setting.getOptionalPassword();
    this.dbName = setting.getConfigurationValueAsString("dbname", "solarreader");
  }

  /**
   * Retrieves the version information of the connected MySQL database.
   *
   * <p>This method executes a query to fetch the database version and version comment.
   *
   * @return a {@link String} combining the database type and version
   * @throws SQLException if an error occurs while querying the database
   */
  public String getDatabaseVersion() throws SQLException {
    try (Connection connection = createConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT VERSION(), @@version_comment")) {
      if (resultSet.next()) {
        String version = resultSet.getString(1);
        String database = resultSet.getString(2);
        return database + " " + version;
      }
    }
    return "unknown";
  }

  /**
   * Writes the content of a {@link Table} to the database.
   *
   * <p>This method generates an SQL {@code INSERT} statement based on the table structure and data,
   * and executes it using a batch process.
   *
   * @param table the {@link Table} object containing data to be written
   * @throws IOException if an error occurs during the database write operation
   */
  public void writeTable(Table table) throws IOException {
    String sql = generateInsertStatement(table);
    Logger.debug("sql: " + sql);
    try (Connection connection = createConnection();
        PreparedStatement pstmt = connection.prepareStatement(sql)) {
      List<TableRow> rows = table.getRows();
      for (TableRow row : rows) {
        List<TableCell> cells = row.getCells();
        for (int i = 0; i < cells.size(); i++) {
          pstmt.setObject(i + 1, cells.get(i).getCalculated());
        }
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    } catch (SQLException e) {
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Generates an SQL {@code INSERT} statement for a given {@link Table}.
   *
   * @param table the {@link Table} object representing the table structure and data
   * @return a {@link String} containing the generated SQL statement
   */
  private String generateInsertStatement(Table table) {
    String columnNames =
        table.getColumns().stream()
            .map(TableColumn::getColumnName)
            .collect(Collectors.joining(", "));
    String placeholders =
        table.getColumns().stream().map(column -> "?").collect(Collectors.joining(", "));

    return String.format(
        "INSERT INTO %s (%s) VALUES (%s);", table.getTableName(), columnNames, placeholders);
  }

  /**
   * Creates a new database connection using the provided settings.
   *
   * <p>This method constructs a connection URL and establishes a connection to the MySQL database
   * using {@link DriverManager}.
   *
   * @return a {@link Connection} object representing the database connection
   * @throws SQLException if a connection cannot be established
   */
  private Connection createConnection() throws SQLException {
    String url = String.format("jdbc:mariadb://%s:%s/%s", host, port, dbName);
    Logger.debug("Connecting to " + url);
    DriverManager.setLoginTimeout(5);
    return DriverManager.getConnection(url, user, password);
  }
}
