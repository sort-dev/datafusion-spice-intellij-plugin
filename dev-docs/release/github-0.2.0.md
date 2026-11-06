# SQL Dialects for DataFusion & Spice 0.2.0

The first public release adds separate DataFusion and Spice SQL support to
DataGrip and IntelliJ IDEA with Database Tools, on IDE builds 261 through 263.

## Included

- SQL completion, highlighting, documentation, local diagnostics and reference navigation.
- Separate live catalog inventories and explicit refresh for each engine.
- Product-specific Apache Arrow Flight SQL JDBC connection templates.
- Independent, off-by-default Brikk PIPE generated-SQL preview and run-to-stage.

## Install

Download `datafusion-spice-intellij-plugin-0.2.0.zip` below. In the IDE, open
**Settings > Plugins > Gear > Install Plugin from Disk**, select the ZIP and
restart when prompted. Version 0.2.0 supersedes the earlier 0.1.0 development builds.

See the [README](https://github.com/sort-dev/datafusion-spice-intellij-plugin#readme)
for connection setup, driver VM options and version-specific upstream limitations.

Editor checks run locally; the plugin does not execute, prepare or EXPLAIN your
SQL in the background to validate it. Database features and DDL depend on the
server and catalog configuration. Generated console namespace switching is not
supported. Restart-free plugin changes are not promised.

This independent Sortdev SRL plugin is not affiliated with or endorsed by Apache
DataFusion, Apache Arrow, the Apache Software Foundation or Spice.ai.

Licensed under Apache-2.0. [Privacy information](https://github.com/sort-dev/datafusion-spice-intellij-plugin/blob/v0.2.0/dev-docs/release/privacy.md).
