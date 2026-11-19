package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
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

    private Totales anteriores;

    @AfterEach
    void borrarElEsquemaDePrueba() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
    }

    @Test
    @DisplayName("Actualizar una base V11 asigna el gratuito a todos los negocios sin perder sus registros")
    void actualizarUnaBaseV11AsignaElGratuitoATodosLosNegociosSinPerderSusRegistros() {
        dadoUnEsquemaEnLaVersionOnce();

        cuandoSeMigraHastaLaVersion("14");

        entoncesTodosLosNegociosTienenElPlanGratuito();
    }

    @Test
    @DisplayName("V15 agrega las capacidades aprobadas")
    void v15AgregaLasCapacidadesAprobadas() {
        dadoUnEsquemaEnLaVersionCatorceConPremiumConfiguradoYConsumo();

        cuandoSeMigraHastaLaVersion("15");

        entoncesSeAgreganLasCapacidadesAprobadas();
    }

    @Test
    @DisplayName("V15 no reemplaza un precio ya definido ni el consumo")
    void v15NoReemplazaUnPrecioYaDefinidoNiElConsumo() {
        dadoUnEsquemaEnLaVersionCatorceConPremiumConfiguradoYConsumo();

        cuandoSeMigraHastaLaVersion("15");

        entoncesSeConservanElPrecioYElConsumo();
    }

    @Test
    @DisplayName("V16 conserva negocios, suscripciones, datos y consumo al agregar pruebas sin activarlas")
    void v16ConservaNegociosSuscripcionesDatosYConsumoAlAgregarPruebasSinActivarlas() {
        dadoUnEsquemaEnLaVersionQuinceConConsumoDeSietePaginas();

        cuandoSeMigraHastaLaVersion("16");

        entoncesSeConservaTodoYNoSeActivaNingunaPrueba();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("V17 conserva prueba, suscripción y datos, configura el precio pendiente y no inicia cobros")
    void v17ConservaPruebaSuscripcionYDatosConfiguraElPrecioPendienteYNoIniciaCobros(boolean precioAnterior) {
        dadoUnEsquemaEnLaVersionDieciseisConUnaPruebaEnCurso(precioAnterior);

        cuandoSeMigraHastaLaVersion("17");

        entoncesSeConservaLaPruebaYSeConfiguraElPrecio(precioAnterior);
        entoncesNoSeIniciaNingunCobro();
    }

    // --- dado ---

    private void dadoUnEsquemaEnLaVersionOnce() {
        migrar("11");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".usuario (email, nombre) VALUES (?, ?)",
                "antes-ens94@example.com", "Cuenta existente");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".negocio (nombre, duenio_id) "
                + "SELECT ?, id FROM " + ESQUEMA + ".usuario WHERE email = ?",
                "Negocio anterior a V14", "antes-ens94@example.com");
        anteriores = new Totales(contar("negocio"), contar("registro"));
    }

    private void dadoUnEsquemaEnLaVersionCatorceConPremiumConfiguradoYConsumo() {
        dadoUnEsquemaEnLaVersionOnce();
        migrar("14");
        jdbcTemplate.update("UPDATE " + ESQUEMA + ".plan "
                + "SET precio_mensual = 123.45, moneda = 'ARS', configurado = true WHERE codigo = 'PREMIUM'");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".consumo_ia_diario (negocio_id, fecha, paginas, reservadas) "
                + "SELECT id, DATE '2026-10-19', 3, 2 FROM " + ESQUEMA + ".negocio");
    }

    private void dadoUnEsquemaEnLaVersionQuinceConConsumoDeSietePaginas() {
        dadoUnEsquemaEnLaVersionOnce();
        migrar("15");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".consumo_ia_diario (negocio_id, fecha, paginas) "
                + "SELECT id, DATE '2026-10-19', 7 FROM " + ESQUEMA + ".negocio");
    }

    private void dadoUnEsquemaEnLaVersionDieciseisConUnaPruebaEnCurso(boolean precioAnterior) {
        dadoUnEsquemaEnLaVersionOnce();
        migrar("16");
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
    }

    // --- cuando ---

    private void cuandoSeMigraHastaLaVersion(String version) {
        migrar(version);
    }

    // --- entonces ---

    private void entoncesTodosLosNegociosTienenElPlanGratuito() {
        Long negocios = contar("negocio");
        Long gratuitos = jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + ".suscripcion s "
                + "JOIN " + ESQUEMA + ".plan p ON p.id = s.plan_id "
                + "WHERE p.codigo = 'GRATUITO' AND s.estado = 'ACTIVA' AND s.fin_en IS NULL", Long.class);
        assertThat(negocios).isEqualTo(anteriores.negocios());
        assertThat(gratuitos).isEqualTo(negocios);
        assertThat(contarDonde("negocio", "nombre = 'Negocio anterior a V14'")).isEqualTo(1);
        assertThat(contar("registro")).isEqualTo(anteriores.registros());
        assertThat(anteriores.registros()).isPositive();
    }

    private void entoncesSeAgreganLasCapacidadesAprobadas() {
        assertThat(jdbcTemplate.queryForObject("SELECT limite_proyectos FROM " + ESQUEMA
                + ".plan WHERE codigo = 'GRATUITO'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT limite_registros FROM " + ESQUEMA
                + ".plan WHERE codigo = 'GRATUITO'", Long.class)).isEqualTo(1000);
        assertThat(jdbcTemplate.queryForObject("SELECT limite_paginas_ia_mensuales FROM " + ESQUEMA
                + ".plan WHERE codigo = 'PREMIUM'", Integer.class)).isEqualTo(100);
    }

    private void entoncesSeConservanElPrecioYElConsumo() {
        assertThat(jdbcTemplate.queryForObject("SELECT precio_mensual::text FROM " + ESQUEMA
                + ".plan WHERE codigo = 'PREMIUM'", String.class)).isEqualTo("123.45");
        assertThat(jdbcTemplate.queryForObject("SELECT moneda FROM " + ESQUEMA
                + ".plan WHERE codigo = 'PREMIUM'", String.class)).isEqualTo("ARS");
        assertThat(contarDonde("consumo_ia_diario", "paginas = 3 AND reservadas = 2 AND paginas_inmediatas = 0"))
                .isEqualTo(12);
    }

    private void entoncesSeConservaTodoYNoSeActivaNingunaPrueba() {
        entoncesTodosLosNegociosTienenElPlanGratuito();
        assertThat(contar("prueba_premium")).isZero();
        assertThat(contarDonde("consumo_ia_diario", "paginas = 7")).isEqualTo(anteriores.negocios());
    }

    private void entoncesSeConservaLaPruebaYSeConfiguraElPrecio(boolean precioAnterior) {
        assertThat(contarDonde("prueba_premium", "paginas_ia=8")).isEqualTo(anteriores.negocios());
        assertThat(contarDonde("suscripcion",
                "estado='PRUEBA' AND fin_en=TIMESTAMPTZ '2026-10-27T12:00:00Z'")).isEqualTo(anteriores.negocios());
        assertThat(contar("registro")).isEqualTo(anteriores.registros());
        assertThat(jdbcTemplate.queryForObject("SELECT precio_mensual::text FROM " + ESQUEMA
                + ".plan WHERE codigo='PREMIUM'", String.class))
                .isEqualTo(precioAnterior ? "123.45" : "14900.00");
    }

    private void entoncesNoSeIniciaNingunCobro() {
        assertThat(contar("renovacion_premium")).isZero();
        assertThat(contar("pago_premium")).isZero();
    }

    // --- datos ---

    private void migrar(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(ESQUEMA)
                .defaultSchema(ESQUEMA)
                .target(version)
                .load()
                .migrate();
    }

    private Long contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + "." + tabla, Long.class);
    }

    private Long contarDonde(String tabla, String condicion) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + ESQUEMA + "." + tabla + " WHERE " + condicion, Long.class);
    }

    private record Totales(long negocios, long registros) {
    }
}
