-- Simple Note — optional sample data
--
-- Entirely optional. Run after schema.sql if you want the client to have something to show
-- on first launch. Safe to re-run: it clears the table first.

USE simple_note;

DELETE FROM notes;
ALTER TABLE notes AUTO_INCREMENT = 1;

INSERT INTO notes (title, content, is_pinned, is_archived, created_at, updated_at) VALUES
  ('Welcome to Simple Note',
   'This note came from db/seed.sql. Edit it, pin it, archive it, or delete it.',
   TRUE,  FALSE, '2026-08-10 09:00:00.000000', '2026-08-14 08:30:00.000000'),

  ('Groceries',
   'Milk\nEggs\nCoffee beans\nOlive oil',
   FALSE, FALSE, '2026-08-12 18:20:00.000000', '2026-08-14 07:05:00.000000'),

  ('Reading list',
   'A Philosophy of Software Design\nThe Pragmatic Programmer',
   FALSE, FALSE, '2026-08-13 21:14:00.000000', '2026-08-13 21:14:00.000000'),

  ('Standup notes — week 32',
   'Shipped the search endpoint. Next: pagination.',
   FALSE, TRUE,  '2026-08-04 10:00:00.000000', '2026-08-08 16:45:00.000000');
