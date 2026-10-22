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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aplica las migraciones sobre una base propia (sin contexto de Spring) para comprobar los datos de prueba de V10.
 * No comparte base con los demás tests porque ellos borran negocios y usuarios.
 */
class DatosPruebaSanJustoMigracionTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    @DisplayName("Deja 10 comercios que aportan a la zona de la UNLaM")
    void migrateShouldLeaveTenSharingNegocios() {
        Integer comercios = jdbc.queryForObject(
                "SELECT count(*) FROM negocio n JOIN zona z ON z.id = n.zona_id"
                        + " WHERE z.nombre = 'San Justo - UNLaM' AND n.comparte_datos_zona", Integer.class);

        assertThat(comercios).isEqualTo(10);
    }

    @Test
    @DisplayName("Los agregados de la zona respetan el mínimo de 3 comercios y no incluyen el Fernet")
    void migrateShouldOmitProductsFromFewerThanThreeComercios() {
        List<String> productos = jdbc.queryForList(
                "SELECT producto_normalizado FROM agregado_zona_producto", String.class);

        assertThat(productos).isNotEmpty().noneMatch(nombre -> nombre.startsWith("fernet"));
        assertThat(jdbc.queryForObject("SELECT min(comercios) FROM agregado_zona_producto", Integer.class))
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("El negocio demo trae su sistema armado con inventario, ventas, clientes y entradas")
    void migrateShouldLeaveDemoSistemaWithFourBloques() {
        Integer bloquesDelDemo = jdbc.queryForObject(
                "SELECT count(*) FROM bloque b JOIN proyecto p ON p.id = b.proyecto_id"
                        + " WHERE p.es_demo AND b.seccion_id IS NOT NULL", Integer.class);

        assertThat(bloquesDelDemo).isEqualTo(4);
    }

    @Test
    @DisplayName("Cada venta sembrada apunta a un producto del mismo negocio")
    void migrateShouldLinkEveryVentaToAnInventarioRegistroOfItsNegocio() {
        Integer huerfanas = jdbc.queryForObject(
                "SELECT count(*) FROM registro v"
                        + " JOIN bloque b ON b.id = v.bloque_id AND b.tipo = 'VENTAS'"
                        + " JOIN campo c ON c.bloque_id = b.id AND c.clave = 'product'"
                        + " WHERE NOT EXISTS (SELECT 1 FROM registro p WHERE p.id::text = v.valores ->> c.id::text"
                        + " AND p.negocio_id = v.negocio_id)", Integer.class);

        assertThat(huerfanas).isZero();
    }
}
