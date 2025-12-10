-- Additional initialization script for Docker PostgreSQL
-- This runs when container is first created

-- Ensure database and user exist
CREATE DATABASE IF NOT EXISTS resourcedb;
