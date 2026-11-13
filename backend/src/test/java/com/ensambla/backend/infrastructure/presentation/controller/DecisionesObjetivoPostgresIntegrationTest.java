package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las decisiones de la persona frente a un objetivo de punta a punta con PostgreSQL: se anotan solas al aplicar o
 * descartar un aviso, se vinculan al mini-objetivo del producto y muestran su efecto cuando el impacto ya se midió.
 */
@Transactional
class DecisionesObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.now();

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IAvisoJpaRepository avisos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID negocioId;
    private UUID productoId;
    private String semanaDeHoy;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuario();
        negocioId = givenNegocio(usuarioId);
        productoId = UUID.randomUUID();
        sesion = givenSesion(usuarioId);
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now());
        semanaDeHoy = SemanaObjetivo.lunesDe(hoy).toString();
    }

    @Test
    @DisplayName("Aplicar un aviso deja la decisión en la semana de hoy, vinculada al mini-objetivo del producto")
    void aplicarShouldRecordTheDecisionInTheCurrentWeek() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID miniId = givenMini(objetivoId);
        UUID avisoId = givenAvisoEstancado();

        whenResolver("aplicar", avisoId);

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas.length()").value(1))
                .andExpect(jsonPath("$.semanas[0].semanaInicio").value(semanaDeHoy))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_APLICADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].avisoId").value(avisoId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].miniObjetivoId").value(miniId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].registroId").value(productoId.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].descripcion").value("Bajá el precio de la Yerba"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MIDIENDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").doesNotExist());
    }

    @Test
    @DisplayName("Cuando el impacto del aviso ya se midió la decisión trae su efecto en el ingreso semanal")
    void listarShouldShowTheEffectOnceTheImpactWasMeasured() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID avisoId = givenAvisoEstancado();
        whenResolver("aplicar", avisoId);
        givenImpactoMedido(avisoId);

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MEDIDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").value(350.0));
    }

    @Test
    @DisplayName("Descartar un aviso deja la decisión de descartado, sin medición")
    void descartarShouldRecordTheDiscardedDecision() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID avisoId = givenAvisoEstancado();

        whenResolver("descartar", avisoId);

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_DESCARTADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    @Test
    @DisplayName("La decisión se anota en cada objetivo activo y no en los que ya llegaron a su meta")
    void aplicarShouldRecordOneDecisionPerActiveObjetivo() throws Exception {
        UUID activo = givenObjetivo(negocioId, 500000);
        UUID otroActivo = givenObjetivo(negocioId, 600000);
        UUID alcanzado = givenObjetivo(negocioId, 100);
        jdbcTemplate.update("UPDATE objetivo SET alcanzado_en = now() WHERE id = ?", alcanzado);
        UUID avisoId = givenAvisoEstancado();

        whenResolver("aplicar", avisoId);

        assertThat(decisionesDe(activo)).isEqualTo(1);
        assertThat(decisionesDe(otroActivo)).isEqualTo(1);
        assertThat(decisionesDe(alcanzado)).isZero();
    }

    @Test
    @DisplayName("Sin objetivos aplicar un aviso funciona igual y no anota nada")
    void aplicarShouldStillWorkWithoutObjetivos() throws Exception {
        UUID avisoId = givenAvisoEstancado();

        whenResolver("aplicar", avisoId);

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE negocio_id = ?", Integer.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Aceptar el paso de un mini-objetivo responde 201 y queda en el historial")
    void decidirPasoShouldRecordTheAcceptedStep() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID miniId = givenMini(objetivoId);

        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"tipo\":\"ACEPTADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.miniObjetivoId").value(miniId.toString()));

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    @Test
    @DisplayName("Ignorar el paso es explícito: queda anotado como ignorado")
    void decidirPasoShouldRecordTheIgnoredStep() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID miniId = givenMini(objetivoId);

        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"tipo\":\"IGNORADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PASO_IGNORADO"));
    }

    @Test
    @DisplayName("Decidir el paso de un mini-objetivo que ya no está en curso responde 400")
    void decidirPasoShouldRejectAMiniObjetivoNoLongerInProgress() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID miniId = givenMini(objetivoId);
        MiniObjetivoEntity abandonado = minis.findById(miniId).orElseThrow();
        abandonado.setEstado(EstadoMiniObjetivo.ABANDONADO);
        minis.saveAndFlush(abandonado);

        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"tipo\":\"ACEPTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una decisión que no es de paso, o sin cuerpo válido, responde 400")
    void decidirPasoShouldRejectAnInvalidBody() throws Exception {
        UUID objetivoId = givenObjetivo(negocioId, 500000);
        UUID miniId = givenMini(objetivoId);

        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"tipo\":\"AVISO_APLICADO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", objetivoId, miniId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un objetivo o mini-objetivo de otro negocio responde 404 y no se anota nada")
    void shouldReturn404ForAnotherBusiness() throws Exception {
        UUID ajeno = givenObjetivoDeOtroNegocio();

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", ajeno).session(sesion))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", ajeno, UUID.randomUUID())
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content("{\"tipo\":\"ACEPTADO\"}"))
                .andExpect(status().isNotFound());
        assertThat(decisionesDe(ajeno)).isZero();
    }

    @Test
    @DisplayName("Sin sesión responde 401 y sin token CSRF decidir un paso responde 403")
    void shouldRequireSessionAndCsrf() throws Exception {
        mockMvc.perform(get("/api/objetivos/{id}/decisiones", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/objetivos/{id}/mini-objetivos/{mini}/decisiones", UUID.randomUUID(),
                        UUID.randomUUID())
                        .session(sesion).contentType("application/json").content("{\"tipo\":\"ACEPTADO\"}"))
                .andExpect(status().isForbidden());
    }

    // --- act ---
    private void whenResolver(String accion, UUID avisoId) throws Exception {
        mockMvc.perform(post("/api/avisos/{id}/" + accion, avisoId).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
    }

    // --- arrange ---
    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenNegocio(UUID usuarioId) {
        UUID creado = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creado), AHORA.minusDays(1));
        return creado;
    }

    private UUID givenObjetivo(UUID negocio, int meta) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Ganar más', ?)",
                id, negocio, meta);
        return id;
    }

    private UUID givenObjetivoDeOtroNegocio() {
        UUID otroNegocio = givenNegocio(givenUsuario());
        return givenObjetivo(otroNegocio, 400000);
    }

    /** Un mini-objetivo en curso del producto del aviso. */
    private UUID givenMini(UUID objetivoId) {
        return minis.saveAndFlush(MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) 1)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .registroId(productoId)
                .referencia("Yerba 1 kg")
                .meta(new BigDecimal("14183.00"))
                .valorInicial(new BigDecimal("12600.00"))
                .valorActual(new BigDecimal("12600.00"))
                .aporteEstimado(new BigDecimal("6786.00"))
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(LocalDate.parse(semanaDeHoy))
                .titulo("Subile el precio a Yerba 1 kg")
                .justificacion("Te deja más por unidad")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    /** Un aviso pendiente de «producto estancado» sobre el producto del mini-objetivo, sin acción que ejecutar. */
    private UUID givenAvisoEstancado() {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.ESTANCADO).grupo(GrupoAviso.VENTAS).prioridad((short) 2)
                .entidad("registro:" + productoId).huella("ESTANCADO:" + productoId + ":" + UUID.randomUUID())
                .titulo("Bajá el precio de la Yerba").detalle("No se vende hace semanas")
                .acciones(List.of()).estado(EstadoAviso.PENDIENTE)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    /** Lo que deja MedirImpactoAvisos cuando termina la ventana: el ingreso por día pasa de $100 a $150. */
    private void givenImpactoMedido(UUID avisoId) {
        AvisoEntity aviso = avisos.findById(avisoId).orElseThrow();
        Map<String, Object> impacto = new LinkedHashMap<>();
        impacto.put("ventanaDias", 14);
        impacto.put("unidadesPorDiaAntes", 2);
        impacto.put("unidadesPorDiaDespues", 3);
        impacto.put("ingresoPorDiaAntes", 100);
        impacto.put("ingresoPorDiaDespues", 150);
        Map<String, Object> datos = new LinkedHashMap<>(aviso.getDatos());
        datos.put("impacto", impacto);
        aviso.setDatos(datos);
        avisos.saveAndFlush(aviso);
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        return session;
    }

    // --- assert ---
    private int decisionesDe(UUID objetivoId) {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId);
    }
}
