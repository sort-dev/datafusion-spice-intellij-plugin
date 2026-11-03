# Driver VM options investigation

## Observed installation

Read-only inspection of the user's running IntelliJ installation identified
IDE **2026.2.1 / 262.9437.185**, with bundled JBR **25.0.3**. No SQL was executed,
no connection was opened, and no saved IDE settings were modified.

The installed main plugin JAR under
`~/.local/share/JetBrains/IntelliJIdea2026.2/datafusion-spice-intellij-plugin/lib/`
is byte-for-byte identical to the latest dogfood ZIP's main JAR. Its driver XML
contains both required flags for both products. This is not a stale-install or
missing-packaged-defaults problem.

The relevant saved driver entry in
`~/.config/JetBrains/IntelliJIdea2026.2/options/databaseDrivers.xml` has
`id="spice-brikk-flight"` and an explicit VM-options override containing only:

```text
--add-opens=java.base/java.nio=ALL-UNNAMED
```

That older saved driver override can mask the updated default. New connections
reuse the existing driver configuration; reinstalling a plugin does not remove
explicit user driver overrides. Only the matching driver's VM-options field was
reported; credentials and other data-source settings were not dumped.

## Why the new-connection field is blank

The installed Database Tools classes show three distinct operations:

1. `LocalDataSource.fromDriver` associates the driver but does not copy its VM
   options into the data source. `LocalDataSource.getVmOptions` returns only the
   data source's own field.
2. `AdvancedPropertiesPanel.resetDataSource` displays that data-source field.
   `resetDriver`, used for the Drivers tab, displays `DatabaseDriverImpl.vmOptions`.
   Saving a blank data-source field normalizes it to null.
3. `ConsoleRunConfiguration.setOptionsFromDataSource` adds the driver's VM
   options to the launch parameter list, then adds the data source's VM options.

Thus a blank connection field is normal and does not mean driver-level options
are absent. The correct place to inspect/fix inherited options is **Data Sources
and Drivers > Drivers > Spice (sort.dev) > Advanced > VM options**. Set both flags
there once. Leave connection-level additions blank unless intentionally needed.

This conclusion comes from the installed IDE's code and on-disk saved driver
override, not a claim to have clicked through or inspected the live dialog.
It does not establish the runtime state of a driver process already started
before changing the settings; reconnect after applying a change.

## Evidence and follow-up

Read-only `javap` captures for the installed IDE are under
`build/vm-options-investigation/`: `LocalDataSource.txt`,
`AdvancedPropertiesPanel.txt`, `ConsoleRunConfiguration.txt` and
`DatabaseDriverImpl.txt`. The default loader and launch concatenation are existing
platform mechanisms, not new plugin code.

Keep the XML defaults. Do not add a startup migration, rewrite saved user settings
while the IDE is open, or duplicate driver defaults into per-connection fields.
Existing tests verified the loaded driver definition but did not verify this
driver-versus-connection distinction. A focused configuration/UI regression can
cover null connection options plus inherited launch options in a later authorized
test pass, without executing SQL.
