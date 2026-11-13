package com.borrowhub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Robust cloud-ready DataSource configuration for BorrowHub.
 * Automatically handles:
 * 1. Cloud PaaS DATABASE_URL / MYSQL_URL (mysql://user:pass@host:port/db) by converting to JDBC format.
 * 2. JDBC format DATABASE_URL / SPRING_DATASOURCE_URL (jdbc:mysql://...).
 * 3. Individual environment variables (DB_HOST / MYSQL_HOST, DB_PORT / MYSQL_PORT, DB_NAME / MYSQL_DATABASE,
 *    DB_USERNAME / MYSQL_USER, DB_PASSWORD / MYSQL_PASSWORD).
 * 4. Required Asia/Kolkata (+05:30) timezone enforcement and UTF-8 encoding.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String DEFAULT_TIMEZONE_PARAMS =
            "serverTimezone=%2B05:30&connectionTimeZone=%2B05:30&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true";

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("BorrowHubHikariPool");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        config.setConnectionInitSql("SET time_zone = '+05:30'");

        String rawUrl = resolveFirstNonEmpty(env, "DATABASE_URL", "MYSQL_URL", "SPRING_DATASOURCE_URL");

        if (rawUrl != null && (rawUrl.startsWith("mysql://") || rawUrl.startsWith("mysql2://"))) {
            configureFromCloudUri(config, rawUrl);
        } else if (rawUrl != null && rawUrl.startsWith("jdbc:mysql://")) {
            configureFromJdbcUrl(config, rawUrl, env);
        } else {
            configureFromIndividualProperties(config, env);
        }

        return new HikariDataSource(config);
    }

    private void configureFromCloudUri(HikariConfig config, String rawUrl) {
        try {
            logger.info("Configuring DataSource from cloud PaaS URI format (mysql://)");
            URI uri = new URI(rawUrl.replaceFirst("^mysql2?://", "http://"));

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : "borrowhub_db";

            String userInfo = uri.getUserInfo();
            String username = "borrowhub_user";
            String password = "";
            if (userInfo != null) {
                int colonIdx = userInfo.indexOf(':');
                if (colonIdx >= 0) {
                    username = URLDecoder.decode(userInfo.substring(0, colonIdx), StandardCharsets.UTF_8);
                    password = URLDecoder.decode(userInfo.substring(colonIdx + 1), StandardCharsets.UTF_8);
                } else {
                    username = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                }
            }

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?" + DEFAULT_TIMEZONE_PARAMS;

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            logger.info("DataSource configured successfully for host: {}, port: {}, database: {}, username: {}",
                    host, port, dbName, username);
        } catch (Exception e) {
            logger.error("Failed to parse cloud database URI. Falling back to default configuration.", e);
            throw new IllegalStateException("Invalid DATABASE_URL format: " + e.getMessage(), e);
        }
    }

    private void configureFromJdbcUrl(HikariConfig config, String jdbcUrl, Environment env) {
        String enrichedUrl = jdbcUrl;
        if (!enrichedUrl.contains("serverTimezone")) {
            enrichedUrl += (enrichedUrl.contains("?") ? "&" : "?") + DEFAULT_TIMEZONE_PARAMS;
        }

        String username = resolveFirstNonEmpty(env, "DB_USERNAME", "MYSQL_USER", "spring.datasource.username");
        if (username == null) username = "borrowhub_user";

        String password = resolveFirstNonEmpty(env, "DB_PASSWORD", "MYSQL_PASSWORD", "spring.datasource.password");
        if (password == null) password = "";

        config.setJdbcUrl(enrichedUrl);
        config.setUsername(username);
        config.setPassword(password);

        logger.info("DataSource configured from JDBC URL with user: {}", username);
    }

    private void configureFromIndividualProperties(HikariConfig config, Environment env) {
        String host = resolveFirstNonEmpty(env, "DB_HOST", "MYSQL_HOST");
        if (host == null || host.isBlank()) host = "localhost";

        String portStr = resolveFirstNonEmpty(env, "DB_PORT", "MYSQL_PORT");
        int port = 3306;
        if (portStr != null && !portStr.isBlank()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {}
        }

        String dbName = resolveFirstNonEmpty(env, "DB_NAME", "MYSQL_DATABASE");
        if (dbName == null || dbName.isBlank()) dbName = "borrowhub_db";

        String username = resolveFirstNonEmpty(env, "DB_USERNAME", "MYSQL_USER");
        if (username == null || username.isBlank()) username = "borrowhub_user";

        String password = resolveFirstNonEmpty(env, "DB_PASSWORD", "MYSQL_PASSWORD");
        if (password == null) password = "";

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?" + DEFAULT_TIMEZONE_PARAMS;

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        logger.info("DataSource configured from individual properties: host={}, port={}, database={}, user={}",
                host, port, dbName, username);
    }

    private String resolveFirstNonEmpty(Environment env, String... propertyNames) {
        for (String name : propertyNames) {
            String val = env.getProperty(name);
            if (val == null || val.isBlank() || val.startsWith("${")) {
                val = System.getenv(name);
            }
            if (val != null && !val.isBlank() && !val.startsWith("${")) {
                return val.trim();
            }
        }
        return null;
    }
}
