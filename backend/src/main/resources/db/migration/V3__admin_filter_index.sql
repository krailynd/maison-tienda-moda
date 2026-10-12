-- ============================================================================
-- ADMIN PANEL - FILTER INDEX (slice A)
-- Composite index on the partitioned `orders` table for the admin panel's
-- status / payment_status / created_at filters. Because `orders` is a
-- partitioned table (RANGE on order_year), this statement creates a
-- partitioned index; PostgreSQL automatically creates matching indexes on
-- every existing partition (orders_2025 ... orders_2029) and on future ones.
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_orders_admin_filter
    ON orders (status, payment_status, created_at);
