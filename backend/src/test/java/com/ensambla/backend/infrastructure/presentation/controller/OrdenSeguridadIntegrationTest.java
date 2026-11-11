package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.ItemDeOrden;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.whatsapp.MensajeriaWhatsappSimulada;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ClienteWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.OrdenCompraEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IClienteWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IOrdenCompraJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las órdenes de compra por HTTP contra PostgreSQL: aislamiento por negocio, CSRF y el descuento de stock al aceptar. */
@Transactional
class OrdenSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private ObjectMapper objectMapper;

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
    private IClienteWhatsappJpaRepository clientes;

    @Autowired
    private IOrdenCompraJpaRepository ordenes;

    @Autowired
    private MensajeriaWhatsappPort mensajeria;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID clienteId;
    private UUID campoStockId;
    private UUID yerbaId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(givenUsuario());
        negocioId = givenNegocio(usuarioId);
        sesion = givenSesion(usuarioId);
        clienteId = clientes.saveAndFlush(ClienteWhatsappEntity.builder()
                .telefono("549" + (1000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000L)))
                .creadoEn(AHORA).build()).getId();
        givenInventario(10);
    }

    @Test
    @DisplayName("La lista HTTP contiene sólo las órdenes del negocio de la sesión, con el WhatsApp del cliente y los ítems")
    void listarShouldIsolateBusinesses() throws Exception {
        UUID propia = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 3);
        givenOrden(givenNegocio(UsuarioId.de(givenUsuario())), EstadoOrdenCompra.PENDIENTE, 1);

        ResultActions result = mockMvc.perform(get("/api/ordenes").session(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propia.toString()))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].clienteTelefono").value(clientes.findById(clienteId).orElseThrow().getTelefono()))
                .andExpect(jsonPath("$[0].items[0].nombre").value("Yerba Playadito 1 kg"))
                .andExpect(jsonPath("$[0].items[0].cantidad").value(3))
                .andExpect(jsonPath("$[0].total").value(10500.0));
    }

    @Test
    @DisplayName("Filtrar por estado trae sólo las órdenes de ese estado")
    void listarShouldFilterByState() throws Exception {
        givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 1);
        UUID lista = givenOrden(negocioId, EstadoOrdenCompra.LISTA, 1);

        ResultActions result = mockMvc.perform(get("/api/ordenes?estado=LISTA").session(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(lista.toString()));
    }

    @Test
    @DisplayName("Aceptar una orden propia la deja aceptada y descuenta del inventario lo que pidió el cliente")
    void aceptarShouldDeductTheStock() throws Exception {
        UUID orden = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 3);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                .content("{\"estado\":\"ACEPTADA\"}"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.clienteAvisado").value(true));
        assertThat(stock()).isEqualByComparingTo("7");
        assertThat(avisosEnviadosA(clientes.findById(clienteId).orElseThrow().getTelefono()))
                .singleElement().satisfies(aviso -> assertThat(aviso).contains("aceptó tu pedido"));
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.ACEPTADA);
    }

    @Test
    @DisplayName("Si el stock ya no alcanza, aceptar responde 400 y no cambia ni la orden ni el inventario")
    void aceptarShouldFailWithoutEnoughStock() throws Exception {
        UUID orden = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 11);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                .content("{\"estado\":\"ACEPTADA\"}"));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No alcanza el stock de Yerba Playadito 1 kg: hay 10 y la orden pide 11"));
        assertThat(stock()).isEqualByComparingTo("10");
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    @DisplayName("Rechazar guarda el motivo y no toca el inventario")
    void rechazarShouldKeepTheReasonAndTheStock() throws Exception {
        UUID orden = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 3);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                .content("{\"estado\":\"RECHAZADA\",\"motivo\":\"No me queda stock\"}"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("No me queda stock"));
        assertThat(stock()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Un paso que no está permitido responde 400 y la orden queda como estaba")
    void cambiarEstadoShouldRejectAnInvalidStep() throws Exception {
        UUID orden = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 1);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                .content("{\"estado\":\"LISTA\"}"));

        result.andExpect(status().isBadRequest());
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    @DisplayName("Un PATCH sobre la orden de otro negocio responde 404 y la conserva intacta")
    void cambiarEstadoShouldNotTouchForeignOrders() throws Exception {
        UUID ajena = givenOrden(givenNegocio(UsuarioId.de(givenUsuario())), EstadoOrdenCompra.PENDIENTE, 1);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", ajena).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                .content("{\"estado\":\"ACEPTADA\"}"));

        result.andExpect(status().isNotFound());
        assertThat(ordenes.findById(ajena).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    @DisplayName("Cambiar el estado sin CSRF devuelve 403 y no cambia nada")
    void cambiarEstadoShouldRequireCsrf() throws Exception {
        UUID orden = givenOrden(negocioId, EstadoOrdenCompra.PENDIENTE, 1);

        ResultActions result = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden).session(sesion)
                .contentType("application/json").content("{\"estado\":\"ACEPTADA\"}"));

        result.andExpect(status().isForbidden());
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    @DisplayName("Listar sin sesión devuelve 401")
    void listarShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/ordenes").session(givenSesionSinUsuario())).andExpect(status().isUnauthorized());
    }

    // --- arrange ---

    /** Lo que Sami le mandó al cliente: en los tests el proveedor de WhatsApp es el simulado, que guarda lo que envía. */
    private List<String> avisosEnviadosA(String telefono) {
        return ((MensajeriaWhatsappSimulada) mensajeria).enviados().stream()
                .filter(enviado -> enviado.telefono().equals(telefono))
                .map(MensajeriaWhatsappSimulada.Enviado::texto)
                .toList();
    }

    private BigDecimal stock() {
        Object valor = registros.findById(yerbaId).orElseThrow().getValores().get(campoStockId.toString());
        return new BigDecimal(valor.toString());
    }

    private void givenInventario(int stock) {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder().negocioId(negocioId.valor())
                .nombre("Almacén").creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
        UUID bloqueId = bloques.saveAndFlush(BloqueEntity.builder().negocioId(negocioId.valor())
                .proyectoId(proyectoId).nombre("Inventario").posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
        campoStockId = campos.saveAndFlush(CampoEntity.builder().negocioId(negocioId.valor()).bloqueId(bloqueId)
                .nombre("Cantidad").tipo(TipoCampo.NUMERO).clave("quantity").obligatorio(false)
                .opciones(List.of()).posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
        yerbaId = registros.saveAndFlush(RegistroEntity.builder().negocioId(negocioId.valor()).bloqueId(bloqueId)
                .valores(Map.of(campoStockId.toString(), stock)).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    /** Una orden de Yerba Playadito a $3.500 la unidad; sólo las del negocio de la sesión apuntan al inventario real. */
    private UUID givenOrden(NegocioId negocio, EstadoOrdenCompra estado, int cantidad) {
        ItemDeOrden item = new ItemDeOrden(RegistroId.de(yerbaId), "Yerba Playadito 1 kg",
                BigDecimal.valueOf(cantidad), new BigDecimal("3500"));
        return ordenes.saveAndFlush(OrdenCompraEntity.builder()
                .negocioId(negocio.valor()).clienteId(clienteId).estado(estado)
                .items(objectMapper.writeValueAsString(List.of(item)))
                .total(item.subtotal()).creadoEn(AHORA).actualizadoEn(AHORA).build()).getId();
    }

    private UUID givenUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private NegocioId givenNegocio(UsuarioId usuarioId) {
        return NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId());
    }

    private MockHttpSession givenSesion(UsuarioId usuarioId) throws Exception {
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
