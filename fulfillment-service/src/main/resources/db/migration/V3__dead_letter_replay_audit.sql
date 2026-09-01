ALTER TABLE dead_letter_event ADD COLUMN replayed_by VARCHAR(64) NULL AFTER replayed_at;
