# 0005 — `schema.sql` + `ddl-auto=validate` instead of Flyway

**Status:** Accepted

## Context

Something has to create the `notes` table and keep it in step with `NoteEntity`. The brief
names **phpMyAdmin** as the database management tool and asks not to add libraries that are
not required.

## Decision

A checked-in `note-backend/db/schema.sql` that the developer runs once in phpMyAdmin, with
`spring.jpa.hibernate.ddl-auto=validate` so Hibernate refuses to boot if the entity mapping
and the table disagree.

## Alternatives considered

**`ddl-auto=update`.** Convenient, and wrong outside a toy: it silently alters live tables,
never drops anything, and produces a schema nobody has reviewed. Rejected.

**Flyway.** The production-grade answer, and the right one as soon as this app has a second
deployment. Rejected for now on two grounds: it is a dependency the brief did not ask for, and
it takes ownership of the schema away from phpMyAdmin, which the brief explicitly names.

## Consequences

- The DDL is reviewable, commented, and lives next to the code it belongs to.
- Drift fails loudly at boot rather than quietly at runtime. This is not theoretical — it is
  what verified that `BOOLEAN`/`TINYINT(1)` and `DATETIME(6)` actually match what Hibernate
  expects, which no unit test could have checked.
- Adding a column is a manual step: edit `schema.sql`, edit `NoteEntity`, run the SQL.
  Acceptable at one table; the trigger to adopt Flyway is the second environment or the first
  migration that must run unattended.
- `@DataJpaTest` runs on H2 with `ddl-auto=create-drop`, because `schema.sql` is MySQL-specific
  DDL that H2 will not parse. Those tests therefore verify the queries and the adapter, not the
  production DDL — booting against real MySQL is what verifies that.
