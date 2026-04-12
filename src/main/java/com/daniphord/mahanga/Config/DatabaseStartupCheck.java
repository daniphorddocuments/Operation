package com.daniphord.mahanga.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    @Bean
    @Order(0)
    ApplicationRunner repairLegacySchema(DataSource dataSource) {
        return args -> repairLegacyVideoSessionSchema(dataSource);
    }

    @Bean
    @Order(1)
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

    void repairLegacyVideoSessionSchema(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, "VIDEO_SESSIONS")) {
                return;
            }
            if (!columnExists(connection, "VIDEO_SESSIONS", "AUDIO_ENABLED")) {
                statement.executeUpdate("ALTER TABLE video_sessions ADD COLUMN audio_enabled BOOLEAN");
                log.warn("Repaired legacy schema: added missing video_sessions.audio_enabled column");
            }
            int updatedRows = statement.executeUpdate("UPDATE video_sessions SET audio_enabled = TRUE WHERE audio_enabled IS NULL");
            if (updatedRows > 0) {
                log.warn("Repaired legacy schema: backfilled {} video session row(s) with audio_enabled=TRUE", updatedRows);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to repair legacy video session schema.", exception);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(null, null, tableName, null)) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }
}
