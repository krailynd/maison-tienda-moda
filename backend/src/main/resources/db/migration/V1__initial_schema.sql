-- ============================================================================
-- FASHION E-COMMERCE - DATABASE SCHEMA
-- PostgreSQL 15+
-- ============================================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- ============================================================================
-- TABLE: roles
-- ============================================================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

INSERT INTO roles (name, description) VALUES 
('ADMIN', 'Administrador del sistema con acceso completo'),
('MANAGER', 'Gerente con acceso a gestión de productos y órdenes'),
('CUSTOMER', 'Cliente regular de la tienda'),
('GUEST', 'Usuario no autenticado');

-- ============================================================================
-- TABLE: users
-- ============================================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(20),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    role_id BIGINT REFERENCES roles(id) DEFAULT 3,
    loyalty_points INTEGER DEFAULT 0,
    newsletter_subscribed BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_deleted ON users(deleted_at);

-- Trigger para actualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE: addresses
-- ============================================================================
CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    alias VARCHAR(100),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company VARCHAR(200),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NOT NULL DEFAULT 'PE',
    phone VARCHAR(20),
    is_default BOOLEAN DEFAULT false,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_addresses_country ON addresses(country_code);
CREATE INDEX idx_addresses_postal ON addresses(postal_code);

-- ============================================================================
-- TABLE: categories
-- ============================================================================
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    image_url TEXT,
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    seo_title VARCHAR(255),
    seo_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_active ON categories(is_active);

INSERT INTO categories (name, slug, description, sort_order) VALUES
('Mujer', 'mujer', 'Ropa y accesorios para mujer', 1),
('Hombre', 'hombre', 'Ropa y accesorios para hombre', 2),
('Niños', 'ninos', 'Ropa para niños y niñas', 3),
('Accesorios', 'accesorios', 'Bolsos, cinturones, joyería', 4);

INSERT INTO categories (parent_id, name, slug, description, sort_order) VALUES
(1, 'Vestidos', 'vestidos-mujer', 'Vestidos casuales y de fiesta', 1),
(1, 'Blusas', 'blusas-mujer', 'Blusas y camisas para mujer', 2),
(1, 'Pantalones', 'pantalones-mujer', 'Pantalones y jeans para mujer', 3),
(2, 'Camisas', 'camisas-hombre', 'Camisas formales y casuales', 1),
(2, 'Pantalones', 'pantalones-hombre', 'Pantalones y jeans para hombre', 2);

-- ============================================================================
-- TABLE: brands
-- ============================================================================
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    logo_url TEXT,
    website_url VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_brands_slug ON brands(slug);
CREATE INDEX idx_brands_featured ON brands(is_featured);

-- ============================================================================
-- TABLE: seasons
-- ============================================================================
CREATE TABLE seasons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO seasons (name, code, start_date, end_date) VALUES
('Primavera Verano 2026', 'PV2026', '2026-03-01', '2026-08-31'),
('Otoño Invierno 2026', 'OI2026', '2026-09-01', '2027-02-28');

-- ============================================================================
-- TABLE: collections
-- ============================================================================
CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) UNIQUE NOT NULL,
    description TEXT,
    banner_url TEXT,
    season_id BIGINT REFERENCES seasons(id),
    is_featured BOOLEAN DEFAULT false,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_collections_season ON collections(season_id);
CREATE INDEX idx_collections_featured ON collections(is_featured);

-- ============================================================================
-- TABLE: sizes
-- ============================================================================
CREATE TABLE sizes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    type VARCHAR(20) DEFAULT 'STANDARD', -- STANDARD, US, EU, UK, CM
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

INSERT INTO sizes (name, code, type, sort_order) VALUES
('Extra Pequeño', 'XS', 'STANDARD', 1),
('Pequeño', 'S', 'STANDARD', 2),
('Mediano', 'M', 'STANDARD', 3),
('Grande', 'L', 'STANDARD', 4),
('Extra Grande', 'XL', 'STANDARD', 5),
('Doble Extra Grande', 'XXL', 'STANDARD', 6);

-- ============================================================================
-- TABLE: colors
-- ============================================================================
CREATE TABLE colors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    hex_code VARCHAR(7) NOT NULL,
    rgb_value VARCHAR(20),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

