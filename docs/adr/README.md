# Architecture Decision Records

Each record captures a decision that was not obvious, the alternatives considered, and what it
would cost to reverse. Decisions with a single sensible answer are not recorded here.

| # | Decision | Status |
|---|---|---|
| [0001](0001-clean-architecture-layering.md) | Clean Architecture layering on both sides | Accepted |
| [0002](0002-separate-models-per-layer.md) | A separate model per layer, not one shared class | Accepted |
| [0003](0003-errors-as-values.md) | Errors as values on the client, exceptions on the server | Accepted |
| [0004](0004-idempotent-flag-endpoints.md) | Pin and archive take a value, not a toggle | Accepted |
| [0005](0005-schema-ownership.md) | `schema.sql` + `ddl-auto=validate` instead of Flyway | Accepted |
| [0006](0006-mariadb-driver.md) | Ship both MySQL and MariaDB JDBC drivers | Accepted |
| [0007](0007-adaptive-layout-no-nav-library.md) | Adaptive layout without a navigation library | Accepted |
| [0008](0008-mvi-state-and-effects.md) | MVI with State separate from Effects | Accepted |
