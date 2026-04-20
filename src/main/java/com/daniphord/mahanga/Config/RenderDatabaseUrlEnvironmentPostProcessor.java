package com.daniphord.mahanga.Config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "renderDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configuredDatasourceUrl = firstNonBlank(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("SPRING_DATASOURCE_URL")
        );
        if (configuredDatasourceUrl != null) {
            return;
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("database.url")
        );
        if (databaseUrl == null) {
            return;
        }

        Map<String, Object> properties = convertDatabaseUrl(databaseUrl, environment);
        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Map<String, Object> convertDatabaseUrl(String databaseUrl, ConfigurableEnvironment environment) {
        try {
            URI uri = URI.create(databaseUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme))) {
                return Map.of();
            }

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            if (host == null || host.isBlank() || database.isBlank()) {
                return Map.of();
            }

            String user = null;
            String password = null;
            if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                String[] credentials = uri.getUserInfo().split(":", 2);
                user = credentials[0];
                if (credentials.length > 1) {
                    password = credentials[1];
                }
            }

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + database);

            if (firstNonBlank(environment.getProperty("spring.datasource.username"), environment.getProperty("SPRING_DATASOURCE_USERNAME")) == null
                    && user != null && !user.isBlank()) {
                properties.put("spring.datasource.username", user);
            }

            if (firstNonBlank(environment.getProperty("spring.datasource.password"), environment.getProperty("SPRING_DATASOURCE_PASSWORD")) == null
                    && password != null) {
                properties.put("spring.datasource.password", password);
            }

            return properties;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