INSERT INTO colors (name, hex_code, rgb_value, sort_order) VALUES
('Negro', '#000000', 'rgb(0,0,0)', 1),
('Blanco', '#FFFFFF', 'rgb(255,255,255)', 2),
('Rojo', '#FF0000', 'rgb(255,0,0)', 3),
('Azul', '#0000FF', 'rgb(0,0,255)', 4),
('Verde', '#008000', 'rgb(0,128,0)', 5),
('Beige', '#F5F5DC', 'rgb(245,245,220)', 6);

-- ============================================================================
-- TABLE: materials
-- ============================================================================
CREATE TABLE materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    composition TEXT,
    care_instructions TEXT,
    is_eco_friendly BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

INSERT INTO materials (name, composition, is_eco_friendly) VALUES
('Algodón', '100% algodón orgánico', true),
('Poliéster', '100% poliéster reciclado', true),
('Lana', '100% lana merino', false),
('Seda', '100% seda natural', false);

-- ============================================================================
-- TABLE: products
-- ============================================================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    sku_prefix VARCHAR(50),
    description TEXT,
    short_description VARCHAR(500),
    brand_id BIGINT REFERENCES brands(id),
    category_id BIGINT REFERENCES categories(id),
    collection_id BIGINT REFERENCES collections(id),
    season_id BIGINT REFERENCES seasons(id),
    material_id BIGINT REFERENCES materials(id),
    price DECIMAL(10,2) NOT NULL,
    compare_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    tax_rate DECIMAL(5,2) DEFAULT 18.00,
    weight DECIMAL(8,2),
    length_cm DECIMAL(8,2),
    width_cm DECIMAL(8,2),
    height_cm DECIMAL(8,2),
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    is_new BOOLEAN DEFAULT false,
    is_eco_friendly BOOLEAN DEFAULT false,
    stock_tracking BOOLEAN DEFAULT true,
    low_stock_threshold INTEGER DEFAULT 5,
    min_order_quantity INTEGER DEFAULT 1,
    max_order_quantity INTEGER DEFAULT 99,
    average_rating DECIMAL(3,2) DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    purchase_count INTEGER DEFAULT 0,
    seo_title VARCHAR(255),
    seo_description TEXT,
    meta_keywords TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_collection ON products(collection_id);
