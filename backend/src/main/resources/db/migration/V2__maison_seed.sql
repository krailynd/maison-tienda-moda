-- =============================================================================
-- V2__maison_seed.sql
-- MAISON catalog seed (MVP): replaces the generic S1 catalog with the luxury
-- products defined in the storefront (src/lib/data.ts) so the API serves the
-- same catalog the UI was designed for. Images point to the frontend's local
-- assets (/images/pN.png) instead of dead example.com URLs.
--
-- Design notes:
--   * Keeps products 1..8 (their ids) and rewrites them in place.
--   * Adds the storefront's sizes/colors as rows and maps variants via
--     (color, size) subselects so the script does not depend on hard-coded ids.
--   * Generates one variant per (color x size) combination with a plpgsql DO
--     block, plus one inventory row per variant in warehouse 1 (WH-001).
--   * Ratings/review counts mirror the storefront data.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0. Safe reset of catalog-dependent rows (MVP has no carts/wishlists/reviews)
-- ---------------------------------------------------------------------------
DELETE FROM cart_items;
DELETE FROM product_images WHERE product_id BETWEEN 1 AND 8;
DELETE FROM inventories WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id BETWEEN 1 AND 8);
DELETE FROM product_variants WHERE product_id BETWEEN 1 AND 8;

-- ---------------------------------------------------------------------------
-- 1. Colors: align existing Negro to the storefront hex, add the new ones
-- ---------------------------------------------------------------------------
UPDATE colors SET hex_code = '#1C1C1E' WHERE name = 'Negro' AND hex_code = '#000000';

INSERT INTO colors (name, hex_code, sort_order) VALUES
('Marfil',          '#F5F0EB', 10),
('Champagne',       '#E8D4C0', 11),
('Camel',           '#C4956A', 12),
('Gris',            '#9E9E9E', 13),
('Carbón',          '#3A3A3C', 14),
('Rosa empolvado',  '#C9A9A6', 15),
('Crema',           '#F5F0EB', 16),
('Salvia',          '#A8B5A0', 17),
('Cognac',          '#8B5A2B', 18),
('Arena',           '#D4B896', 19)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Sizes: add numeric / unique sizes used by the storefront
-- ---------------------------------------------------------------------------
INSERT INTO sizes (name, code, type, sort_order) VALUES
('Único',     'Único', 'ONE_SIZE', 100),
('Talla 40',  '40',    'SHOES',    110),
('Talla 41',  '41',    'SHOES',    111),
('Talla 42',  '42',    'SHOES',    112),
('Talla 43',  '43',    'SHOES',    113),
('Talla 44',  '44',    'SHOES',    114),
('Talla 46',  '46',    'PANTS',    120),
('Talla 48',  '48',    'PANTS',    121),
('Talla 50',  '50',    'PANTS',    122),
('Talla 52',  '52',    'PANTS',    123)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Brand: MAISON house brand
-- ---------------------------------------------------------------------------
INSERT INTO brands (name, slug, description, is_featured)
SELECT 'Maison', 'maison', 'MAISON — moda de lujo atemporal', true
WHERE NOT EXISTS (SELECT 1 FROM brands WHERE slug = 'maison');

-- ---------------------------------------------------------------------------
-- 4. Rewrite products 1..8 with the storefront catalog
-- ---------------------------------------------------------------------------
-- category ids: 1 = Mujer, 2 = Hombre, 4 = Accesorios
UPDATE products SET
    name = 'Blusa de Seda Marfil',
    slug = 'blusa-seda-marfil',
    description = 'Blusa confeccionada en seda pura de 22 momme con botones de nácar genuino. Corte fluido que cae con elegancia natural, ideal para transiciones del día a la noche.',
    short_description = 'Seda pura de 22 momme, corte fluido',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 1,
    price = 289.00,
    compare_price = NULL,
    average_rating = 4.80,
    review_count = 128,
    is_new = true,
    is_featured = true
