![Release](https://img.shields.io/github/v/release/solarreader-plugins/plugin-mysqlexporter)
![License](https://img.shields.io/github/license/solarreader-plugins/plugin-mysqlexporter)
![Last Commit](https://img.shields.io/github/last-commit/solarreader-plugins/plugin-mysqlexporter)
![Issues](https://img.shields.io/github/issues/solarreader-plugins/plugin-mysqlexporter)

This plugin allows you to export solar data from the [Solarreader](https://github.com/solarreader-core/solarreader) project to any MySQL- or MariaDB-compatible database.

## Features

- Supports MySQL versions 5.x and 8.x as well as MariaDB versions 10.x and above
- Direct export of solar metrics from the Solarreader core
- User-friendly GUI for configuration – no manual file editing required
- Fully compatible with standard MySQL and MariaDB installations
- Optimized for performance and stability

## Requirements

- A running MySQL (5.x or 8.x) or MariaDB (10.x or higher) server
- An existing database with pre-created tables matching the schema
- A database user account with the following privileges:
  - `INSERT` – to write new data entries
  - `SELECT` – required for server version detection
- The main project **Solarreader**, available at:  
  [https://github.com/solarreader-core/solarreader](https://github.com/solarreader-core/solarreader)