CREATE INDEX idx_products_season ON products(season_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_featured ON products(is_featured);
CREATE INDEX idx_products_new ON products(is_new);
CREATE INDEX idx_products_search ON products USING gin(name gin_trgm_ops, short_description gin_trgm_ops);

-- Trigger para actualizar updated_at en products
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE: product_variants
-- ============================================================================
CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    size_id BIGINT REFERENCES sizes(id),
    color_id BIGINT REFERENCES colors(id),
    price DECIMAL(10,2),
    compare_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    weight DECIMAL(8,2),
    barcode VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_product_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_variants_sku ON product_variants(sku);
CREATE INDEX idx_product_variants_size ON product_variants(size_id);
CREATE INDEX idx_product_variants_color ON product_variants(color_id);
CREATE INDEX idx_product_variants_active ON product_variants(is_active);

-- ============================================================================
-- TABLE: product_images
-- ============================================================================
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    alt_text VARCHAR(255),
    position INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    width INTEGER,
    height INTEGER,
    file_size INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_product_images_product ON product_images(product_id);
CREATE INDEX idx_product_images_variant ON product_images(variant_id);
CREATE INDEX idx_product_images_primary ON product_images(is_primary);

-- ============================================================================
-- TABLE: inventories
-- ============================================================================
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE NOT NULL,
    address TEXT,
    city VARCHAR(100),
    country_code VARCHAR(2) DEFAULT 'PE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO warehouses (name, code, city, country_code) VALUES
('Almacén Central', 'WH-001', 'Lima', 'PE'),
('Almacén Norte', 'WH-002', 'Trujillo', 'PE');

CREATE TABLE inventories (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses(id),
    quantity_on_hand INTEGER DEFAULT 0,
    quantity_reserved INTEGER DEFAULT 0,
    quantity_available INTEGER GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    reorder_point INTEGER DEFAULT 10,
    max_stock_level INTEGER,
    last_counted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(variant_id, warehouse_id)
);

CREATE INDEX idx_inventories_variant ON inventories(variant_id);
CREATE INDEX idx_inventories_warehouse ON inventories(warehouse_id);
CREATE INDEX idx_inventories_available ON inventories(quantity_available);
CREATE INDEX idx_inventories_low_stock ON inventories(warehouse_id, quantity_on_hand) WHERE quantity_on_hand <= reorder_point;

-- ============================================================================
-- TABLE: carts
-- ============================================================================
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    session_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    coupon_code VARCHAR(50),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    shipping_method_id BIGINT,
    notes TEXT,
    abandoned_at TIMESTAMP WITH TIME ZONE,
    recovered_at TIMESTAMP WITH TIME ZONE,
    converted_to_order_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_carts_user ON carts(user_id);
CREATE INDEX idx_carts_session ON carts(session_id);
CREATE INDEX idx_carts_status ON carts(status);
CREATE INDEX idx_carts_abandoned ON carts(abandoned_at) WHERE abandoned_at IS NOT NULL;

-- ============================================================================
-- TABLE: cart_items
-- ============================================================================
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_items_variant ON cart_items(variant_id);

-- ============================================================================
-- TABLE: orders (Partitioned by year)
-- ============================================================================
CREATE TABLE orders (
    id BIGSERIAL,
    uuid UUID DEFAULT uuid_generate_v4() NOT NULL,
    user_id BIGINT REFERENCES users(id),
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    sub_total DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    shipping_cost DECIMAL(12,2) DEFAULT 0,
    tax_amount DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(3) DEFAULT 'PEN',
    billing_address JSONB,
    shipping_address JSONB,
    customer_notes TEXT,
    internal_notes TEXT,
    payment_method VARCHAR(50),
    payment_intent_id VARCHAR(255),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    shipping_method VARCHAR(100),
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    estimated_delivery_date DATE,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    refund_amount DECIMAL(12,2) DEFAULT 0,
    coupon_code VARCHAR(50),
    coupon_discount DECIMAL(10,2) DEFAULT 0,
    loyalty_points_earned INTEGER DEFAULT 0,
    loyalty_points_used INTEGER DEFAULT 0,
    ip_address VARCHAR(45),
    user_agent TEXT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP WITH TIME ZONE,
    order_year INTEGER NOT NULL,
    PRIMARY KEY (id, order_year)
) PARTITION BY RANGE (order_year);

-- Crear particiones para los próximos 5 años
CREATE TABLE orders_2025 PARTITION OF orders FOR VALUES FROM (2025) TO (2026);
CREATE TABLE orders_2026 PARTITION OF orders FOR VALUES FROM (2026) TO (2027);
CREATE TABLE orders_2027 PARTITION OF orders FOR VALUES FROM (2027) TO (2028);
CREATE TABLE orders_2028 PARTITION OF orders FOR VALUES FROM (2028) TO (2029);
CREATE TABLE orders_2029 PARTITION OF orders FOR VALUES FROM (2029) TO (2030);

-- PostgreSQL no permite columnas generadas como clave de partición,
-- así que order_year es una columna real sincronizada por trigger.
CREATE OR REPLACE FUNCTION set_orders_order_year() RETURNS trigger AS $$
BEGIN
    NEW.order_year := EXTRACT(YEAR FROM NEW.created_at AT TIME ZONE 'UTC')::int;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_set_order_year
    BEFORE INSERT OR UPDATE OF created_at ON orders
    FOR EACH ROW EXECUTE FUNCTION set_orders_order_year();

CREATE INDEX idx_orders_uuid ON orders(uuid);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_date ON orders(created_at);
CREATE INDEX idx_orders_tracking ON orders(tracking_number);

-- ============================================================================
-- TABLE: order_items
-- ============================================================================
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_year INTEGER NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(255),
    sku VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    total_price DECIMAL(12,2) NOT NULL,
    fulfillment_status VARCHAR(50) DEFAULT 'PENDING',
    shipped_quantity INTEGER DEFAULT 0,
    returned_quantity INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id, order_year) REFERENCES orders(id, order_year) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order ON order_items(order_id, order_year);
CREATE INDEX idx_order_items_variant ON order_items(variant_id);
CREATE INDEX idx_order_items_fulfillment ON order_items(fulfillment_status);

-- ============================================================================
-- TABLE: order_status_history
-- ============================================================================
CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_year INTEGER NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_by BIGINT REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    notification_sent BOOLEAN DEFAULT false,
    FOREIGN KEY (order_id, order_year) REFERENCES orders(id, order_year) ON DELETE CASCADE
);

