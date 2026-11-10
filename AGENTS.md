# Project guidance

- README.md is user-facing only. Build, architecture, verification and contributor
  notes belong in `dev-docs/`; obsolete records can move to `dev-docs/archive/`.
- Maintain `dev-docs/TODO.md` for deferred work and qualification gaps. Keep the
  OpenChamber project TODOs in sync when a tracked deferred item changes.
- Follow the Trino, ClickHouse, DuckDB and Doris sibling plugin patterns. Use
  released Brikk adapters, not copied SQL machinery or locally patched libraries.
- Do not edit vendored `build-logic/`. Change versions through the root catalog
  overrides; follow the shared conventions' ZIP/legal/dependency checks.
- Preserve distinct DataFusionSQL/DATAFUSION_BRIKK and SpiceSQL/SPICE_BRIKK
  ownership. GenericSQL, PostgreSQL and Flight URL syntax do not identify Spice.
- Background validation must not execute SQL, EXPLAIN or prepare statements.
  Keep native verification unavailable and preserve local diagnostics.
- Document upstream Spice/Arrow transport issues with their observed versions,
  actual owners and issue links. Do not add unconditional editor banners or SQL
  warnings that assume every user-installed driver/server still has those bugs.
- Test actual descriptor loading, PSI/console behavior and background logged
  errors on 262/263. Shared-library tests alone are not consumer qualification.
- Never copy another plugin's internal-API exemptions. Publication and pushes
  require explicit owner authorization. Local build success does not grant it.
