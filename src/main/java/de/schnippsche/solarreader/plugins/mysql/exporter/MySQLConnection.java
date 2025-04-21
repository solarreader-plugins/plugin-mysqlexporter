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

import de.schnippsche.solarreader.backend.connection.general.Connection;
import de.schnippsche.solarreader.backend.table.Table;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Represents a specialized {@link Connection} interface for interactions with a MySQL database.
 * This interface provides additional methods for writing data to tables and retrieving database
 * version information.
 *
 * <p>Implementations of this interface should handle the specific details of connecting to and
 * interacting with a MySQL database.
 *
 * @see Connection
 */
public interface MySQLConnection extends Connection {

  /**
   * Writes the contents of a {@link Table} to the database.
   *
   * <p>This method is expected to handle the serialization of table data and execute the necessary
   * SQL commands to store it in the database.
   *
   * @param table the {@link Table} object containing data to be written
   * @throws IOException if an I/O error occurs during the write operation
   */
  void writeTable(Table table) throws IOException;

  /**
   * Retrieves the version information of the connected MySQL database.
   *
   * <p>This method provides details about the database version, which can be useful for
   * compatibility checks or debugging purposes.
   *
   * @return a {@link String} representing the database version
   * @throws SQLException if a database access error occurs or the version information cannot be
   *     retrieved
   */
  String getDatabaseVersion() throws SQLException;
}
