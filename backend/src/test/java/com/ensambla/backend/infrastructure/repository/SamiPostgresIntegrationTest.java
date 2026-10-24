package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSami;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.RespuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTaller;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.MensajeTallerItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSami;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sami contra la base real: las plantillas que nombra el diccionario existen y el proyecto se crea entero o no se crea. */
class SamiPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final UUID VENTAS_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a4");

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private ConversarConSami conversarConSami;

    @Autowired
    private CrearProyectoDesdeSami crearProyectoDesdeSami;

    @Autowired
    private ConversarEnTaller conversarEnTaller;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID usuarioId;
    private UUID negocioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba Sami')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba Sami', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(negocioId, OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("«Vendo velas y anoto quién me debe» propone la plantilla Ventas de la base con saldo pendiente")
    void conversarShouldProposeSeededVentasTemplate() {
        ConversarConSamiResponse response = conversarConSami.execute(
                new ConversarConSamiRequest(usuarioId, "Vendo velas y anoto quién me debe", List.of()));

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantillaId()).isEqualTo(VENTAS_ID);
        assertThat(response.propuesta().columnas()).extracting("nombre").containsExactly("Saldo pendiente");
    }

    @Test
    @DisplayName("Cada plantilla que nombra el diccionario existe en la base")
    void everyDictionaryTemplateShouldExistInTheDatabase() {
        List<String> nombres = jdbcTemplate.queryForList("SELECT nombre FROM plantilla WHERE activa", String.class);

        assertThat(nombres).contains("Ventas", "Inventario", "Pedidos", "Clientes", "Gastos");
    }

    @Test
    @DisplayName("Con un texto ambiguo pregunta y, tras responder, propone la plantilla elegida")
    void conversarShouldAskThenProposeAfterAnswer() {
        String texto = "quiero un stock y llevar los pedidos";

        ConversarConSamiResponse pregunta = conversarConSami.execute(
                new ConversarConSamiRequest(usuarioId, texto, List.of()));
        ConversarConSamiResponse propuesta = conversarConSami.execute(new ConversarConSamiRequest(
                usuarioId, texto, List.of(new RespuestaSamiItem(pregunta.pregunta().id(), "pedidos"))));

        assertThat(pregunta.tipo()).isEqualTo("PREGUNTA");
        assertThat(propuesta.tipo()).isEqualTo("PROPUESTA");
        assertThat(propuesta.propuesta().plantilla()).isEqualTo("Pedidos");
    }

    @Test
    @DisplayName("Con «otra cosa» consulta a la IA, que reserva y registra el consumo del negocio")
    void conversarShouldFallBackToAiAndRecordConsumption() {
        ConversarConSamiResponse response = conversarConSami.execute(new ConversarConSamiRequest(
                usuarioId, "quiero ordenar mi negocio", List.of(new RespuestaSamiItem("principal", "otra"))));

        assertThat(response.propuesta().origen()).isEqualTo("IA");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT consultas FROM consumo_sami_diario WHERE negocio_id = ?", Integer.class, negocioId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Un turno del taller pasa por la IA simulada, valida las herramientas y registra la consulta")
    void tallerShouldRunATurnAndRecordTheConsultation() {
        ConversarEnTallerResponse response = conversarEnTaller.execute(new ConversarEnTallerRequest(
                usuarioId, "sumá los gastos",
                new EstadoTallerItem("Mi almacén", "mosaic", List.of()),
                List.of(new MensajeTallerItem("sami", "Hola")), null));

        assertThat(response.acciones()).extracting("herramienta").containsExactly("agregar_pieza");
        assertThat(response.acciones().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT consultas FROM consumo_sami_diario WHERE negocio_id = ?", Integer.class, negocioId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Un turno del taller que no se entiende devuelve una botonera para elegir")
    void tallerShouldAskWithOptionsWhenItDoesNotUnderstand() {
        ConversarEnTallerResponse response = conversarEnTaller.execute(new ConversarEnTallerRequest(
                usuarioId, "ayuda", new EstadoTallerItem("", "mosaic", List.of()), List.of(), null));

        assertThat(response.preguntas().getFirst().opciones()).hasSizeBetween(2, 6);
        assertThat(response.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Crea el proyecto con los bloques de la plantilla más las columnas aceptadas")
    void crearProyectoShouldPersistTemplateAndExtraColumns() {
        var proyecto = crearProyectoDesdeSami.execute(new CrearProyectoDesdeSamiRequest(
                usuarioId, VENTAS_ID, "Velas de Ana", List.of(
                new ColumnaSamiItem("Saldo pendiente", "NUMERO", null),
                new ColumnaSamiItem("Forma de pago", "OPCION", List.of("Efectivo", "Tarjeta")))));

        assertThat(proyecto.nombre()).isEqualTo("Velas de Ana");
        List<String> campos = jdbcTemplate.queryForList("""
                SELECT c.nombre FROM campo c JOIN bloque b ON b.id = c.bloque_id
                WHERE b.proyecto_id = ? ORDER BY c.posicion
                """, String.class, proyecto.id());
        assertThat(campos).containsExactly(
                "Fecha", "Producto", "Cantidad", "Monto", "Cliente", "Saldo pendiente", "Forma de pago");
    }

    @Test
    @DisplayName("Si una columna falla no queda ningún proyecto del negocio")
    void crearProyectoShouldCreateNothingWhenAColumnFails() {
        assertThatThrownBy(() -> crearProyectoDesdeSami.execute(new CrearProyectoDesdeSamiRequest(
                usuarioId, VENTAS_ID, null, List.of(
                new ColumnaSamiItem("Saldo pendiente", "NUMERO", null),
                new ColumnaSamiItem(" ", "TEXTO", null)))))
                .isInstanceOf(CampoInvalidoException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM proyecto WHERE negocio_id = ?", Integer.class, negocioId)).isZero();
    }
}
