package com.example.AluguelDeCarros;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Banco descartável: nunca usa as credenciais pessoais do Supabase. */
public abstract class PostgresTestBase {
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Somente o PostgreSQL descartável local não fornece TLS.
        registry.add("spring.datasource.hikari.data-source-properties.sslmode", () -> "disable");
    }
}
