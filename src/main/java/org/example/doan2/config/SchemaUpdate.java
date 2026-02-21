package org.example.doan2.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaUpdate implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaUpdate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Force bien_the_id to be nullable
            jdbcTemplate.execute("ALTER TABLE chi_tiet_don_hang MODIFY COLUMN bien_the_id INT NULL");
            System.out.println("Successfully altered table chi_tiet_don_hang column bien_the_id to be NULLABLE");
        } catch (Exception e) {
            System.out.println("Error altering table (might already be fixed or table missing but ignore): " + e.getMessage());
        }
    }
}
