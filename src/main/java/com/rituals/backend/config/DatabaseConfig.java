package com.rituals.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs before any Spring beans are created.
 * Strips embedded user:pass credentials out of the datasource URL so HikariCP
 * receives a clean jdbc:postgresql://host:port/db?... URL plus separate
 * username/password properties.
 *
 * This handles both cases:
 *   postgresql://user:pass@host/db?sslmode=require  (Render DATABASE_URL format)
 *   jdbc:postgresql://user:pass@host/db?sslmode=require  (with jdbc: prefix)
 */
public class DatabaseConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || url.isBlank()) return;

        // Strip jdbc: prefix for URI parsing
        String rawUrl = url.startsWith("jdbc:") ? url.substring(5) : url;
        if (!rawUrl.startsWith("postgresql://")) return;

        try {
            URI uri = new URI(rawUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo == null || userInfo.isBlank()) return; // No embedded credentials, nothing to do

            String[] parts = userInfo.split(":", 2);
            String username = parts[0];
            String password = parts.length > 1 ? parts[1] : "";

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "?sslmode=require";
            String fixedUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + query;

            // Only override if not already set separately
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", fixedUrl);
            if (environment.getProperty("spring.datasource.username") == null
                    || environment.getProperty("spring.datasource.username").isBlank()) {
                props.put("spring.datasource.username", username);
            }
            if (environment.getProperty("spring.datasource.password") == null
                    || environment.getProperty("spring.datasource.password").isBlank()) {
                props.put("spring.datasource.password", password);
            }

            // addFirst so this takes highest priority
            environment.getPropertySources().addFirst(new MapPropertySource("fixedDatasource", props));
            System.out.println("[DatabaseConfig] Fixed embedded-credentials datasource URL -> " + fixedUrl);
        } catch (Exception e) {
            System.err.println("[DatabaseConfig] Could not parse datasource URL: " + e.getMessage());
        }
    }
}
