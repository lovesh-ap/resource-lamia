-- Initialize database schema with deliberate missing indexes for slow queries
-- This script seeds the database with sample data

-- Create indexes only on selected columns (deliberately missing some for slow queries)
CREATE INDEX IF NOT EXISTS idx_data_record_category ON data_record(category)^^;
CREATE INDEX IF NOT EXISTS idx_data_record_timestamp ON data_record(timestamp)^^;
-- Deliberately NOT indexing data_record.payload for slow LIKE searches

-- Deliberately NOT indexing related_entity.data_record_id to cause slow joins
CREATE INDEX IF NOT EXISTS idx_related_entity_status ON related_entity(status)^^;

-- Deliberately NOT indexing audit_log foreign keys
-- CREATE INDEX idx_audit_log_record_id ON audit_log(record_id);
-- CREATE INDEX idx_audit_log_related_id ON audit_log(related_id);

-- Seed data only if tables are empty
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM data_record;
    
    IF record_count = 0 THEN
        -- Insert 75,000 DataRecord entries
        INSERT INTO data_record (payload, timestamp, category, amount)
        SELECT
            'Sample data payload with content for record ' || i || ' ' || 
            repeat('data ', 50),
            NOW() - (i || ' minutes')::INTERVAL,
            'category_' || (i % 10),
            (random() * 1000)::DECIMAL(10,2)
        FROM generate_series(1, 75000) AS i;
        
        RAISE NOTICE 'Inserted 75000 data_record entries';
        
        -- Insert 150,000 RelatedEntity entries (2 per DataRecord)
        INSERT INTO related_entity (data_record_id, metadata, status)
        SELECT
            (i % 75000) + 1,
            'Metadata for related entity ' || i || ' with additional information ' || 
            repeat('metadata ', 20),
            'status_' || (i % 5)
        FROM generate_series(1, 150000) AS i;
        
        RAISE NOTICE 'Inserted 150000 related_entity entries';
        
        -- Insert 200,000 AuditLog entries
        INSERT INTO audit_log (record_id, related_id, description, created_at)
        SELECT
            (i % 75000) + 1,
            (i % 150000) + 1,
            'Audit log entry ' || i || ' describing action taken on record with details ' ||
            repeat('audit ', 15),
            NOW() - (i || ' minutes')::INTERVAL
        FROM generate_series(1, 200000) AS i;
        
        RAISE NOTICE 'Inserted 200000 audit_log entries';
        
        RAISE NOTICE 'Database initialization completed successfully';
    ELSE
        RAISE NOTICE 'Database already contains data, skipping initialization';
    END IF;
END $$^^;
