package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V34 pasa el plan del negocio a la cuenta: se prueba contra una base V33 con cuentas ya creadas (gratuitas, en
 * prueba y Premium con renovación y pagos) para comprobar que ningún dato se pierde.
 */
class MigracionPlanPorCuentaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String ESQUEMA = "prueba_h55_plan_por_cuenta";

    // Flyway abre y cierra sus propias conexiones: las migraciones con tablas temporales de sesión (como V33) no
    // pueden compartir la conexión del pool entre una migración y la siguiente.
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String usuario;

    @Value("${spring.datasource.password}")
    private String contrasenia;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
    }

    @Test
    @DisplayName("Las cuentas actuales heredan el plan de su negocio sin perder suscripciones, pruebas ni cobros")
    void migrateShouldMoveSubscriptionTrialRenewalAndPaymentsToTheAccount() {
        migrarHasta("33");
        UUID gratuita = givenCuentaConNegocio("gratuita");
        UUID premium = givenCuentaConNegocio("premium");
        UUID enPrueba = givenCuentaConNegocio("prueba");
        givenSuscripcion(negocioDe(premium), "PREMIUM", "ACTIVA", "now() - interval '10 days'",
                "now() + interval '20 days'");
        givenRenovacionYPago(negocioDe(premium));
        givenSuscripcion(negocioDe(enPrueba), "PREMIUM", "PRUEBA", "now() - interval '1 day'",
                "now() - interval '1 day' + interval '168 hours'");
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".prueba_premium (negocio_id, inicio_en, fin_en, paginas_ia) "
                + "VALUES (?, now() - interval '1 day', now() - interval '1 day' + interval '168 hours', 3)",
                negocioDe(enPrueba));
        givenSuscripcion(negocioDe(gratuita), "GRATUITO", "ACTIVA", "now()", null);
        long suscripcionesAntes = contar("suscripcion");

        migrarHasta("34");

        assertThat(contar("suscripcion")).isEqualTo(suscripcionesAntes);
        assertThat(planDe(gratuita)).isEqualTo("GRATUITO/ACTIVA");
        assertThat(planDe(premium)).isEqualTo("PREMIUM/ACTIVA");
        assertThat(planDe(enPrueba)).isEqualTo("PREMIUM/PRUEBA");
        assertThat(jdbcTemplate.queryForObject("SELECT paginas_ia FROM " + ESQUEMA
                + ".prueba_premium WHERE usuario_id = ?", Integer.class, enPrueba)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".renovacion_premium WHERE usuario_id = ? AND estado = 'AUTORIZADA'", Long.class, premium))
                .isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".pago_premium WHERE usuario_id = ? AND estado = 'approved'", Long.class, premium)).isOne();
    }

    @Test
    @DisplayName("El límite de proyectos pasa a ser de negocios: Gratuito 1 y Premium el propio más cinco")
    void migrateShouldTurnProjectLimitIntoBusinessLimit() {
        migrarHasta("33");

        migrarHasta("34");

        assertThat(jdbcTemplate.queryForObject("SELECT limite_negocios FROM " + ESQUEMA
                + ".plan WHERE codigo = 'GRATUITO'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT limite_negocios FROM " + ESQUEMA
                + ".plan WHERE codigo = 'PREMIUM'", Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = 'plan' AND column_name = 'limite_proyectos'",
                Long.class, ESQUEMA)).isZero();
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE " + ESQUEMA
                + ".plan SET limite_negocios = NULL WHERE codigo = 'PREMIUM'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una cuenta con más de un negocio hereda el plan del más antiguo y descarta lo derivado del otro")
    void migrateShouldKeepThePlanOfTheOldestNegocio() {
        migrarHasta("33");
        UUID cuenta = givenCuenta("dos-negocios");
        UUID antiguo = givenNegocio(cuenta, "Antiguo", "2026-01-01T00:00:00Z");
        UUID nuevo = givenNegocio(cuenta, "Nuevo", "2026-06-01T00:00:00Z");
        givenSuscripcion(antiguo, "PREMIUM", "ACTIVA", "now() - interval '5 days'", "now() + interval '25 days'");
        givenSuscripcion(nuevo, "GRATUITO", "ACTIVA", "now()", null);

        migrarHasta("34");

        assertThat(planDe(cuenta)).isEqualTo("PREMIUM/ACTIVA");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".suscripcion WHERE usuario_id = ?", Long.class, cuenta)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".negocio WHERE duenio_id = ?", Long.class, cuenta)).isEqualTo(2);
    }

    @Test
    @DisplayName("Una cuenta con negocio pero sin suscripción arranca en Gratuito, como al registrarse")
    void migrateShouldGiveFreePlanToAccountsWithoutSubscription() {
        migrarHasta("33");
        UUID cuenta = givenCuentaConNegocio("sin-suscripcion");

        migrarHasta("34");

        assertThat(planDe(cuenta)).isEqualTo("GRATUITO/ACTIVA");
    }

    @Test
    @DisplayName("Si hay cobros en un negocio que no es el actual de la cuenta la migración se detiene sin cambiar nada")
    void migrateShouldStopBeforeLosingPaymentData() {
        migrarHasta("33");
        UUID cuenta = givenCuenta("cobro-ajeno");
        givenNegocio(cuenta, "Antiguo", "2026-01-01T00:00:00Z");
        UUID nuevo = givenNegocio(cuenta, "Nuevo", "2026-06-01T00:00:00Z");
        givenSuscripcion(nuevo, "PREMIUM", "ACTIVA", "now() - interval '5 days'", "now() + interval '25 days'");
        givenRenovacionYPago(nuevo);

        assertThatThrownBy(() -> migrarHasta("34")).isInstanceOf(FlywayException.class)
                .hasMessageContaining("renovaciones o pagos Premium");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".renovacion_premium WHERE negocio_id = ?", Long.class, nuevo)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = 'suscripcion' AND column_name = 'negocio_id'",
                Long.class, ESQUEMA)).isOne();
    }

    @Test
    @DisplayName("Después de V34 la cuenta tiene una sola suscripción y borrar un negocio no la elimina")
    void migrateShouldEnforceOneSubscriptionPerAccountThatSurvivesNegocioDeletion() {
        migrarHasta("33");
        UUID cuenta = givenCuentaConNegocio("restricciones");
        givenSuscripcion(negocioDe(cuenta), "GRATUITO", "ACTIVA", "now()", null);

        migrarHasta("34");

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".suscripcion "
                + "(usuario_id, plan_id, estado, inicio_en) SELECT ?, id, 'ACTIVA', now() FROM " + ESQUEMA
                + ".plan WHERE codigo = 'GRATUITO'", cuenta)).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("DELETE FROM " + ESQUEMA + ".negocio WHERE duenio_id = ?", cuenta);
        assertThat(planDe(cuenta)).isEqualTo("GRATUITO/ACTIVA");
        jdbcTemplate.update("DELETE FROM " + ESQUEMA + ".usuario WHERE id = ?", cuenta);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA
                + ".suscripcion WHERE usuario_id = ?", Long.class, cuenta)).isZero();
    }

    // --- arrange ---
    private void migrarHasta(String version) {
        Flyway.configure().dataSource(url, usuario, contrasenia).schemas(ESQUEMA).defaultSchema(ESQUEMA)
                .target(version).load().migrate();
    }

    private UUID givenCuenta(String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".usuario (id, email, nombre) VALUES (?, ?, ?)",
                id, nombre + "-h55@example.test", nombre);
        return id;
    }

    private UUID givenNegocio(UUID cuenta, String nombre, String creadoEn) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".negocio (id, nombre, duenio_id, creado_en) "
                + "VALUES (?, ?, ?, ?::timestamptz)", id, nombre, cuenta, creadoEn);
        return id;
    }

    private UUID givenCuentaConNegocio(String nombre) {
        UUID cuenta = givenCuenta(nombre);
        givenNegocio(cuenta, "Negocio de " + nombre, "2026-01-01T00:00:00Z");
        return cuenta;
    }

    private UUID negocioDe(UUID cuenta) {
        return jdbcTemplate.queryForObject("SELECT id FROM " + ESQUEMA
                + ".negocio WHERE duenio_id = ? ORDER BY creado_en, id LIMIT 1", UUID.class, cuenta);
    }

    private void givenSuscripcion(UUID negocio, String plan, String estado, String inicio, String fin) {
        jdbcTemplate.update("DELETE FROM " + ESQUEMA + ".suscripcion WHERE negocio_id = ?", negocio);
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".suscripcion (negocio_id, plan_id, estado, inicio_en, fin_en) "
                + "SELECT ?, id, ?, " + inicio + ", " + (fin == null ? "NULL" : fin) + " FROM " + ESQUEMA
                + ".plan WHERE codigo = ?", negocio, estado, plan);
    }

    private void givenRenovacionYPago(UUID negocio) {
        UUID renovacion = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".renovacion_premium (id, negocio_id, proveedor_id, enlace, "
                + "estado, importe, moneda, primer_cobro_en, creado_en, actualizado_en) "
                + "VALUES (?, ?, ?, 'https://mp.test/a', 'AUTORIZADA', 14900, 'ARS', now(), now(), now())",
                renovacion, negocio, "mp-" + renovacion);
        jdbcTemplate.update("INSERT INTO " + ESQUEMA + ".pago_premium (factura_id, negocio_id, renovacion_id, pago_id, "
                + "estado, inicio_en, fin_en, modificado_proveedor_en, actualizado_en) "
                + "VALUES (?, ?, ?, ?, 'approved', now(), now() + interval '720 hours', now(), now())",
                "fac-" + renovacion, negocio, renovacion, "pago-" + renovacion);
    }

    // --- assert ---
    private long contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + ESQUEMA + "." + tabla, Long.class);
    }

    private String planDe(UUID cuenta) {
        return jdbcTemplate.queryForObject("SELECT p.codigo || '/' || s.estado FROM " + ESQUEMA
                + ".suscripcion s JOIN " + ESQUEMA + ".plan p ON p.id = s.plan_id WHERE s.usuario_id = ?",
                String.class, cuenta);
    }
}