WHERE id = 1;

UPDATE products SET
    name = 'Abrigo Oversize Camel',
    slug = 'abrigo-lana-camel',
    description = 'Abrigo oversize en lana virgen de doble faz, sin forro, con acabados cosidos a mano. Silueta envolvente con hombros caídos y cuello sastre.',
    short_description = 'Lana virgen doble faz, silueta envolvente',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 1,
    price = 890.00,
    compare_price = 1090.00,
    average_rating = 4.90,
    review_count = 86,
    is_new = false,
    is_featured = true
WHERE id = 2;

UPDATE products SET
    name = 'Pantalón Sastre Negro',
    slug = 'pantalon-sastre-negro',
    description = 'Pantalón de sastrería de talle alto con pinzas y caída recta. Tejido de lana fría con elasticidad natural para máximo confort.',
    short_description = 'Lana fría Super 120s, talle alto',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 2,
    price = 340.00,
    compare_price = NULL,
    average_rating = 4.70,
    review_count = 64,
    is_new = false,
    is_featured = false
WHERE id = 3;

UPDATE products SET
    name = 'Suéter de Cachemir Rosa',
    slug = 'sueter-cachemir-rosa',
    description = 'Suéter de cachemir de grado A, hilado en 12 gauge para una calidez ligera. Tono rosa empolvado teñido con pigmentos naturales.',
    short_description = 'Cachemir grado A, teñido natural',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 1,
    price = 420.00,
    compare_price = NULL,
    average_rating = 4.90,
    review_count = 212,
    is_new = true,
    is_featured = true
WHERE id = 4;

UPDATE products SET
    name = 'Vestido Slip Champagne',
    slug = 'vestido-slip-champagne',
    description = 'Vestido slip cortado al bies en satén de seda con tirantes ajustables. Un esencial atemporal que fluye con el movimiento.',
    short_description = 'Satén de seda, corte al bies',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 1,
    price = 510.00,
    compare_price = NULL,
    average_rating = 4.60,
    review_count = 93,
    is_new = false,
    is_featured = false
WHERE id = 5;

UPDATE products SET
    name = 'Bolso de Cuero Cognac',
    slug = 'bolso-cuero-cognac',
    description = 'Bolso estructurado en cuero de curtido vegetal con herrajes de latón dorado. Envejece con una pátina única que cuenta tu historia.',
    short_description = 'Cuero de curtido vegetal, hecho a mano',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 4,
    price = 1250.00,
    compare_price = NULL,
    average_rating = 5.00,
    review_count = 47,
    is_new = true,
    is_featured = true
WHERE id = 6;

UPDATE products SET
    name = 'Sneakers de Cuero Blanco',
    slug = 'sneakers-cuero-blanco',
    description = 'Sneakers minimalistas en cuero napa con suela de goma natural. Silueta limpia sin logos, el básico definitivo del guardarropa.',
    short_description = 'Cuero napa, suela de goma natural',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 2,
    price = 380.00,
    compare_price = NULL,
    average_rating = 4.80,
    review_count = 156,
    is_new = false,
    is_featured = true
WHERE id = 7;

UPDATE products SET
    name = 'Blazer de Lino Salvia',
    slug = 'blazer-lino-salvia',
    description = 'Blazer desestructurado en lino belga lavado, hombro natural y construcción ligera. Perfecto para climas cálidos con espíritu sartorial.',
    short_description = 'Lino belga lavado, desestructurado',
    brand_id = (SELECT id FROM brands WHERE slug = 'maison'),
    category_id = 2,
    price = 620.00,
    compare_price = 740.00,
    average_rating = 4.70,
    review_count = 71,
    is_new = false,
    is_featured = false
WHERE id = 8;

-- ---------------------------------------------------------------------------
-- 5. Generate one variant per (color x size) per product, plus inventory
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
    v_color_id BIGINT;
    v_size_id BIGINT;
    v_variant_id BIGINT;
    v_sku TEXT;
    v_stock INT;
    v_seq INT;