CREATE INDEX idx_order_status_order ON order_status_history(order_id, order_year);
CREATE INDEX idx_order_status_date ON order_status_history(changed_at);

-- ============================================================================
-- TABLE: payments
-- ============================================================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_year INTEGER NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(3) DEFAULT 'PEN',
    status VARCHAR(50) DEFAULT 'PENDING',
    provider_response JSONB,
    failure_reason TEXT,
    refunded_amount DECIMAL(12,2) DEFAULT 0,
    refund_reason TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id, order_year) REFERENCES orders(id, order_year) ON DELETE CASCADE
);

CREATE INDEX idx_payments_order ON payments(order_id, order_year);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);
CREATE INDEX idx_payments_status ON payments(status);

-- ============================================================================
-- TABLE: shipping_methods
-- ============================================================================
CREATE TABLE shipping_methods (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    base_cost DECIMAL(10,2) DEFAULT 0,
    cost_per_kg DECIMAL(10,2) DEFAULT 0,
    free_shipping_threshold DECIMAL(10,2),
    min_delivery_days INTEGER,
    max_delivery_days INTEGER,
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    available_countries TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO shipping_methods (name, code, description, base_cost, cost_per_kg, free_shipping_threshold, min_delivery_days, max_delivery_days, is_default) VALUES
('Envío Estándar', 'STANDARD', 'Entrega en 3-5 días hábiles', 10.00, 2.00, 150.00, 3, 5, true),
('Envío Exprés', 'EXPRESS', 'Entrega en 1-2 días hábiles', 25.00, 5.00, 200.00, 1, 2, false),
('Recojo en Tienda', 'PICKUP', 'Recojo gratuito en tienda', 0, 0, 0, 0, 1, false);

-- ============================================================================
-- TABLE: shipping_tracking
-- ============================================================================
CREATE TABLE shipping_tracking (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_year INTEGER NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    carrier VARCHAR(100),
    status VARCHAR(50) DEFAULT 'IN_TRANSIT',
    current_location VARCHAR(255),
    estimated_delivery DATE,
    actual_delivery TIMESTAMP WITH TIME ZONE,
    events JSONB[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id, order_year) REFERENCES orders(id, order_year) ON DELETE CASCADE
);

CREATE INDEX idx_shipping_tracking_order ON shipping_tracking(order_id, order_year);
CREATE INDEX idx_shipping_tracking_number ON shipping_tracking(tracking_number);

-- ============================================================================
-- TABLE: coupons
-- ============================================================================
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED, FREE_SHIPPING, BUY_X_GET_Y
    discount_value DECIMAL(10,2) NOT NULL,
    min_purchase_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    usage_limit INTEGER,
    usage_limit_per_user INTEGER,
    times_used INTEGER DEFAULT 0,
    applicable_categories BIGINT[],
    excluded_categories BIGINT[],
    applicable_products BIGINT[],
    excluded_products BIGINT[],
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    is_public BOOLEAN DEFAULT false,
    auto_apply BOOLEAN DEFAULT false,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_active ON coupons(is_active);
CREATE INDEX idx_coupons_dates ON coupons(valid_from, valid_until);

-- ============================================================================
-- TABLE: wishlists
-- ============================================================================
CREATE TABLE wishlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) DEFAULT 'Mi Lista de Deseos',
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wishlist_items (
    id BIGSERIAL PRIMARY KEY,
    wishlist_id BIGINT NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    priority INTEGER DEFAULT 0,
    notes TEXT,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(wishlist_id, variant_id)
);

CREATE INDEX idx_wishlist_items_wishlist ON wishlist_items(wishlist_id);
CREATE INDEX idx_wishlist_items_variant ON wishlist_items(variant_id);

-- ============================================================================
-- TABLE: reviews
-- ============================================================================
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    comment TEXT,
    is_verified_purchase BOOLEAN DEFAULT false,
    is_approved BOOLEAN DEFAULT false,
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    helpful_count INTEGER DEFAULT 0,
    not_helpful_count INTEGER DEFAULT 0,
    images TEXT[],
    response TEXT,
    responded_by BIGINT REFERENCES users(id),
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_approved ON reviews(is_approved);

-- ============================================================================
-- TABLE: notifications
-- ============================================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    action_url VARCHAR(255),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    channel VARCHAR(50) DEFAULT 'IN_APP',
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;

