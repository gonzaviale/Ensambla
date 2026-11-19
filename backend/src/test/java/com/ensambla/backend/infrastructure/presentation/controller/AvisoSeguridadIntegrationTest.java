package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AvisoSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    @Autowired
    private IRegistroJpaRepository registros;

    @Autowired
    private IAvisoJpaRepository avisos;

    private MockMvc mockMvc;
    private UUID negocioId;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID avisoId;
    private UUID registroId;
    private UUID precioId;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = crearUsuario();
        negocioId = crearNegocio(usuarioId);
        sesion = crearSesion(usuarioId);
    }

    @Test
    @DisplayName("Listar devuelve sólo los avisos del negocio de la sesión, los pendientes primero")
    void listarDevuelveSoloLosAvisosDelNegocioDeLaSesionLosPendientesPrimero() throws Exception {
        dadoUnAvisoDeOtroNegocio();
        dadoUnAvisoPropio("descartado", EstadoAviso.DESCARTADO);
        dadoUnAvisoPropio("propio", EstadoAviso.PENDIENTE);

        cuandoSeListanLosAvisos();

        entoncesSeListanSoloLosAvisosPropiosConLosPendientesPrimero();
    }

    @Test
    @DisplayName("El contador cuenta los pendientes propios y cuántos son urgentes")
    void elContadorCuentaLosPendientesPropiosYCuantosSonUrgentes() throws Exception {
        dadoUnAvisoDeOtroNegocio();
        dadoUnAvisoPropio("uno", EstadoAviso.PENDIENTE);
        dadoUnAvisoPropio("dos", EstadoAviso.DESCARTADO);

        cuandoSePideElContadorDeAvisos();

        entoncesSeCuentaUnPendienteUrgente();
    }

    @Test
    @DisplayName("Aplicar cambia el precio del producto y cierra el aviso en una sola operación")
    void aplicarCambiaElPrecioDelProductoYCierraElAvisoEnUnaSolaOperacion() throws Exception {
        dadoUnAvisoQueCambiaElPrecioDeUnProducto();

        cuandoSeAplicaElAviso();

        entoncesElAvisoQuedaAplicado();
        entoncesElProductoTieneElPrecioNuevo();
    }

    @Test
    @DisplayName("Si la acción falla el aviso sigue pendiente")
    void siLaAccionFallaElAvisoSigueSiendoPendiente() throws Exception {
        dadoUnAvisoConUnaAccionRota();

        cuandoSeAplicaElAviso();

        entoncesSeRechazaElPedido();
        entoncesElAvisoSigueSiendoPendiente();
    }

    @Test
    @DisplayName("Aplicar un aviso ajeno devuelve 404 y no lo toca")
    void aplicarUnAvisoAjenoDevuelve404YNoLoToca() throws Exception {
        dadoUnAvisoDeOtroNegocio();

        cuandoSeAplicaElAviso();

        entoncesRespondeNoEncontrado();
        entoncesElAvisoSigueSiendoPendiente();
    }

    @Test
    @DisplayName("Descartar deja el aviso descartado con su fecha de resolución")
    void descartarDejaElAvisoDescartadoConSuFechaDeResolucion() throws Exception {
        dadoUnAvisoPropio("propio", EstadoAviso.PENDIENTE);

        cuandoSeDescartaElAviso();

        entoncesElAvisoQuedaDescartadoConSuFechaDeResolucion();
    }

    @Test
    @DisplayName("Descartar dos veces el mismo aviso devuelve 400")
    void descartarDosVecesElMismoAvisoDevuelve400() throws Exception {
        dadoUnAvisoPropio("propio", EstadoAviso.DESCARTADO);

        cuandoSeDescartaElAviso();

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("Descartar sin CSRF devuelve 403")
    void descartarSinCsrfDevuelve403() throws Exception {
        dadoUnAvisoPropio("propio", EstadoAviso.PENDIENTE);

        cuandoSeDescartaElAvisoSinCsrf();

        entoncesSeProhibe();
    }

    @Test
    @DisplayName("Listar avisos exige una sesión válida")
    void listarAvisosExigeUnaSesionValida() throws Exception {
        cuandoSeListanLosAvisosSinUsuario();

        entoncesNoAutoriza();
    }

    // --- dado ---

    private void dadoUnAvisoDeOtroNegocio() {
        UUID ajeno = crearNegocio(crearUsuario());
        avisoId = crearAviso(ajeno, "ajeno", EstadoAviso.PENDIENTE, List.of());
    }

    private void dadoUnAvisoPropio(String nombre, EstadoAviso estado) {
        avisoId = crearAviso(negocioId, nombre, estado, List.of());
    }

    private void dadoUnAvisoQueCambiaElPrecioDeUnProducto() {
        UUID bloqueId = crearBloque();
        precioId = crearCampo(bloqueId, "Precio", "price");
        registroId = crearRegistro(bloqueId, Map.of(precioId.toString(), 3500));
        avisoId = crearAviso(negocioId, "precio", EstadoAviso.PENDIENTE, List.of(Map.of(
                "tipo", "EDITAR_REGISTRO", "registroId", registroId.toString(), "valores", Map.of("price", 4200))));
    }

    private void dadoUnAvisoConUnaAccionRota() {
        avisoId = crearAviso(negocioId, "roto", EstadoAviso.PENDIENTE, List.of(Map.of(
                "tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id", "valores", Map.of("price", 1))));
    }

    // --- cuando ---

    private void cuandoSeListanLosAvisos() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos").session(sesion));
    }

    private void cuandoSeListanLosAvisosSinUsuario() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos").session(crearSesionSinUsuario()));
    }

    private void cuandoSePideElContadorDeAvisos() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos/contador").session(sesion));
    }

    private void cuandoSeAplicaElAviso() throws Exception {
        respuesta = mockMvc.perform(post("/api/avisos/{id}/aplicar", avisoId)
                .session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeDescartaElAviso() throws Exception {
        respuesta = mockMvc.perform(post("/api/avisos/{id}/descartar", avisoId)
                .session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeDescartaElAvisoSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/avisos/{id}/descartar", avisoId).session(sesion));
    }

    // --- entonces ---

    private void entoncesSeListanSoloLosAvisosPropiosConLosPendientesPrimero() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].huella").doesNotExist())
                .andExpect(jsonPath("$[0].titulo").value("Aviso propio"))
                .andExpect(jsonPath("$[1].titulo").value("Aviso descartado"));
    }

    private void entoncesSeCuentaUnPendienteUrgente() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.urgentes").value(1));
    }

    private void entoncesElAvisoQuedaAplicado() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APLICADO"));
        assertThat(avisos.findById(avisoId)).get().extracting(AvisoEntity::getEstado).isEqualTo(EstadoAviso.APLICADO);
    }

    private void entoncesElProductoTieneElPrecioNuevo() {
        assertThat(registros.findById(registroId).orElseThrow().getValores().get(precioId.toString()))
                .isEqualTo(4200);
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesElAvisoSigueSiendoPendiente() {
        assertThat(avisos.findById(avisoId)).get().extracting(AvisoEntity::getEstado).isEqualTo(EstadoAviso.PENDIENTE);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesElAvisoQuedaDescartadoConSuFechaDeResolucion() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESCARTADO"))
                .andExpect(jsonPath("$.resueltoEn").exists());
        assertThat(avisos.findById(avisoId).orElseThrow().getResueltoEn()).isNotNull();
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    // --- datos ---

    private UUID crearUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearNegocio(UUID usuarioId) {
        UUID creado = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        suscripcionService.asignarGratuito(NegocioId.de(creado), AHORA.minusDays(1));
        return creado;
    }

    private UUID crearBloque() {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId).nombre("Inventario").creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId).proyectoId(proyectoId).nombre("Productos").tipo(TipoBloque.INVENTARIO)
                .posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearCampo(UUID bloqueId, String nombre, String clave) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).nombre(nombre).clave(clave).tipo(TipoCampo.MONEDA)
                .obligatorio(false).opciones(List.of()).minimo(BigDecimal.ZERO).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearRegistro(UUID bloqueId, Map<String, Object> valores) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).valores(new LinkedHashMap<>(valores))
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearAviso(UUID dueno, String nombre, EstadoAviso estado, List<Object> acciones) {
        boolean pendiente = estado == EstadoAviso.PENDIENTE;
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(dueno).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 3)
                .entidad("registro:" + nombre).huella("STOCK:registro:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(acciones).estado(estado)
                .resueltoEn(pendiente ? null : AHORA)
                .creadoEn(pendiente ? AHORA : AHORA.minusHours(1)).actualizadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession crearSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = crearSesionSinUsuario();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private MockHttpSession crearSesionSinUsuario() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String cuerpo = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(cuerpo, CsrfResponse.class).token();
        return session;
    }
}
