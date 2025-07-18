-- Palm Oil Database Schema
-- Version: 2
-- Created: 2025-07-18

-- Main table for storing reconnaissance form data
CREATE TABLE IF NOT EXISTS recon_forms (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    tree_id TEXT NOT NULL,
    plot_id TEXT NOT NULL,
    number_of_fruits INTEGER NOT NULL,
    harvest_days INTEGER NOT NULL,
    image1_path TEXT,
    image2_path TEXT,
    image3_path TEXT,
    created_at INTEGER NOT NULL,
    is_synced INTEGER NOT NULL DEFAULT 0
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS index_recon_forms_tree_id ON recon_forms(tree_id);
CREATE INDEX IF NOT EXISTS index_recon_forms_plot_id ON recon_forms(plot_id);
CREATE INDEX IF NOT EXISTS index_recon_forms_created_at ON recon_forms(created_at);
CREATE INDEX IF NOT EXISTS index_recon_forms_is_synced ON recon_forms(is_synced);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS index_recon_forms_tree_plot ON recon_forms(tree_id, plot_id);
CREATE INDEX IF NOT EXISTS index_recon_forms_sync_date ON recon_forms(is_synced, created_at);

-- Views for common queries
CREATE VIEW IF NOT EXISTS v_unsynced_forms AS
SELECT 
    id,
    tree_id,
    plot_id,
    number_of_fruits,
    harvest_days,
    image1_path,
    image2_path,
    image3_path,
    created_at,
    datetime(created_at / 1000, 'unixepoch') AS created_date
FROM recon_forms 
WHERE is_synced = 0
ORDER BY created_at DESC;

CREATE VIEW IF NOT EXISTS v_forms_with_images AS
SELECT 
    id,
    tree_id,
    plot_id,
    number_of_fruits,
    harvest_days,
    CASE 
        WHEN image1_path IS NOT NULL THEN 1 ELSE 0 
    END +
    CASE 
        WHEN image2_path IS NOT NULL THEN 1 ELSE 0 
    END +
    CASE 
        WHEN image3_path IS NOT NULL THEN 1 ELSE 0 
    END AS image_count,
    created_at,
    datetime(created_at / 1000, 'unixepoch') AS created_date
FROM recon_forms 
WHERE image1_path IS NOT NULL OR image2_path IS NOT NULL OR image3_path IS NOT NULL
ORDER BY created_at DESC;

-- Triggers for data integrity
CREATE TRIGGER IF NOT EXISTS trigger_update_created_at
AFTER UPDATE ON recon_forms
BEGIN
    UPDATE recon_forms SET created_at = NEW.created_at WHERE id = NEW.id;
END;

-- Sample queries for common operations:

-- Get all forms for a specific tree
-- SELECT * FROM recon_forms WHERE tree_id = 'A001' ORDER BY created_at DESC;

-- Get forms that need syncing
-- SELECT * FROM v_unsynced_forms;

-- Get forms with images
-- SELECT * FROM v_forms_with_images;

-- Get forms by date range (last 7 days)
-- SELECT * FROM recon_forms WHERE created_at >= (strftime('%s', 'now', '-7 days') * 1000);

-- Get summary statistics
-- SELECT 
--     COUNT(*) as total_forms,
--     COUNT(CASE WHEN is_synced = 0 THEN 1 END) as unsynced_forms,
--     COUNT(CASE WHEN image1_path IS NOT NULL OR image2_path IS NOT NULL OR image3_path IS NOT NULL THEN 1 END) as forms_with_images
-- FROM recon_forms;
