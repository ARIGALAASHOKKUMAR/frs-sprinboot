
package com.frs.backend.config;

import com.frs.backend.repository.FaceTemplateRepository;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.boot.CommandLineRunner;

import javax.sql.DataSource;
import java.nio.file.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfig {

    private Map<String, String> settings() {

        Map<String, String> map = new HashMap<>();

        // Read .env only for local development
        Path dotEnv = Paths.get(".env");

        if (Files.exists(dotEnv)) {
            try {
                for (String line : Files.readAllLines(dotEnv)) {

                    String s = line.trim();

                    if (s.isEmpty() || s.startsWith("#") || !s.contains("=")) {
                        continue;
                    }

                    int n = s.indexOf('=');

                    map.put(
                            s.substring(0, n).trim(),
                            s.substring(n + 1).trim()
                                    .replaceAll("^[\\\"']|[\\\"']$", "")
                    );
                }

            } catch (Exception ex) {
                System.err.println("Unable to read .env: " + ex.getMessage());
            }
        }

        // Environment variables override .env values
        System.getenv().forEach(map::put);

        return map;
    }

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {

        Map<String, String> e = settings();

        HikariDataSource ds = new HikariDataSource();

        String databaseUrl = e.get("DATABASE_URL");

        String host = e.get("PGHOST");
        String port = e.getOrDefault("PGPORT", "5432");
        String database = e.getOrDefault("PGDATABASE", "postgres");

        String username = e.get("PGUSER");
        String password = e.get("PGPASSWORD");

        // Fallback to Spring Boot properties
        if (username == null || username.isBlank()) {
            username = environment.getProperty("spring.datasource.username");
        }

        if (password == null || password.isBlank()) {
            password = environment.getProperty("spring.datasource.password");
        }

        if (databaseUrl != null && !databaseUrl.isBlank()) {

            try {

                URI uri = URI.create(databaseUrl);

                String userInfo = uri.getUserInfo();

                if (userInfo != null) {

                    String[] credentials = userInfo.split(":", 2);

                    username = URLDecoder.decode(
                            credentials[0],
                            StandardCharsets.UTF_8
                    );

                    if (credentials.length > 1) {
                        password = URLDecoder.decode(
                                credentials[1],
                                StandardCharsets.UTF_8
                        );
                    }
                }

                String db = uri.getPath() == null ? "/postgres" : uri.getPath();

                String query = uri.getQuery() == null
                        ? ""
                        : "?" + uri.getQuery();

                ds.setJdbcUrl(
                        "jdbc:postgresql://" +
                                uri.getHost() + ":" +
                                (uri.getPort() < 0 ? 5432 : uri.getPort()) +
                                db + query
                );

            } catch (Exception ex) {

                throw new IllegalArgumentException(
                        "Invalid DATABASE_URL configuration",
                        ex
                );
            }

        } else if (host != null && !host.isBlank()) {

            // Supabase / PostgreSQL environment configuration
            ds.setJdbcUrl(
                    "jdbc:postgresql://" +
                            host + ":" + port +
                            "/" + database +
                            "?sslmode=require"
            );

        } else {

            // Use application.properties configuration
            String springUrl = environment.getProperty(
                    "spring.datasource.url"
            );

            if (springUrl == null || springUrl.isBlank()) {
                throw new IllegalStateException(
                        "Database URL is missing. Configure DATABASE_URL, PGHOST or spring.datasource.url."
                );
            }

            ds.setJdbcUrl(springUrl);
        }

        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Database username is missing.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Database password is missing.");
        }

        ds.setUsername(username);
        ds.setPassword(password);

        ds.setDriverClassName("org.postgresql.Driver");

        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);

        ds.setConnectionTimeout(30000);
        ds.setIdleTimeout(600000);
        ds.setMaxLifetime(1800000);

        return ds;
    }

    @Bean
    public CommandLineRunner initialize(
            DataSource ds,
            FaceTemplateRepository faceTemplates
    ) {

        return args -> {

            System.out.println("Initializing database...");

            // Initialize database schema
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(
                            new ClassPathResource("schema.sql")
                    );

            populator.setContinueOnError(false);

            DatabasePopulatorUtils.execute(populator, ds);

            System.out.println("Database schema initialized successfully.");

            // Import legacy face descriptor
            Path legacy = Paths.get("../faces.json");

            if (!Files.exists(legacy)) {
                legacy = Paths.get("faces.json");
            }

            if (Files.exists(legacy)) {

                try {

                    com.fasterxml.jackson.databind.JsonNode root =
                            new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readTree(Files.readString(legacy));

                    com.fasterxml.jackson.databind.JsonNode desc =
                            root == null ? null : root.get("descriptor");

                    if (desc != null &&
                            desc.isArray() &&
                            desc.size() == 128) {

                        StringBuilder values = new StringBuilder("{");

                        for (int i = 0; i < 128; i++) {

                            if (i > 0) {
                                values.append(',');
                            }

                            values.append(desc.get(i).asDouble());
                        }

                        values.append('}');

                        Timestamp registeredAt =
                                root.hasNonNull("registeredAt")
                                        ? Timestamp.from(
                                                Instant.parse(
                                                        root.get("registeredAt").asText()
                                                )
                                        )
                                        : null;

                        faceTemplates.importLegacy(
                                values.toString(),
                                registeredAt
                        );

                        System.out.println(
                                "Legacy face descriptor imported successfully."
                        );
                    }

                } catch (Exception ex) {

                    System.err.println(
                            "Legacy face descriptor was not imported: " +
                                    ex.getMessage()
                    );
                }

            } else {

                System.out.println("No legacy faces.json found. Skipping import.");
            }
        };
    }
}
