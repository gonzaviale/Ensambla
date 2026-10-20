package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionPlanesPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String ESQUEMA = "prueba_ens94_actualizacion";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Actualizar una base V11 asigna el gratuito a todos los negocios sin perder sus registros")
    void migrateShouldBackfillExistingNegociosWithoutDataLoss() {
        try {
            Totales anteriores = givenSchemaAtVersionEleven();

            migrateToVersionTwelve();

            thenEveryExistingNegocioHasFreePlan(anteriores);
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    // --- arrange ---
    private Totales givenSchemaAtVersionEleven() {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(ESQUEMA)
                .defaultSchema(ESQUEMA)
                .target("11")
                .load()
                .migrate();
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".usuario (email, nombre) VALUES (?, ?)",
                "antes-ens94@example.com", "Cuenta existente");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".negocio (nombre, duenio_id) "
                + "SELECT ?, id FROM " + ESQUEMA + ".usuario WHERE email = ?",
                "Negocio anterior a V12", "antes-ens94@example.com");
        return new Totales(
                jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".negocio", Long.class),
                jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".registro", Long.class));
    }

    // --- act ---
    private void migrateToVersionTwelve() {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(ESQUEMA)
                .defaultSchema(ESQUEMA)
                .target("12")
                .load()
                .migrate();
    }

    // --- assert ---
    private void thenEveryExistingNegocioHasFreePlan(Totales anteriores) {
        Long negocios = jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".negocio", Long.class);
        Long gratuitos = jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".suscripcion s "
                + "JOIN " + ESQUEMA + ".plan p ON p.id = s.plan_id "
                + "WHERE p.codigo = 'GRATUITO' AND s.estado = 'ACTIVA' AND s.fin_en IS NULL", Long.class);
        assertThat(negocios).isEqualTo(anteriores.negocios());
        assertThat(gratuitos).isEqualTo(negocios);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".negocio WHERE nombre = 'Negocio anterior a V12'", Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".registro", Long.class)).isEqualTo(anteriores.registros());
        assertThat(anteriores.registros()).isPositive();
    }

    private record Totales(long negocios, long registros) {
    }
}
