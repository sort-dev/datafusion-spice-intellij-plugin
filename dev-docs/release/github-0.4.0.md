# SQL Dialects for DataFusion & Spice 0.4.0

This release requires IDE 2026.2 or later, for DataGrip and IntelliJ IDEA with
Database Tools on IDE builds 262 through 263. IDE 2026.1 (build 261) is no longer
supported; stay on 0.3.0 there.

## Changes

- Requires IDE 2026.2 (build 262) or later; IDE 2026.1 is no longer supported.
- Runs on the IDE's Java 25.
- Brikk SQL and IntelliJ support remain 1.0.1.

## Install

Download `datafusion-spice-intellij-plugin-0.4.0.zip` below. In the IDE, open
**Settings > Plugins > Gear > Install Plugin from Disk**, select the ZIP and
restart when prompted. Version 0.4.0 replaces 0.3.0.

See the [README](https://github.com/sort-dev/datafusion-spice-intellij-plugin#readme)
for connection setup, driver VM options and version-specific upstream limitations.

Editor checks run locally; the plugin does not execute, prepare or EXPLAIN your
SQL in the background to validate it. Database features and DDL depend on the
server and catalog configuration. Generated console namespace switching is not
supported. Restart-free plugin changes are not promised.

This independent Sortdev SRL plugin is not affiliated with or endorsed by Apache
DataFusion, Apache Arrow, the Apache Software Foundation or Spice.ai.

Licensed under Apache-2.0. [Privacy information](https://github.com/sort-dev/datafusion-spice-intellij-plugin/blob/v0.4.0/dev-docs/release/privacy.md).