BEGIN
    -- Each entry: product id | price | colors (by name) | sizes (by code)
    FOR r IN
        SELECT * FROM (VALUES
            (1, 289.00, ARRAY['Marfil','Champagne','Negro'],  ARRAY['XS','S','M','L']),
            (2, 890.00, ARRAY['Camel','Gris'],                ARRAY['S','M','L']),
            (3, 340.00, ARRAY['Negro','Carbón'],              ARRAY['46','48','50','52']),
            (4, 420.00, ARRAY['Rosa empolvado','Crema','Salvia'], ARRAY['XS','S','M','L']),
            (5, 510.00, ARRAY['Champagne','Negro'],           ARRAY['XS','S','M','L']),
            (6, 1250.00, ARRAY['Cognac','Negro'],             ARRAY['Único']),
            (7, 380.00, ARRAY['Blanco','Crema'],              ARRAY['40','41','42','43','44']),
            (8, 620.00, ARRAY['Salvia','Arena'],              ARRAY['48','50','52'])
        ) AS t(product_id, price, colors, sizes)
    LOOP
        v_seq := 0;
        FOR i IN 1..array_length(r.colors, 1) LOOP
            SELECT id INTO v_color_id FROM colors WHERE name = r.colors[i];
            FOR j IN 1..array_length(r.sizes, 1) LOOP
                SELECT id INTO v_size_id FROM sizes WHERE code = r.sizes[j];
                IF v_color_id IS NULL OR v_size_id IS NULL THEN
                    RAISE NOTICE 'Missing color/size for product %', r.product_id;
                    CONTINUE;
                END IF;
                v_seq := v_seq + 1;
                v_sku := 'MAIS-' || lpad(r.product_id::text, 3, '0') || '-' || lpad(v_seq::text, 2, '0');
                v_stock := 10 + (r.product_id * v_seq) % 25; -- 10..34

                INSERT INTO product_variants
                    (product_id, name, sku, size_id, color_id, price, is_default, position)
                VALUES (
                    r.product_id,
                    (SELECT name FROM products WHERE id = r.product_id) || ' - ' ||
                        (SELECT name FROM sizes WHERE id = v_size_id) || ' / ' ||
                        (SELECT name FROM colors WHERE id = v_color_id),
                    v_sku,
                    v_size_id,
                    v_color_id,
                    r.price,
                    (v_seq = 1),
                    v_seq
                )
                RETURNING id INTO v_variant_id;

                INSERT INTO inventories (variant_id, warehouse_id, quantity_on_hand, quantity_reserved, reorder_point)
                VALUES (v_variant_id, 1, v_stock, 0, 5);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 6. Product images -> frontend local assets (/images/pN.png)
-- ---------------------------------------------------------------------------
INSERT INTO product_images (product_id, image_url, thumbnail_url, alt_text, position, is_primary) VALUES
(1, '/images/p1.png', '/images/p1.png', 'Blusa de Seda Marfil', 1, true),
(2, '/images/p2.png', '/images/p2.png', 'Abrigo Oversize Camel', 1, true),
(3, '/images/p3.png', '/images/p3.png', 'Pantalón Sastre Negro', 1, true),
(4, '/images/p4.png', '/images/p4.png', 'Suéter de Cachemir Rosa', 1, true),
(5, '/images/p5.png', '/images/p5.png', 'Vestido Slip Champagne', 1, true),
(6, '/images/p6.png', '/images/p6.png', 'Bolso de Cuero Cognac', 1, true),
(7, '/images/p7.png', '/images/p7.png', 'Sneakers de Cuero Blanco', 1, true),
(8, '/images/p8.png', '/images/p8.png', 'Blazer de Lino Salvia', 1, true);

-- Also update the category counts helper columns if the schema exposes them
-- (products table has no cached count column in V1; counts come from queries).
