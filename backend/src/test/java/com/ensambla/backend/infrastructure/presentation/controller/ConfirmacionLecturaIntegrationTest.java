package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfirmacionLecturaIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String CONFIRMAR = "/api/lotes/{id}/confirmar";

    private static final String CUERPO_NUEVO = """
            {
              "nombreProyecto": "Ventas",
              "nombreBloque": "Hoja 1",
              "campos": [
                {"clave": "monto", "nombre": "Monto", "tipo": "NUMERO"},
                {"clave": "pagado", "nombre": "Pagado", "tipo": "SI_NO"}
              ],
              "filas": [
                {"pagina": 1, "valores": {"monto": 850, "pagado": true}},
                {"pagina": null, "valores": {"monto": 100}}
              ]
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private ArchivoCuadernoPort archivoCuadernoPort;

    private MockMvc mockMvc;
    private UUID usuarioId;
    private UUID negocioId;
    private UUID loteId;
    private UUID fotoId;
    private MockHttpSession sesion;
    private String csrfToken;
    private final List<String> clavesGuardadas = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Negocio', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(negocioId, OffsetDateTime.now().minusDays(1));
        fotoId = UUID.randomUUID();
        loteId = givenLote("LISTO", fotoId);
        sesion = givenSesion(usuarioId);
    }

    @AfterEach
    void tearDown() {
        clavesGuardadas.forEach(clave -> archivoCuadernoPort.eliminar(negocioId, clave));
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Confirmar sin sesión devuelve 401")
    void confirmarShouldRequireSession() throws Exception {
        mockMvc.perform(post(CONFIRMAR, loteId).contentType(MediaType.APPLICATION_JSON).content(CUERPO_NUEVO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Confirmar sin CSRF devuelve 403")
    void confirmarShouldRequireCsrf() throws Exception {
        mockMvc.perform(post(CONFIRMAR, loteId).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_NUEVO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crea proyecto, tabla, columnas y registros con su foto y deja el lote confirmado")
    void confirmarShouldCreateEverythingAndConfirmLote() throws Exception {
        ResultActions result = confirmar(loteId, CUERPO_NUEVO);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.registros").value(2))
                .andExpect(jsonPath("$.proyectoId").isNotEmpty())
                .andExpect(jsonPath("$.bloqueId").isNotEmpty());
        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(1);
        assertThat(contar("campo")).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(2);
        assertThat(contarConFoto(fotoId)).isEqualTo(1);
        assertThat(estadoDelLote(loteId)).isEqualTo("CONFIRMADO");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT confirmado_en IS NOT NULL AND bloque_destino_id IS NOT NULL FROM lote_subida WHERE id = ?",
                Boolean.class, loteId)).isTrue();
    }

    @Test
    @DisplayName("Rechaza por cuota todas las filas de una foto y revierte la estructura recién creada")
    void confirmarShouldRollbackNewStructureWhenRecordQuotaIsExceeded() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Sistema existente')",
                proyectoId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Inicial')",
                bloqueId, negocioId, proyectoId);
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 999)
                """, negocioId, bloqueId);
        String cuerpo = CUERPO_NUEVO.replace("\"nombreProyecto\": \"Ventas\"",
                "\"proyectoId\": \"" + proyectoId + "\"");

        confirmar(loteId, cuerpo).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("LIMITE_REGISTROS"));

        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(1);
        assertThat(contar("campo")).isZero();
        assertThat(contar("registro")).isEqualTo(999);
        assertThat(estadoDelLote(loteId)).isEqualTo("LISTO");
    }

    @Test
    @DisplayName("Después de confirmar la lectura del lote figura como confirmada")
    void obtenerShouldReportConfirmedAfterConfirmar() throws Exception {
        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isCreated());

        mockMvc.perform(get("/api/lotes/{id}/lectura", loteId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("Confirmar dos veces el mismo lote responde 400 y no duplica nada")
    void confirmarShouldRejectSecondConfirmation() throws Exception {
        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isCreated());

        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este cuaderno ya se guardó"));

        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("registro")).isEqualTo(2);
    }

    @Test
    @DisplayName("Si una fila es inválida no queda proyecto, tabla ni registros y el lote sigue listo")
    void confirmarShouldRollbackEverythingWhenRowIsInvalid() throws Exception {
        String cuerpo = CUERPO_NUEVO.replace("\"monto\": 100", "\"monto\": \"mucho\"");

        confirmar(loteId, cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['filas[1].monto']").value("Tiene que ser un número"));

        assertThat(contar("proyecto")).isZero();
        assertThat(contar("bloque")).isZero();
        assertThat(contar("campo")).isZero();
        assertThat(contar("registro")).isZero();
        assertThat(estadoDelLote(loteId)).isEqualTo("LISTO");
    }

    @Test
    @DisplayName("Suma las filas de otro lote a la tabla ya creada reutilizando su columna")
    void confirmarShouldAddRowsToExistingBloque() throws Exception {
        ResultActions primera = confirmar(loteId, CUERPO_NUEVO);
        String bloqueId = JsonPath.read(primera.andReturn().getResponse().getContentAsString(), "$.bloqueId");
        UUID campoMonto = jdbcTemplate.queryForObject(
                "SELECT id FROM campo WHERE bloque_id = ?::uuid AND nombre = 'Monto'", UUID.class, bloqueId);
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());
        String cuerpo = """
                {
                  "bloqueId": "%s",
                  "campos": [{"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"}],
                  "filas": [{"pagina": 1, "valores": {"monto": 40}}]
                }
                """.formatted(bloqueId, campoMonto);

        confirmar(otroLote, cuerpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueId").value(bloqueId))
                .andExpect(jsonPath("$.registros").value(1));

        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("registro")).isEqualTo(3);
        assertThat(estadoDelLote(otroLote)).isEqualTo("CONFIRMADO");
    }

    @Test
    @DisplayName("Crea la tabla dentro de un proyecto existente")
    void confirmarShouldCreateBloqueInExistingProyecto() throws Exception {
        ResultActions primera = confirmar(loteId, CUERPO_NUEVO);
        String proyectoId = JsonPath.read(primera.andReturn().getResponse().getContentAsString(), "$.proyectoId");
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());
        String cuerpo = """
                {
                  "proyectoId": "%s",
                  "nombreBloque": "Hoja 2",
                  "campos": [{"clave": "nota", "nombre": "Nota", "tipo": "TEXTO"}],
                  "filas": [{"pagina": 1, "valores": {"nota": "hola"}}]
                }
                """.formatted(proyectoId);

        confirmar(otroLote, cuerpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.proyectoId").value(proyectoId));

        assertThat(contar("proyecto")).isEqualTo(1);
        assertThat(contar("bloque")).isEqualTo(2);
    }

    @Test
    @DisplayName("Un lote que todavía no terminó de leerse no se puede confirmar")
    void confirmarShouldRejectLoteThatIsStillPending() throws Exception {
        UUID pendiente = givenLote("PENDIENTE", UUID.randomUUID());

        confirmar(pendiente, CUERPO_NUEVO).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este cuaderno todavía no terminó de leerse"));
    }

    @Test
    @DisplayName("Confirmar un lote de otro negocio devuelve 404 y no crea nada")
    void confirmarShouldHideForeignLote() throws Exception {
        UUID otroUsuario = UUID.randomUUID();
        UUID otroNegocio = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otra persona')",
                otroUsuario, otroUsuario + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocio, otroUsuario);
        MockHttpSession sesionAjena = givenSesion(otroUsuario);

        try {
            mockMvc.perform(post(CONFIRMAR, loteId).session(sesionAjena).header("X-CSRF-TOKEN", csrfToken)
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_NUEVO))
                    .andExpect(status().isNotFound());
            assertThat(contar("proyecto")).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", otroNegocio);
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", otroUsuario);
        }
    }

    @Test
    @DisplayName("Sirve la foto de un lote del negocio con su tipo")
    void fotoShouldServeFotoOfOwnLote() throws Exception {
        mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, fotoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));
    }

    @Test
    @DisplayName("Pedir una foto sin sesión devuelve 401")
    void fotoShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, fotoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una foto que no es del lote devuelve 404")
    void fotoShouldReturn404WhenFotoIsNotInLote() throws Exception {
        mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La foto de un lote de otro negocio devuelve 404")
    void fotoShouldHideFotoOfForeignNegocio() throws Exception {
        UUID otroUsuario = UUID.randomUUID();
        UUID otroNegocio = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otra persona')",
                otroUsuario, otroUsuario + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocio, otroUsuario);
        MockHttpSession sesionAjena = givenSesion(otroUsuario);

        try {
            mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", loteId, fotoId).session(sesionAjena))
                    .andExpect(status().isNotFound());
        } finally {
            jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", otroNegocio);
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", otroUsuario);
        }
    }

    @Test
    @DisplayName("Un registro confirmado expone su foto de origen y se puede ver")
    void fotoOrigenShouldServeFotoOfConfirmedRegistro() throws Exception {
        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isCreated());
        UUID registroId = registroConFoto();

        mockMvc.perform(get("/api/registros/{id}", registroId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoOrigenId").value(fotoId.toString()));
        mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));
    }

    @Test
    @DisplayName("Un registro cargado a mano no tiene foto de origen y devuelve 404")
    void fotoOrigenShouldReturn404WhenRegistroHasNoFoto() throws Exception {
        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isCreated());
        UUID registroId = jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? AND foto_origen_id IS NULL", UUID.class, negocioId);

        mockMvc.perform(get("/api/registros/{id}", registroId).session(sesion))
                .andExpect(jsonPath("$.fotoOrigenId").isEmpty());
        mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La foto de origen de un registro de otro negocio devuelve 404")
    void fotoOrigenShouldHideRegistroOfForeignNegocio() throws Exception {
        confirmar(loteId, CUERPO_NUEVO).andExpect(status().isCreated());
        UUID registroId = registroConFoto();
        UUID otroUsuario = UUID.randomUUID();
        UUID otroNegocio = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otra persona')",
                otroUsuario, otroUsuario + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocio, otroUsuario);
        MockHttpSession sesionAjena = givenSesion(otroUsuario);

        try {
            mockMvc.perform(get("/api/registros/{id}/foto-origen", registroId).session(sesionAjena))
                    .andExpect(status().isNotFound());
        } finally {
            jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", otroNegocio);
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", otroUsuario);
        }
    }

    @Test
    @DisplayName("Un body sin columnas devuelve 400")
    void confirmarShouldRejectBodyWithoutColumns() throws Exception {
        confirmar(loteId, "{\"nombreProyecto\": \"Ventas\", \"nombreBloque\": \"Hoja\", \"filas\": []}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Suma las filas a una tabla existente, le agrega la columna nueva y no toca los registros previos")
    void confirmarShouldAddNewFieldAndRowsToExistingBloque() throws Exception {
        BloqueExistente bloque = givenBloqueConfirmado();
        List<Map<String, Object>> antes = registrosDe(bloque.id());
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());

        confirmar(otroLote, cuerpoConTelefono(bloque, "40", "11 4444-5555"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueId").value(bloque.id()))
                .andExpect(jsonPath("$.registros").value(2));

        assertThat(contar("campo")).isEqualTo(3);
        UUID telefono = jdbcTemplate.queryForObject(
                "SELECT id FROM campo WHERE bloque_id = ?::uuid AND nombre = 'Teléfono'", UUID.class, bloque.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT posicion FROM campo WHERE id = ?", Integer.class, telefono)).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM registro WHERE bloque_id = ?::uuid AND valores ->> ? = '11 4444-5555'
                """, Integer.class, bloque.id(), telefono.toString())).isEqualTo(2);
        assertThat(registrosDe(bloque.id()).stream().filter(antes::contains).toList())
                .containsExactlyInAnyOrderElementsOf(antes);
        assertThat(estadoDelLote(otroLote)).isEqualTo("CONFIRMADO");
    }

    @Test
    @DisplayName("Si una fila es inválida no queda la columna nueva ni los registros y el lote sigue listo")
    void confirmarShouldRollbackNewFieldWhenRowIsInvalid() throws Exception {
        BloqueExistente bloque = givenBloqueConfirmado();
        List<Map<String, Object>> antes = registrosDe(bloque.id());
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());

        confirmar(otroLote, cuerpoConTelefono(bloque, "\"mucho\"", "11 4444-5555"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['filas[1].monto']").value("Tiene que ser un número"));

        assertThat(contar("campo")).isEqualTo(2);
        assertThat(registrosDe(bloque.id())).containsExactlyInAnyOrderElementsOf(antes);
        assertThat(estadoDelLote(otroLote)).isEqualTo("LISTO");
    }

    @Test
    @DisplayName("Si una columna nueva repite el nombre de un campo no se crea ninguna de las nuevas")
    void confirmarShouldRollbackEveryNewFieldWhenOneNameIsTaken() throws Exception {
        BloqueExistente bloque = givenBloqueConfirmado();
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());
        String cuerpo = """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "telefono", "nombre": "Teléfono", "tipo": "TEXTO"},
                    {"clave": "importe", "nombre": " monto ", "tipo": "NUMERO"}
                  ],
                  "filas": [{"pagina": 1, "valores": {"monto": 40, "telefono": "1", "importe": 5}}]
                }
                """.formatted(bloque.id(), bloque.campoMonto());

        confirmar(otroLote, cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['campos[2].nombre']").value("Ya hay un campo llamado \"monto\" en este bloque"));

        assertThat(contar("campo")).isEqualTo(2);
        assertThat(contar("registro")).isEqualTo(2);
        assertThat(estadoDelLote(otroLote)).isEqualTo("LISTO");
    }

    @Test
    @DisplayName("Dos columnas que apuntan al mismo campo de la tabla se rechazan y no se guarda nada")
    void confirmarShouldRejectTwoColumnsForTheSameField() throws Exception {
        BloqueExistente bloque = givenBloqueConfirmado();
        UUID otroLote = givenLote("LISTO", UUID.randomUUID());
        String cuerpo = """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "importe", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"}
                  ],
                  "filas": [{"pagina": 1, "valores": {"monto": 40, "importe": 5}}]
                }
                """.formatted(bloque.id(), bloque.campoMonto(), bloque.campoMonto());

        confirmar(otroLote, cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dos columnas no pueden guardarse en el mismo campo de la tabla"));

        assertThat(contar("registro")).isEqualTo(2);
        assertThat(estadoDelLote(otroLote)).isEqualTo("LISTO");
    }

    // --- arrange ---
    private record BloqueExistente(String id, UUID campoMonto) {
    }

    /** Una tabla que ya tiene datos: la que dejaría una primera lectura confirmada (Monto y Pagado, 2 registros). */
    private BloqueExistente givenBloqueConfirmado() throws Exception {
        String bloqueId = JsonPath.read(
                confirmar(loteId, CUERPO_NUEVO).andReturn().getResponse().getContentAsString(), "$.bloqueId");
        UUID campoMonto = jdbcTemplate.queryForObject(
                "SELECT id FROM campo WHERE bloque_id = ?::uuid AND nombre = 'Monto'", UUID.class, bloqueId);
        return new BloqueExistente(bloqueId, campoMonto);
    }

    /** Dos filas que suman a la tabla y traen la columna nueva «Teléfono» (el monto de la segunda se pasa tal cual). */
    private String cuerpoConTelefono(BloqueExistente bloque, String montoSegundaFila, String telefono) {
        return """
                {
                  "bloqueId": "%s",
                  "campos": [
                    {"clave": "monto", "campoId": "%s", "nombre": "Monto", "tipo": "NUMERO"},
                    {"clave": "telefono", "nombre": "Teléfono", "tipo": "TEXTO"}
                  ],
                  "filas": [
                    {"pagina": 1, "valores": {"monto": 20, "telefono": "%s"}},
                    {"pagina": 1, "valores": {"monto": %s, "telefono": "%s"}}
                  ]
                }
                """.formatted(bloque.id(), bloque.campoMonto(), telefono, montoSegundaFila, telefono);
    }

    private UUID givenLote(String estado, UUID foto) {
        UUID id = UUID.randomUUID();
        String clave = negocioId + "/fotos/" + foto + ".jpg";
        jdbcTemplate.update("INSERT INTO lote_subida (id, negocio_id, estado) VALUES (?, ?, ?)", id, negocioId, estado);
        jdbcTemplate.update("""
                INSERT INTO foto_cuaderno (id, negocio_id, lote_id, clave_almacenamiento, tipo_contenido,
                                           tamanio_bytes, posicion)
                VALUES (?, ?, ?, ?, 'image/jpeg', ?, 0)
                """, foto, negocioId, id, clave, JPEG.length);
        archivoCuadernoPort.guardar(negocioId, clave, JPEG);
        clavesGuardadas.add(clave);
        return id;
    }

    private MockHttpSession givenSesion(UUID usuario) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonPath.read(body, "$.token");
        return session;
    }

    // --- act ---
    private ResultActions confirmar(UUID lote, String cuerpo) throws Exception {
        return mockMvc.perform(post(CONFIRMAR, lote).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // --- assert ---
    private int contar(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + tabla + " WHERE negocio_id = ?", Integer.class, negocioId);
    }

    private List<Map<String, Object>> registrosDe(String bloqueId) {
        return jdbcTemplate.queryForList(
                "SELECT id::text AS id, valores::text AS valores, actualizado_en::text AS actualizado_en "
                        + "FROM registro WHERE bloque_id = ?::uuid ORDER BY id", bloqueId);
    }

    private int contarConFoto(UUID foto) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM registro WHERE foto_origen_id = ?", Integer.class, foto);
    }

    private UUID registroConFoto() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? AND foto_origen_id = ?", UUID.class, negocioId, fotoId);
    }

    private String estadoDelLote(UUID lote) {
        return jdbcTemplate.queryForObject("SELECT estado FROM lote_subida WHERE id = ?", String.class, lote);
    }
}
