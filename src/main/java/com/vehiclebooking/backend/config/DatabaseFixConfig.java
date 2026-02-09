package com.vehiclebooking.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseFixConfig {

    @Bean
    public CommandLineRunner fixConstraints(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                System.out.println("🔧 Running database constraint fixes...");
                // Fix for Payments table
                jdbcTemplate.execute("ALTER TABLE payments MODIFY booking_id BIGINT NULL");
                jdbcTemplate.execute("ALTER TABLE payments MODIFY status VARCHAR(50)");

                // Fix for Transactions table
                jdbcTemplate.execute("ALTER TABLE transactions MODIFY booking_id BIGINT NULL");
                jdbcTemplate.execute("ALTER TABLE transactions MODIFY status VARCHAR(50)");

                // Fix for Vehicles table
                jdbcTemplate.execute("ALTER TABLE vehicles MODIFY type VARCHAR(50)");

                System.out.println("✅ Database constraints and column types updated successfully.");
            } catch (Exception e) {
                System.out.println("⚠️ Note: Database constraint fix skipped or already applied: " + e.getMessage());
            }
        };
    }
}
