
package com.frs.backend.config;

import com.frs.backend.repository.FaceTemplateRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.boot.CommandLineRunner;

import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;

@Configuration
public class DatabaseConfig {

    private Map<String, String> settings() {
        Map<String, String> map = new HashMap<>();

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

        System.getenv().forEach(map::put);

        return map;
    }

    @Bean
    DataSource dataSource() {

        Map<String, String> e = settings();

        HikariDataSource ds = new HikariDataSource();

        // Local PostgreSQL configuration
        String host = e.getOrDefault("PGHOST", "localhost");
        String port = e.getOrDefault("PGPORT", "5432");
        String database = e.getOrDefault("PGDATABASE", "ashokdb");

        String user = e.getOrDefault(
                "PGUSER",
                "apforestdepartment"
        );

        String password = e.getOrDefault(
                "PGPASSWORD",
                ""
        );

        // Optional DATABASE_URL support
        String url = e.get("DATABASE_URL");

        if (url != null && !url.isBlank()) {

            if (url.startsWith("postgres://") ||
                    url.startsWith("postgresql://")) {

                try {
                    java.net.URI uri = java.net.URI.create(url);

                    String info = uri.getUserInfo();

                    if (info != null) {
                        String[] credentials = info.split(":", 2);

                        user = java.net.URLDecoder.decode(
                                credentials[0],
                                java.nio.charset.StandardCharsets.UTF_8
                        );

                        if (credentials.length > 1) {
                            password = java.net.URLDecoder.decode(
                                    credentials[1],
                                    java.nio.charset.StandardCharsets.UTF_8
                            );
                        }
                    }

                    String db = uri.getPath() == null ? "" : uri.getPath();
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
                            "Invalid DATABASE_URL", ex
                    );
                }

            } else {
                ds.setJdbcUrl(
                        url.startsWith("jdbc:")
                                ? url
                                : url.replaceFirst(
                                "^postgres://",
                                "jdbc:postgresql://"
                        )
                );
            }

        } else {

            ds.setJdbcUrl(
                    "jdbc:postgresql://" +
                            host + ":" + port +
                            "/" + database
            );
        }

        ds.setUsername(user);
        ds.setPassword(password);

        if ("true".equalsIgnoreCase(e.get("PGSSL"))) {
            ds.addDataSourceProperty("sslmode", "require");
        }

        ds.setMaximumPoolSize(10);

        return ds;
    }

    @Bean
    CommandLineRunner initialize(DataSource ds, FaceTemplateRepository faceTemplates) {

        return args -> {

            // Initialize database schema
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(
                            new ClassPathResource("schema.sql")
                    );

            populator.setContinueOnError(false);

            DatabasePopulatorUtils.execute(populator, ds);

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

                        Object at = root.hasNonNull("registeredAt")
                                ? java.sql.Timestamp.from(
                                java.time.Instant.parse(
                                        root.get("registeredAt").asText()
                                )
                        )
                                : null;

                        faceTemplates.importLegacy(values.toString(), (java.sql.Timestamp) at);
                    }

                } catch (Exception ex) {
                    System.err.println(
                            "Legacy face descriptor was not imported: " +
                                    ex.getMessage()
                    );
                }
            }
        };
    }
}
