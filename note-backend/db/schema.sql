-- Simple Note — MySQL schema
--
-- Run this once in phpMyAdmin (Import tab, or paste into the SQL tab) before starting the
-- backend. The application boots with `spring.jpa.hibernate.ddl-auto=validate`, so Hibernate
-- will refuse to start if the entity mapping and this table ever drift apart.

CREATE DATABASE IF NOT EXISTS simple_note
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE simple_note;

CREATE TABLE IF NOT EXISTS notes (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(255) NOT NULL DEFAULT '',
  content     TEXT         NOT NULL,
  is_pinned   BOOLEAN      NOT NULL DEFAULT FALSE,
  is_archived BOOLEAN      NOT NULL DEFAULT FALSE,
  -- DATETIME(6) matches the microsecond precision Hibernate maps java.time.Instant to.
  -- Values are stored in UTC (see hibernate.jdbc.time_zone and connectionTimeZone).
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,

  PRIMARY KEY (id),

  -- Matches the default list query: filter on is_archived, then read out in sort order.
  --
  -- Columns are ascending on purpose. The query sorts `is_pinned DESC, updated_at DESC` —
  -- both keys descend together, so the engine can satisfy it with a backward scan of this
  -- index and still avoid a filesort. Declaring the columns DESC here would buy nothing and
  -- is not portable: MariaDB before 10.8 parses `DESC` in an index definition and silently
  -- ignores it, so the same DDL would produce different indexes on MariaDB and MySQL 8.
  KEY idx_notes_list (is_archived, is_pinned, updated_at),

  -- Last line of defence for the domain rule in NewNote/Note. The application rejects these
  -- with a 400 long before they reach here; this stops anything else (a manual phpMyAdmin
  -- insert, a future import script) from creating a note that says nothing.
  CONSTRAINT chk_notes_not_empty
    CHECK (TRIM(title) <> '' OR TRIM(content) <> '')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
