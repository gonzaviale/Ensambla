package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;
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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuario();
        negocioId = givenNegocio(usuarioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Listar devuelve sólo los avisos del negocio de la sesión, los pendientes primero")
    void listarShouldReturnOnlyOwnAvisosPendingFirst() throws Exception {
        UUID ajeno = givenNegocio(givenUsuario());
        givenAviso(ajeno, "ajeno", EstadoAviso.PENDIENTE, List.of());
        givenAviso(negocioId, "descartado", EstadoAviso.DESCARTADO, List.of());
        givenAviso(negocioId, "propio", EstadoAviso.PENDIENTE, List.of());

        mockMvc.perform(get("/api/avisos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].huella").doesNotExist())
                .andExpect(jsonPath("$[0].titulo").value("Aviso propio"))
                .andExpect(jsonPath("$[1].titulo").value("Aviso descartado"));
    }

    @Test
    @DisplayName("El contador cuenta los pendientes propios y cuántos son urgentes")
    void contarShouldCountOwnPendingAvisos() throws Exception {
        givenAviso(givenNegocio(givenUsuario()), "ajeno", EstadoAviso.PENDIENTE, List.of());
        givenAviso(negocioId, "uno", EstadoAviso.PENDIENTE, List.of());
        givenAviso(negocioId, "dos", EstadoAviso.DESCARTADO, List.of());

        mockMvc.perform(get("/api/avisos/contador").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.urgentes").value(1));
    }

    @Test
    @DisplayName("Aplicar cambia el precio del producto y cierra el aviso en una sola operación")
    void aplicarShouldEditRegistroAndCloseAviso() throws Exception {
        UUID bloqueId = givenBloque();
        UUID precioId = givenCampo(bloqueId, "Precio", "price");
        UUID registroId = givenRegistro(bloqueId, Map.of(precioId.toString(), 3500));
        UUID avisoId = givenAviso(negocioId, "precio", EstadoAviso.PENDIENTE, List.of(Map.of(
                "tipo", "EDITAR_REGISTRO", "registroId", registroId.toString(), "valores", Map.of("price", 4200))));

        mockMvc.perform(post("/api/avisos/{id}/aplicar", avisoId).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APLICADO"));

        assertThat(avisos.findById(avisoId)).get().extracting(AvisoEntity::getEstado).isEqualTo(EstadoAviso.APLICADO);
        assertThat(registros.findById(registroId).orElseThrow().getValores().get(precioId.toString()))
                .isEqualTo(4200);
    }

    @Test
    @DisplayName("Si la acción falla el aviso sigue pendiente")
    void aplicarShouldLeaveAvisoPendingWhenActionFails() throws Exception {
        UUID avisoId = givenAviso(negocioId, "roto", EstadoAviso.PENDIENTE, List.of(Map.of(
                "tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id", "valores", Map.of("price", 1))));

        mockMvc.perform(post("/api/avisos/{id}/aplicar", avisoId).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isBadRequest());

        assertThat(avisos.findById(avisoId)).get().extracting(AvisoEntity::getEstado).isEqualTo(EstadoAviso.PENDIENTE);
    }

    @Test
    @DisplayName("Aplicar un aviso ajeno devuelve 404 y no lo toca")
    void aplicarShouldHideForeignAviso() throws Exception {
        UUID ajeno = givenNegocio(givenUsuario());
        UUID avisoId = givenAviso(ajeno, "ajeno", EstadoAviso.PENDIENTE, List.of());

        mockMvc.perform(post("/api/avisos/{id}/aplicar", avisoId).session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());

        assertThat(avisos.findById(avisoId)).get().extracting(AvisoEntity::getEstado).isEqualTo(EstadoAviso.PENDIENTE);
    }

    @Test
    @DisplayName("Descartar deja el aviso descartado con su fecha de resolución")
    void descartarShouldDiscardOwnAviso() throws Exception {
        UUID avisoId = givenAviso(negocioId, "propio", EstadoAviso.PENDIENTE, List.of());

        mockMvc.perform(post("/api/avisos/{id}/descartar", avisoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESCARTADO"))
                .andExpect(jsonPath("$.resueltoEn").exists());

        assertThat(avisos.findById(avisoId).orElseThrow().getResueltoEn()).isNotNull();
    }

    @Test
    @DisplayName("Descartar dos veces el mismo aviso devuelve 400")
    void descartarShouldRejectAlreadyResolvedAviso() throws Exception {
        UUID avisoId = givenAviso(negocioId, "propio", EstadoAviso.DESCARTADO, List.of());

        mockMvc.perform(post("/api/avisos/{id}/descartar", avisoId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Descartar sin CSRF devuelve 403")
    void descartarShouldRequireCsrf() throws Exception {
        UUID avisoId = givenAviso(negocioId, "propio", EstadoAviso.PENDIENTE, List.of());

        mockMvc.perform(post("/api/avisos/{id}/descartar", avisoId).session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listar avisos exige una sesión válida")
    void listarShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/avisos").session(givenSesionSinUsuario()))
                .andExpect(status().isUnauthorized());
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
        suscripcionService.asignarGratuito(creado, AHORA.minusDays(1));
        return creado;
    }

    private UUID givenBloque() {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId).nombre("Inventario").creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId).proyectoId(proyectoId).nombre("Productos").tipo(TipoBloque.INVENTARIO)
                .posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenCampo(UUID bloqueId, String nombre, String clave) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).nombre(nombre).clave(clave).tipo(TipoCampo.MONEDA)
                .obligatorio(false).opciones(List.of()).minimo(BigDecimal.ZERO).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenRegistro(UUID bloqueId, Map<String, Object> valores) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId).bloqueId(bloqueId).valores(new LinkedHashMap<>(valores))
                .creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenAviso(UUID dueno, String nombre, EstadoAviso estado, List<Object> acciones) {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(dueno).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK).prioridad((short) 3)
                .entidad("registro:" + nombre).huella("STOCK:registro:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(acciones).estado(estado)
                .resueltoEn(estado == EstadoAviso.PENDIENTE ? null : AHORA)
                .creadoEn(estado == EstadoAviso.PENDIENTE ? AHORA : AHORA.minusHours(1)).actualizadoEn(AHORA)
                .build()).getId();
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = givenSesionSinUsuario();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private MockHttpSession givenSesionSinUsuario() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        return session;
    }
}
