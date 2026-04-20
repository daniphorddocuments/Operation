package com.daniphord.mahanga.config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseConfig {

    @PostConstruct
    public void init() {
        String dbUrl = System.getenv("DATABASE_URL");

        if (dbUrl != null && dbUrl.startsWith("postgres://")) {
            try {
                dbUrl = dbUrl.replace("postgres://", "");

                String[] parts = dbUrl.split("@");
                String credentials = parts[0];
                String hostAndDb = parts[1];

                String[] credParts = credentials.split(":");
                String username = credParts[0];
                String password = credParts[1];

                String jdbcUrl = "jdbc:postgresql://" + hostAndDb;

                System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);
                System.setProperty("SPRING_DATASOURCE_USERNAME", username);
                System.setProperty("SPRING_DATASOURCE_PASSWORD", password);

                System.out.println("✅ Converted DATABASE_URL to JDBC format");

            } catch (Exception e) {
                System.err.println("❌ Failed to parse DATABASE_URL");
            }
        }
    }
}