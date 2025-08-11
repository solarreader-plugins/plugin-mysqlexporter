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
import de.schnippsche.solarreader.backend.exporter.AbstractExporter;
import de.schnippsche.solarreader.backend.exporter.TransferData;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.frontend.ui.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.tinylog.Logger;

/**
 * Represents an exporter for transferring data into a MySQL database. This class extends {@link
 * AbstractExporter} and handles the queuing and export of {@link TransferData} objects using a
 * specified or default {@link MySQLConnection} factory.
 */
public class MySQLExporter extends AbstractExporter {
  private static final String DBNAME = "dbname";
  private static final String REQUIRED_ERROR = "mysql.exporter.required.error";
  private final BlockingQueue<TransferData> queue;
  private final ConnectionFactory<MySQLConnection> connectionFactory;
  private MySQLConnection connection;
  private Thread consumerThread;
  private volatile boolean running;

  /**
   * Constructs a new {@code MySQLExporter} with a default {@link MySQLConnectionFactory}.
   */
  public MySQLExporter() {
    this(new MySQLConnectionFactory());
  }

  /**
   * Constructs a new {@code MySQLExporter} with the specified {@link ConnectionFactory}.
   *
   * @param connectionFactory a factory for creating {@link MySQLConnection} instances
   */
  public MySQLExporter(ConnectionFactory<MySQLConnection> connectionFactory) {
    super();
    this.connectionFactory = connectionFactory;
    this.queue = new LinkedBlockingQueue<>();
  }

  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("mysqlexporter", locale);
  }

  @Override
  public void initialize() {
    Logger.debug("initialize mysql exporter");
    running = true;
    consumerThread = new Thread(this::processQueue, "mySQLExporterThread");
    consumerThread.start();
  }

  @Override
  public void shutdown() {
    running = false;
    if (consumerThread != null && consumerThread.isAlive()) {
      consumerThread.interrupt();
      try {
        consumerThread.join();
      } catch (InterruptedException e) {
        Logger.warn("shutdown mysql exporter interrupted");
        Thread.currentThread().interrupt();
      }
    }
    Logger.debug("shutdown mysql exporter finished");
  }

  @Override
  public void addExport(TransferData transferData) {
    if (transferData.getTables().isEmpty()) {
      Logger.debug("no exporting tables, skip export");
      return;
    }
    Logger.debug("add export to '{}'", exporterData.getName());
    exporterData.setLastCall(transferData.getTimestamp());
    queue.add(transferData);
  }

  @Override
  public String testExporterConnection(Setting setting) throws IOException {
    try {
      MySQLConnection testConnection = connectionFactory.createConnection(setting);
      String message = resourceBundle.getString("mysql.exporter.connection.successful");
      String version = testConnection.getDatabaseVersion();
      Logger.info("connection successful to {}", version);
      return MessageFormat.format(message, version);
    } catch (SQLException e) {
      Logger.error(e.getMessage());
      throw new IOException(e.getMessage());
    }
  }

  @Override
  public Optional<UIList> getExporterDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UITextElementBuilder()
            .withLabel(resourceBundle.getString("mysql.exporter.title"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_HOST)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysql.exporter.host.tooltip"))
            .withLabel(resourceBundle.getString("mysql.exporter.host.text"))
            .withPlaceholder(resourceBundle.getString("mysql.exporter.host.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_PORT)
            .withType(HtmlInputType.NUMBER)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysql.exporter.port.tooltip"))
            .withLabel(resourceBundle.getString("mysql.exporter.port.text"))
            .withPlaceholder(resourceBundle.getString("mysql.exporter.port.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_USER)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysql.exporter.user.tooltip"))
            .withLabel(resourceBundle.getString("mysql.exporter.user.text"))
            .withPlaceholder(resourceBundle.getString("mysql.exporter.user.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_PASSWORD)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(false)
            .withTooltip(resourceBundle.getString("mysql.exporter.password.tooltip"))
            .withLabel(resourceBundle.getString("mysql.exporter.password.text"))
            .withPlaceholder(resourceBundle.getString("mysql.exporter.password.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(DBNAME)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysql.exporter.name.tooltip"))
            .withLabel(resourceBundle.getString("mysql.exporter.name.text"))
            .withPlaceholder(resourceBundle.getString("mysql.exporter.name.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    return Optional.of(uiList);
  }

  @Override
  public Setting getDefaultExporterSetting() {
    Setting setting = new Setting();
    setting.setProviderHost("localhost");
    setting.setProviderPort(3306);
    setting.setOptionalUser("root");
    setting.setOptionalPassword("root");
    setting.setConfigurationValue(DBNAME, "solarreader");
    return setting;
  }

  /**
   * Processes the export queue by taking each entry and exporting it.
   */
  private void processQueue() {
    while (running) {
      try {
        TransferData transferData = queue.take();
        doStandardExport(transferData);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /**
   * Exports the data from the specified table to the MysqlDB.
   *
   * <p>This method converts the table data into the INSERT and sends the data to Mysql. If the
   * table contains a timestamp column, it uses that; otherwise, it uses the current timestamp. If
   * the table is empty, the export is skipped.
   *
   * @param table         the table containing the data to be exported
   * @param zonedDateTime the timestamp for the export.
   * @throws IOException if an I/O error occurs during the export
   */
  @Override
  protected void exportTable(Table table, ZonedDateTime zonedDateTime) throws IOException {
    long startTime = System.currentTimeMillis();
    if (table.getRows().isEmpty() || table.getColumns().isEmpty()) {
      Logger.warn("empty table(s), skip export");
      return;
    }
    connection.writeTable(table);
    Logger.debug(
        "export table '{}' to '{}' finished in {} ms",
        table.getTableName(),
        exporterData.getName(),
        (System.currentTimeMillis() - startTime));
  }

  /**
   * Updates the configuration of the exporter based on the exporter data.
   */
  @Override
  protected void updateConfiguration() {
    this.connection = connectionFactory.createConnection(exporterData.getSetting());
  }
}
