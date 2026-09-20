package com.rituals.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = rawUrl;
        String username = rawUsername;
        String password = rawPassword;

        if (dbUrl != null && !dbUrl.isBlank()) {
            String cleanUrl = dbUrl.startsWith("jdbc:") ? dbUrl.substring(5) : dbUrl;
            if (cleanUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(cleanUrl);
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        username = userInfo[0];
                        password = userInfo.length > 1 ? userInfo[1] : "";
                        
                        String host = uri.getHost();
                        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                        String path = uri.getPath();
                        String query = uri.getQuery() != null ? "?" + uri.getQuery() : "?sslmode=require";

                        dbUrl = "jdbc:postgresql://" + host + ":" + port + path + query;
                    }
                } catch (Exception e) {
                    // Ignore parse errors and fallback
                }
            }
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = "jdbc:postgresql://localhost:5432/rituals?sslmode=disable";
            username = "postgres";
            password = "shivudb";
        } else if (!dbUrl.startsWith("jdbc:")) {
            dbUrl = "jdbc:" + dbUrl;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        config.setDriverClassName("org.postgresql.Driver");

        return new HikariDataSource(config);
    }
}
