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
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.frontend.ui.UITextElementBuilder;
import de.schnippsche.solarreader.plugin.PluginMetadata;
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
@PluginMetadata(
    name = "MySQLExporter",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-MySQLExporter",
    svgImage = "mysqlexporter.svg",
    supportedInterfaces = {SupportedInterface.NONE},
    usedProtocol = KnownProtocol.NONE,
    supports = "MySQL 5.x, 8.x")
public class MySQLExporter extends AbstractExporter {
  private static final String DBNAME = "dbname";
  private static final String REQUIRED_ERROR = "mysqlexporter.required.error";
  private final BlockingQueue<TransferData> queue;
  private final ConnectionFactory<MySQLConnection> connectionFactory;
  private MySQLConnection connection;
  private Thread consumerThread;
  private volatile boolean running = true;

  /** Constructs a new {@code MySQLExporter} with a default {@link MySQLConnectionFactory}. */
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
    consumerThread = new Thread(this::processQueue);
    consumerThread.setName("mysqlexporterThread");
    consumerThread.start();
  }

  @Override
  public void shutdown() {
    running = false;
    consumerThread.interrupt();
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
      String message = resourceBundle.getString("mysqlexporter.connection.successful");
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
            .withLabel(resourceBundle.getString("mysqlexporter.title"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_HOST)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysqlexporter.host.tooltip"))
            .withLabel(resourceBundle.getString("mysqlexporter.host.text"))
            .withPlaceholder(resourceBundle.getString("mysqlexporter.host.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_PORT)
            .withType(HtmlInputType.NUMBER)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysqlexporter.port.tooltip"))
            .withLabel(resourceBundle.getString("mysqlexporter.port.text"))
            .withPlaceholder(resourceBundle.getString("mysqlexporter.port.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_USER)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysqlexporter.user.tooltip"))
            .withLabel(resourceBundle.getString("mysqlexporter.user.text"))
            .withPlaceholder(resourceBundle.getString("mysqlexporter.user.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_PASSWORD)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(false)
            .withTooltip(resourceBundle.getString("mysqlexporter.password.tooltip"))
            .withLabel(resourceBundle.getString("mysqlexporter.password.text"))
            .withPlaceholder(resourceBundle.getString("mysqlexporter.password.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(DBNAME)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("mysqlexporter.name.tooltip"))
            .withLabel(resourceBundle.getString("mysqlexporter.name.text"))
            .withPlaceholder(resourceBundle.getString("mysqlexporter.name.text"))
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

  /** Processes the export queue by taking each entry and exporting it. */
  private void processQueue() {
    while (running) {
      try {
        TransferData transferData = queue.take();
        doStandardExport(transferData);
      } catch (InterruptedException e) {
        if (!running) {
          break; // Exit loop if not running
        }
        Thread.currentThread().interrupt();
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
   * @param table the table containing the data to be exported
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

  /** Updates the configuration of the exporter based on the exporter data. */
  @Override
  protected void updateConfiguration() {
    this.connection = connectionFactory.createConnection(exporterData.getSetting());
  }
}
