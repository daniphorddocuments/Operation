package com.daniphord.mahanga.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    @Bean
    ApplicationRunner verifyDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {

                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new IllegalStateException("Database health check returned an unexpected result.");
                }

                log.info("Database connection OK: url={}, user={}",
                        connection.getMetaData().getURL(),
                        connection.getMetaData().getUserName());
            } catch (Exception exception) {
                log.error("Database connection failed. Check PostgreSQL host, port, database name, username, and password.", exception);
                throw new IllegalStateException(
                        "Unable to connect to PostgreSQL database. Verify spring.datasource.url, username, password, and that PostgreSQL is running.",
                        exception
                );
            }
        };
    }
}
