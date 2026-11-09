# SQL Dialects for DataFusion & Spice 0.3.0

This release moves the plugin to Brikk SQL 1.0.1 and IntelliJ support 1.0.1, for
DataGrip and IntelliJ IDEA with Database Tools on IDE builds 261 through 263.

## Changes

- Local DataFusion and Spice syntax checks and Brikk PIPE lowering include the
  SQLGlot 30.29 catch-up.
- Generated SQL keeps a VALUES alias before its nested join.

## Install

Download `datafusion-spice-intellij-plugin-0.3.0.zip` below. In the IDE, open
**Settings > Plugins > Gear > Install Plugin from Disk**, select the ZIP and
restart when prompted. Version 0.3.0 replaces 0.2.0.

See the [README](https://github.com/sort-dev/datafusion-spice-intellij-plugin#readme)
for connection setup, driver VM options and version-specific upstream limitations.

Editor checks run locally; the plugin does not execute, prepare or EXPLAIN your
SQL in the background to validate it. Database features and DDL depend on the
server and catalog configuration. Generated console namespace switching is not
supported. Restart-free plugin changes are not promised.

This independent Sortdev SRL plugin is not affiliated with or endorsed by Apache
DataFusion, Apache Arrow, the Apache Software Foundation or Spice.ai.

Licensed under Apache-2.0. [Privacy information](https://github.com/sort-dev/datafusion-spice-intellij-plugin/blob/v0.3.0/dev-docs/release/privacy.md).
