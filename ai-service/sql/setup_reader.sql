-- Create read-only user for AI service (Cách 1: cross-database)
-- Adjust password/host as needed

CREATE USER IF NOT EXISTS 'reader'@'%' IDENTIFIED BY 'reader';

-- Grant SELECT on each schema used by your services
GRANT SELECT ON product_db.*    TO 'reader'@'%';
GRANT SELECT ON order_db.*      TO 'reader'@'%';
GRANT SELECT ON inventory_db.*  TO 'reader'@'%';
GRANT SELECT ON customer_db.*   TO 'reader'@'%';
GRANT SELECT ON promotion_db.*  TO 'reader'@'%';
GRANT SELECT ON user_db.*       TO 'reader'@'%';
GRANT SELECT ON auth_db.*       TO 'reader'@'%';

GRANT SELECT ON chatbox_db.*    TO 'reader'@'%';

FLUSH PRIVILEGES;

-- Optional: limit result sizes by using views or enforce LIMIT in app prompts.

