![Release](https://img.shields.io/github/v/release/solarreader-plugins/plugin-mysqlexporter)
![License](https://img.shields.io/github/license/solarreader-plugins/plugin-mysqlexporter)
![Last Commit](https://img.shields.io/github/last-commit/solarreader-plugins/plugin-mysqlexporter)
![Issues](https://img.shields.io/github/issues/solarreader-plugins/plugin-mysqlexporter)

# MySQL Exporter Plugin for Solarreader

This plugin enables exporting solar data from the [Solarreader](https://github.com/solarreader-core/solarreader) project
to any MySQL-compatible database.

## Features

- Supports MySQL versions 5.x and 8.x
- Exports solar metrics directly from the Solarreader core project
- GUI-based configuration – no manual editing of files required
- Compatible with standard MySQL installations
- Optimized for performance and reliability

## Requirements

- A running MySQL server (5.x or 8.x)
- An existing MySQL database with pre-created tables
- A MySQL user account with the following privileges:
  - `INSERT` (to write new data entries)
  - `SELECT` – required for server version detection
- Main project **Solarreader**, which can be found here:  
  [https://github.com/solarreader-core/solarreader](https://github.com/solarreader-core/solarreader)



