package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Test
    @DisplayName("V13 agrega las capacidades aprobadas sin reemplazar un precio ya definido ni el consumo")
    void migrateShouldConfigureCapabilitiesWithoutChangingPriceOrUsage() {
        try {
            givenSchemaAtVersionEleven();
            migrateToVersionTwelve();
            jdbcTemplate.update("UPDATE " + ESQUEMA + ".plan "
                    + "SET precio_mensual = 123.45, moneda = 'ARS', configurado = true WHERE codigo = 'PREMIUM'");
            jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".consumo_ia_diario (negocio_id, fecha, paginas, reservadas) "
                    + "SELECT id, DATE '2026-10-19', 3, 2 FROM " + ESQUEMA + ".negocio");

            Flyway.configure().dataSource(dataSource).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .target("13").load().migrate();

            assertThat(jdbcTemplate.queryForObject("SELECT limite_proyectos FROM " + ESQUEMA
                    + ".plan WHERE codigo = 'GRATUITO'", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT limite_registros FROM " + ESQUEMA
                    + ".plan WHERE codigo = 'GRATUITO'", Long.class)).isEqualTo(1000);
            assertThat(jdbcTemplate.queryForObject("SELECT limite_paginas_ia_mensuales FROM " + ESQUEMA
                    + ".plan WHERE codigo = 'PREMIUM'", Integer.class)).isEqualTo(100);
            assertThat(jdbcTemplate.queryForObject("SELECT precio_mensual::text FROM " + ESQUEMA
                    + ".plan WHERE codigo = 'PREMIUM'", String.class)).isEqualTo("123.45");
            assertThat(jdbcTemplate.queryForObject("SELECT moneda FROM " + ESQUEMA
                    + ".plan WHERE codigo = 'PREMIUM'", String.class)).isEqualTo("ARS");
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".consumo_ia_diario WHERE paginas = 3 AND reservadas = 2 AND paginas_inmediatas = 0",
                    Long.class)).isEqualTo(12);
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    @Test
    @DisplayName("V14 conserva negocios, suscripciones, datos y consumo al agregar pruebas sin activarlas")
    void migrateShouldPreserveVersionThirteenDataWithoutStartingTrials() {
        try {
            Totales anteriores = givenSchemaAtVersionEleven();
            Flyway.configure().dataSource(dataSource).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .target("13").load().migrate();
            jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".consumo_ia_diario (negocio_id, fecha, paginas) "
                    + "SELECT id, DATE '2026-10-19', 7 FROM " + ESQUEMA + ".negocio");

            Flyway.configure().dataSource(dataSource).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .target("14").load().migrate();

            thenEveryExistingNegocioHasFreePlan(anteriores);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".prueba_premium",
                    Long.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".consumo_ia_diario WHERE paginas = 7", Long.class)).isEqualTo(anteriores.negocios());
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("V15 conserva prueba, suscripción y datos, configura el precio pendiente y no inicia cobros")
    void migrateShouldPreserveTrialAndConfiguredPriceWithoutCreatingPayments(boolean precioAnterior) {
        try {
            Totales anteriores = givenSchemaAtVersionEleven();
            Flyway.configure().dataSource(dataSource).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .target("14").load().migrate();
            jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".prueba_premium "
                    + "(negocio_id,inicio_en,fin_en,paginas_ia) SELECT id,TIMESTAMPTZ '2026-10-20T12:00:00Z',"
                    + "TIMESTAMPTZ '2026-10-27T12:00:00Z',8 FROM " + ESQUEMA + ".negocio");
            jdbcTemplate.update("UPDATE " + ESQUEMA + ".suscripcion SET estado='PRUEBA',"
                    + "inicio_en=TIMESTAMPTZ '2026-10-20T12:00:00Z',fin_en=TIMESTAMPTZ '2026-10-27T12:00:00Z',"
                    + "plan_id=(SELECT id FROM " + ESQUEMA + ".plan WHERE codigo='PREMIUM')");
            if (precioAnterior) {
                jdbcTemplate.update("UPDATE " + ESQUEMA + ".plan SET configurado=true,precio_mensual=123.45,"
                        + "moneda='ARS' WHERE codigo='PREMIUM'");
            }

            Flyway.configure().dataSource(dataSource).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                    .target("15").load().migrate();

            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".prueba_premium WHERE paginas_ia=8", Long.class)).isEqualTo(anteriores.negocios());
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".suscripcion WHERE estado='PRUEBA' AND fin_en=TIMESTAMPTZ '2026-10-27T12:00:00Z'",
                    Long.class)).isEqualTo(anteriores.negocios());
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".registro",
                    Long.class)).isEqualTo(anteriores.registros());
            assertThat(jdbcTemplate.queryForObject("SELECT precio_mensual::text FROM " + ESQUEMA
                    + ".plan WHERE codigo='PREMIUM'", String.class)).isEqualTo(precioAnterior ? "123.45" : "14900.00");
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".renovacion_premium", Long.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                    + ".pago_premium", Long.class)).isZero();
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
