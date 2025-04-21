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

import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.util.Setting;

/**
 * Factory class for creating {@link MySQLConnection} instances.
 *
 * <p>This implementation of {@link ConnectionFactory} creates {@link MySQLConnection} objects using
 * the provided {@link Setting} configuration. The default implementation returned is {@link
 * SimpleMySQLConnection}.
 *
 * @see ConnectionFactory
 * @see MySQLConnection
 * @see SimpleMySQLConnection
 */
public class MySQLConnectionFactory implements ConnectionFactory<MySQLConnection> {

  /**
   * Creates a new {@link MySQLConnection} instance using the provided {@link Setting}.
   *
   * <p>This method initializes a {@link SimpleMySQLConnection} with the specified settings.
   *
   * @param setting the {@link Setting} object containing configuration details for the connection
   * @return a new {@link MySQLConnection} instance
   */
  @Override
  public MySQLConnection createConnection(Setting setting) {
    return new SimpleMySQLConnection(setting);
  }
}
