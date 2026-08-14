# 0006 — Ship both MySQL and MariaDB JDBC drivers

**Status:** Accepted

## Context

The brief specifies MySQL with phpMyAdmin. The development machine runs XAMPP, whose control
panel is labelled "MySQL" but which actually bundles **MariaDB 10.4.32**. This is the norm:
XAMPP switched to MariaDB years ago and kept the label.

Booting against it with only `mysql-connector-j` produced:

```
HHH000511: The 5.5.5 version for [org.hibernate.dialect.MySQLDialect] is no longer
supported... The minimum supported version is 8.0.0.
```

MariaDB reports its version string as `5.5.5-10.4.32-MariaDB` — the `5.5.5-` prefix is a
legacy compatibility hack for old clients. Connector/J parses it as version **5.5.5**, so
Hibernate selected `MySQLDialect` and generated SQL for a 2010-era server.

## Decision

Put both drivers on the runtime classpath and let the JDBC URL scheme choose:

```kotlin
runtimeOnly("com.mysql:mysql-connector-j")
runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.10")
```

The default `DB_URL` uses `jdbc:mariadb://`, matching what XAMPP actually runs. Switching to
real MySQL 8 is one environment variable: `DB_URL=jdbc:mysql://...`.

## Rationale

The simple CRUD in this app would probably have worked under the misdetected dialect. "Probably
worked" is not a property worth shipping — Hibernate was making decisions about SQL generation
based on a false belief about the server, and the failure mode would be a subtle query bug
found much later.

Pinning the dialect manually (`spring.jpa.database-platform=...`) was considered. It silences
the symptom but leaves Connector/J talking to a server it does not officially support. Using
each database's own driver is the honest fix.

## Consequences

- One additional `runtimeOnly` dependency, against the brief's "no unnecessary libraries". The
  judgement is that correctly identifying the database is not optional.
- Verified: with the MariaDB driver, Hibernate correctly reports `MariaDBDialect` at version
  10.4.32, and `ddl-auto=validate` passes against the real `schema.sql`.
- A residual warning remains — Hibernate 7 supports MariaDB 10.6+, and XAMPP ships 10.4.
  Everything this app uses works; a newer MariaDB or MySQL 8 clears it.
