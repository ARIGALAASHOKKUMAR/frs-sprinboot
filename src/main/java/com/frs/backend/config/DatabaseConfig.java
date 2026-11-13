
package com.frs.backend.config;

import com.frs.backend.repository.FaceTemplateRepository;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Database URL is missing.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Database username is missing.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Database password is missing.");
        }

        HikariDataSource ds = new HikariDataSource();

        ds.setJdbcUrl(url);
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
                            "Legacy face descriptor was not imported: "
                                    + ex.getMessage()
                    );
                }

            } else {

                System.out.println(
                        "No legacy faces.json found. Skipping import."
                );
            }
        };
    }
}