-- ============================================================================
-- TABLE: audit_logs
-- ============================================================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by BIGINT REFERENCES users(id),
    changed_by_ip VARCHAR(45),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_table ON audit_logs(table_name, record_id);
CREATE INDEX idx_audit_logs_date ON audit_logs(changed_at);
CREATE INDEX idx_audit_logs_user ON audit_logs(changed_by);

-- ============================================================================
-- TABLE: faqs
-- ============================================================================
CREATE TABLE faqs (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO faqs (question, answer, category, sort_order) VALUES
('¿Cuál es el tiempo de envío?', 'El envío estándar toma 3-5 días hábiles. El envío exprés toma 1-2 días hábiles.', 'ENVÍOS', 1),
('¿Cómo puedo devolver un producto?', 'Tienes 30 días para devoluciones. Contacta a servicio al cliente para generar tu etiqueta de devolución.', 'DEVOLUCIONES', 2),
('¿Aceptan tarjetas de crédito?', 'Sí, aceptamos Visa, MasterCard, American Express. También PayPal y transferencia bancaria.', 'PAGOS', 3);

-- ============================================================================
-- TABLE: newsletters
-- ============================================================================
CREATE TABLE newsletter_subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    source VARCHAR(50) DEFAULT 'WEBSITE',
    is_subscribed BOOLEAN DEFAULT true,
    subscribed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP WITH TIME ZONE,
    last_email_sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_newsletter_email ON newsletter_subscribers(email);
CREATE INDEX idx_newsletter_subscribed ON newsletter_subscribers(is_subscribed);

-- ============================================================================
-- VISTAS MATERIALIZADAS
-- ============================================================================

-- Productos más vendidos
CREATE MATERIALIZED VIEW mv_top_selling_products AS
SELECT 
    p.id,
    p.name,
    p.slug,
    p.category_id,
    SUM(oi.quantity) as total_sold,
    COUNT(DISTINCT o.id) as order_count,
    SUM(oi.total_price) as revenue,
    AVG(oi.unit_price) as avg_price
FROM products p
JOIN product_variants pv ON p.id = pv.product_id
JOIN order_items oi ON pv.id = oi.variant_id
JOIN orders o ON oi.order_id = o.id AND oi.order_year = EXTRACT(YEAR FROM o.created_at)
WHERE o.status IN ('DELIVERED', 'COMPLETED')
GROUP BY p.id, p.name, p.slug, p.category_id
ORDER BY total_sold DESC;

CREATE UNIQUE INDEX idx_mv_top_selling_products ON mv_top_selling_products(id);
CREATE INDEX idx_mv_top_selling_sold ON mv_top_selling_products(total_sold DESC);

-- Resumen de ventas por día
CREATE MATERIALIZED VIEW mv_daily_sales_summary AS
SELECT 
    DATE(o.created_at) as sale_date,
    COUNT(DISTINCT o.id) as order_count,
    COUNT(DISTINCT o.user_id) as unique_customers,
    SUM(o.sub_total) as gross_sales,
    SUM(o.discount_amount) as total_discounts,
    SUM(o.shipping_cost) as total_shipping,
    SUM(o.tax_amount) as total_tax,
    SUM(o.total_amount) as net_sales,
    AVG(o.total_amount) as avg_order_value
FROM orders o
WHERE o.deleted_at IS NULL
GROUP BY DATE(o.created_at)
ORDER BY sale_date DESC;

CREATE UNIQUE INDEX idx_mv_daily_sales ON mv_daily_sales_summary(sale_date);

-- Productos con stock bajo
CREATE MATERIALIZED VIEW mv_low_stock_products AS
SELECT 
    p.id,
    p.name,
    p.sku_prefix,
    pv.id as variant_id,
    pv.sku,
    pv.size_id,
    pv.color_id,
    SUM(i.quantity_available) as total_available,
    p.low_stock_threshold
FROM products p
JOIN product_variants pv ON p.id = pv.product_id
JOIN inventories i ON pv.id = i.variant_id
WHERE p.is_active = true AND pv.is_active = true
GROUP BY p.id, p.name, p.sku_prefix, pv.id, pv.sku, pv.size_id, pv.color_id, p.low_stock_threshold
HAVING SUM(i.quantity_available) <= p.low_stock_threshold
ORDER BY total_available ASC;

CREATE UNIQUE INDEX idx_mv_low_stock ON mv_low_stock_products(id, variant_id);

-- ============================================================================
-- FUNCIONES Y TRIGGERS ADICIONALES
-- ============================================================================

-- Función para actualizar stock después de una orden
CREATE OR REPLACE FUNCTION update_inventory_after_order()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.fulfillment_status = 'SHIPPED' THEN
        UPDATE inventories
        SET quantity_on_hand = quantity_on_hand - NEW.quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE variant_id = NEW.variant_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Función para detectar carritos abandonados
CREATE OR REPLACE FUNCTION detect_abandoned_carts()
RETURNS VOID AS $$
BEGIN
    UPDATE carts
    SET abandoned_at = CURRENT_TIMESTAMP,
        status = 'ABANDONED'
    WHERE status = 'ACTIVE'
      AND updated_at < CURRENT_TIMESTAMP - INTERVAL '1 hour'
      AND user_id IS NOT NULL
      AND EXISTS (SELECT 1 FROM cart_items WHERE cart_id = carts.id);
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- DATOS DE PRUEBA
-- ============================================================================

-- Insertar usuario admin
INSERT INTO users (email, password_hash, first_name, last_name, role_id, is_verified, is_active)
VALUES ('admin@fashion.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G.4K5I4UjXl9tC', 'Admin', 'User', 1, true, true);

-- Insertar marcas de ejemplo
INSERT INTO brands (name, slug, description, is_featured) VALUES
('Nike', 'nike', 'Just Do It - Ropa deportiva y lifestyle', true),
('Adidas', 'adidas', 'Impossible is Nothing', true),
('Zara', 'zara', 'Moda rápida y tendencia', true),
('H&M', 'hm', 'Fashion and quality at the best price', false);

-- Insertar productos de ejemplo
INSERT INTO products (name, slug, description, short_description, brand_id, category_id, season_id, price, compare_price, is_active, is_featured, is_new)
VALUES 
('Camiseta Básica Algodón', 'camiseta-basica-algodon', 'Camiseta 100% algodón orgánico, perfecta para el día a día', 'Camiseta básica premium', 1, 1, 1, 29.99, 39.99, true, true, true),
('Jeans Slim Fit', 'jeans-slim-fit', 'Jeans corte slim fit, tela stretch cómoda', 'Jeans modernos y cómodos', 2, 3, 1, 79.99, 99.99, true, false, false),
('Vestido Floral Verano', 'vestido-floral-verano', 'Vestido ligero con estampado floral, ideal para verano', 'Vestido fresco y femenino', 3, 1, 1, 59.99, 79.99, true, true, true);

-- Insertar variantes de productos
INSERT INTO product_variants (product_id, name, sku, size_id, color_id, price, is_default)
VALUES 
(1, 'Camiseta Básica - S / Negro', 'NIKE-TS-S-BLK', 2, 1, 29.99, true),
(1, 'Camiseta Básica - M / Negro', 'NIKE-TS-M-BLK', 3, 1, 29.99, false),
(1, 'Camiseta Básica - L / Blanco', 'NIKE-TS-L-WHT', 4, 2, 29.99, false),
(2, 'Jeans Slim - 32 / Azul', 'ADIDAS-JN-32-BLU', null, 4, 79.99, true),
(3, 'Vestido Floral - S / Multicolor', 'ZARA-VS-S-MLT', 2, null, 59.99, true);

-- Insertar inventario
INSERT INTO inventories (variant_id, warehouse_id, quantity_on_hand, quantity_reserved, reorder_point)
VALUES 
(1, 1, 100, 5, 10),
(2, 1, 80, 3, 10),
(3, 1, 50, 2, 10),
(4, 1, 30, 1, 10),
(5, 1, 25, 0, 10);

-- Insertar imágenes de productos
INSERT INTO product_images (product_id, image_url, thumbnail_url, alt_text, position, is_primary)
VALUES 
(1, 'https://example.com/images/tshirt-black-front.jpg', 'https://example.com/images/thumbs/tshirt-black-front.jpg', 'Camiseta negra frente', 1, true),
(1, 'https://example.com/images/tshirt-black-back.jpg', 'https://example.com/images/thumbs/tshirt-black-back.jpg', 'Camiseta negra espalda', 2, false);

-- ============================================================================
-- SEED TOP-UP (T-S1-015): catalog grows from 3 to 8 products (REQ-CAT-006).
-- Adds "Blusa" products for the search scenario (REQ-CAT-003) plus variants,
-- inventory and images for every new product.
-- ============================================================================

INSERT INTO products (name, slug, description, short_description, brand_id, category_id, season_id, price, compare_price, is_active, is_featured, is_new)
VALUES 
('Blusa Satinada Elegante', 'blusa-satinada-elegante', 'Blusa satinada con corte elegante, ideal para ocasiones especiales', 'Blusa satinada premium', 3, 6, 1, 49.99, 69.99, true, true, true),
('Blusa de Lino Mujer', 'blusa-de-lino-mujer', 'Blusa fresca de lino natural, perfecta para el clima cálido', 'Blusa de lino ligera', 4, 6, 1, 39.99, 49.99, true, false, true),
('Polo Deportivo Hombre', 'polo-deportivo-hombre', 'Polo transpirable de corte deportivo, ideal para el día a día', 'Polo deportivo cómodo', 1, 2, 1, 34.99, null, true, false, false),
('Chaqueta Denim Mujer', 'chaqueta-denim-mujer', 'Chaqueta clásica de denim con lavado moderno', 'Chaqueta denim clásica', 3, 1, 2, 69.99, 89.99, true, true, false),
('Camisa Oxford Hombre', 'camisa-oxford-hombre', 'Camisa Oxford de algodón, formal y versátil', 'Camisa Oxford elegante', 2, 8, 2, 45.99, 59.99, true, false, true);

INSERT INTO product_variants (product_id, name, sku, size_id, color_id, price, is_default)
VALUES 
(4, 'Blusa Satinada - M / Azul', 'ZARA-BS-M-BLU', 3, 4, 49.99, true),
(4, 'Blusa Satinada - L / Azul', 'ZARA-BS-L-BLU', 4, 4, 49.99, false),
(5, 'Blusa de Lino - S / Blanco', 'HM-BL-S-WHT', 2, 2, 39.99, true),
(6, 'Polo Deportivo - M / Verde', 'NIKE-PL-M-GRN', 3, 5, 34.99, true),
(7, 'Chaqueta Denim - M / Azul', 'ZARA-JK-M-BLU', 3, 4, 69.99, true),
(8, 'Camisa Oxford - L / Blanco', 'ADIDAS-CO-L-WHT', 4, 2, 45.99, true);

INSERT INTO inventories (variant_id, warehouse_id, quantity_on_hand, quantity_reserved, reorder_point)
VALUES 
(6, 1, 60, 2, 10),
(7, 1, 40, 1, 10),
(8, 1, 55, 2, 10),
(9, 1, 70, 3, 10),
(10, 1, 25, 1, 10),
(11, 1, 35, 2, 10);

INSERT INTO product_images (product_id, image_url, thumbnail_url, alt_text, position, is_primary)
VALUES 
(4, 'https://example.com/images/blusa-satinada-front.jpg', 'https://example.com/images/thumbs/blusa-satinada-front.jpg', 'Blusa satinada azul frente', 1, true),
(5, 'https://example.com/images/blusa-lino-front.jpg', 'https://example.com/images/thumbs/blusa-lino-front.jpg', 'Blusa de lino blanca frente', 1, true),
(6, 'https://example.com/images/polo-deportivo-front.jpg', 'https://example.com/images/thumbs/polo-deportivo-front.jpg', 'Polo deportivo verde frente', 1, true),
(7, 'https://example.com/images/chaqueta-denim-front.jpg', 'https://example.com/images/thumbs/chaqueta-denim-front.jpg', 'Chaqueta denim azul frente', 1, true),
(8, 'https://example.com/images/camisa-oxford-front.jpg', 'https://example.com/images/thumbs/camisa-oxford-front.jpg', 'Camisa Oxford blanca frente', 1, true);

COMMENT ON TABLE users IS 'Clientes y usuarios del sistema';
COMMENT ON TABLE products IS 'Catálogo principal de productos';
COMMENT ON TABLE product_variants IS 'Variantes de productos por talla y color';
COMMENT ON TABLE orders IS 'Órdenes de compra de clientes';
COMMENT ON TABLE carts IS 'Carritos de compra activos y abandonados';
