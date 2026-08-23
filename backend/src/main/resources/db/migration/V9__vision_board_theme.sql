-- FASE 16: Board Themes. The existing `background` column on vision_boards
-- was already documented (V8 migration's own entity doc comment) as "a
-- stable identifier (theme/template id, or a plain color) resolved
-- entirely client-side" — exactly the Board Theme concept this phase adds,
-- and never populated by any UI up to this point. Rather than adding a
-- second, redundant column, this renames it to `theme` and gives it a
-- concrete default so the backend is always the single source of truth for
-- a board's theme, never an implicit "null means light" convention spread
-- across the frontend.
UPDATE vision_boards SET background = 'LIGHT' WHERE background IS NULL;

ALTER TABLE vision_boards ALTER COLUMN background SET DEFAULT 'LIGHT';
ALTER TABLE vision_boards ALTER COLUMN background SET NOT NULL;
ALTER TABLE vision_boards RENAME COLUMN background TO theme;
