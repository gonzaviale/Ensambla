package com.ensambla.backend.infrastructure.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aplica las migraciones sobre una base propia para comprobar el objetivo de la cuenta demo de V33. No comparte base
 * con los demás tests porque ellos borran negocios y usuarios.
 */
class ObjetivoDemoMigracionTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));
    private static final String DEMO = "demo@ensambla.test";
    private static final String VENTAS_DEL_DEMO_POR_SEMANA = """
            SELECT (current_date - (r.valores ->> cf.id::text)::date) / 7 AS semanas_atras,
                   sum((r.valores ->> cq.id::text)::numeric) AS unidades
            FROM registro r
            JOIN bloque b ON b.id = r.bloque_id AND b.tipo = 'VENTAS'
            JOIN negocio n ON n.id = b.negocio_id
            JOIN usuario u ON u.id = n.duenio_id AND u.email = ?
            JOIN campo cf ON cf.bloque_id = b.id AND cf.clave = 'date'
            JOIN campo cq ON cq.bloque_id = b.id AND cq.clave = 'quantity'
            WHERE r.origen = 'Demo de objetivos'
            GROUP BY 1
            ORDER BY 1
            """;

    private static JdbcTemplate jdbc;

    private List<Map<String, Object>> objetivos;
    private List<BigDecimal> unidades;
    private Integer cantidad;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        jdbc = migrarBase(POSTGRES.getJdbcUrl());
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    @DisplayName("La cuenta demo trae un objetivo creado hace 8 semanas con una meta redonda")
    void laCuentaDemoTraeUnObjetivoCreadoHace8SemanasConUnaMetaRedonda() {
        cuandoSeConsultanLosObjetivosDeLaCuentaDemo();

        entoncesHayUnObjetivoDeHace8SemanasConMetaRedonda();
    }

    @Test
    @DisplayName("Las ventas que suma al demo cubren las últimas 12 semanas y la última vende más que la primera")
    void lasVentasQueSumaAlDemoCubrenLasUltimas12SemanasYLaUltimaVendeMasQueLaPrimera() {
        cuandoSeConsultanLasVentasDelDemoPorSemana();

        entoncesSeCubren12SemanasConVentasEnAumento();
    }

    @Test
    @DisplayName("El azúcar del demo sigue sin ventas en el último mes")
    void elAzucarDelDemoSigueSinVentasEnElUltimoMes() {
        cuandoSeCuentanLasVentasDeAzucarDelUltimoMes();

        entoncesElAzucarNoTieneVentas();
    }

    @Test
    @DisplayName("Sin la cuenta demo la migración no hace nada")
    void sinLaCuentaDemoLaMigracionNoHaceNada() {
        cuandoSeMigraUnaBaseSinCuentaDemo();

        entoncesNoSeCreaNingunObjetivo();
    }

    // --- cuando ---

    private void cuandoSeConsultanLosObjetivosDeLaCuentaDemo() {
        objetivos = jdbc.queryForList("""
                SELECT o.meta, (current_date - o.creado_en::date) AS dias
                FROM objetivo o
                JOIN negocio n ON n.id = o.negocio_id
                JOIN usuario u ON u.id = n.duenio_id
                WHERE u.email = ?
                """, DEMO);
    }

    private void cuandoSeConsultanLasVentasDelDemoPorSemana() {
        unidades = jdbc.queryForList(VENTAS_DEL_DEMO_POR_SEMANA, DEMO).stream()
                .map(fila -> (BigDecimal) fila.get("unidades"))
                .toList();
    }

    private void cuandoSeCuentanLasVentasDeAzucarDelUltimoMes() {
        cantidad = jdbc.queryForObject("""
                SELECT count(*)
                FROM registro r
                JOIN bloque b ON b.id = r.bloque_id AND b.tipo = 'VENTAS'
                JOIN campo cp ON cp.bloque_id = b.id AND cp.clave = 'product'
                JOIN campo cf ON cf.bloque_id = b.id AND cf.clave = 'date'
                JOIN registro p ON p.id::text = r.valores ->> cp.id::text
                JOIN negocio n ON n.id = b.negocio_id
                JOIN usuario u ON u.id = n.duenio_id AND u.email = ?
                WHERE p.valores::text ILIKE '%azúcar%' AND (r.valores ->> cf.id::text)::date > current_date - 30
                """, Integer.class, DEMO);
    }

    private void cuandoSeMigraUnaBaseSinCuentaDemo() {
        JdbcTemplate sinDemo = baseSinCuentaDemo();
        cantidad = sinDemo.queryForObject("SELECT count(*) FROM objetivo", Integer.class);
    }

    // --- entonces ---

    private void entoncesHayUnObjetivoDeHace8SemanasConMetaRedonda() {
        assertThat(objetivos).hasSize(1);
        assertThat((Integer) objetivos.getFirst().get("dias")).isEqualTo(56);
        assertThat((BigDecimal) objetivos.getFirst().get("meta"))
                .isGreaterThanOrEqualTo(new BigDecimal("100000"))
                .satisfies(meta -> assertThat(meta.remainder(new BigDecimal("10000"))).isZero());
    }

    private void entoncesSeCubren12SemanasConVentasEnAumento() {
        assertThat(unidades).hasSize(12);
        assertThat(unidades.getFirst()).isGreaterThan(unidades.getLast());
    }

    private void entoncesElAzucarNoTieneVentas() {
        assertThat(cantidad).isZero();
    }

    private void entoncesNoSeCreaNingunObjetivo() {
        assertThat(cantidad).isZero();
    }

    // --- datos ---

    /** Otra base del mismo contenedor migrada hasta V32; la cuenta demo cambia de email y recién después corre V33. */
    private static JdbcTemplate baseSinCuentaDemo() {
        jdbc.execute("CREATE DATABASE sin_demo");
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/sin_demo");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url, POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("32").load().migrate();
        JdbcTemplate sinDemo = new JdbcTemplate(dataSource);
        sinDemo.update("UPDATE usuario SET email = ? WHERE email = ?", "ya-no-es-demo@ensambla.test", DEMO);
        return migrarBase(url);
    }

    private static JdbcTemplate migrarBase(String url) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url, POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        return new JdbcTemplate(dataSource);
    }
}
