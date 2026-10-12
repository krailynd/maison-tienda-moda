package com.fashion.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S1 gate integration test (T-S1-013): the full context boots on Testcontainers
 * PostgreSQL, Flyway V1 applies, and the seed catalog satisfies REQ-CAT-006
 * (at least 8 products, with a "Blusa" product for the search scenario).
 */
class BackendContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndV1MigrationApplies() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'",
                Integer.class);
        assertThat(tableCount).isPositive();
    }

    @Test
    void seedHasAtLeastEightProducts() {
        Long productCount = jdbcTemplate.queryForObject("SELECT count(*) FROM products", Long.class);
        assertThat(productCount).isGreaterThanOrEqualTo(8L);
    }

    @Test
    void seedContainsBlusaProductForSearchScenario() {
        Long blusaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM products WHERE name ILIKE '%blusa%'", Long.class);
        assertThat(blusaCount).isPositive();
    }

    @Test
    void seedVariantsInventoryAndImagesAreConsistent() {
        Long variantCount = jdbcTemplate.queryForObject("SELECT count(*) FROM product_variants", Long.class);
        Long inventoryCount = jdbcTemplate.queryForObject("SELECT count(*) FROM inventories", Long.class);
        Long imageCount = jdbcTemplate.queryForObject("SELECT count(*) FROM product_images", Long.class);

        assertThat(variantCount).isGreaterThanOrEqualTo(8L);
        assertThat(inventoryCount).isPositive();
        assertThat(imageCount).isPositive();
    }
}
