package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoAvisos;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoDeAviso;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ObtenerImpactoAvisos;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.aviso.AvisoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvisoControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final ListarAvisos listarAvisos = mock(ListarAvisos.class);
    private final ContarAvisos contarAvisos = mock(ContarAvisos.class);
    private final AplicarAviso aplicarAviso = mock(AplicarAviso.class);
    private final DescartarAviso descartarAviso = mock(DescartarAviso.class);
    private final ObtenerImpactoAvisos obtenerImpactoAvisos = mock(ObtenerImpactoAvisos.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        var controller = new AvisoController(listarAvisos, contarAvisos, aplicarAviso,
                descartarAviso, obtenerImpactoAvisos, new AvisoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los avisos del negocio con todos sus datos")
    void getListaLosAvisosDelNegocioConTodosSusDatos() throws Exception {
        dadoUnAvisoPendienteEnElNegocio();

        cuandoSeListanLosAvisos();

        entoncesSeListaElAvisoPendienteConTodosSusDatos();
    }

    @Test
    @DisplayName("GET impacto devuelve lo aplicado y lo que cambió en las ventas de cada aviso medido")
    void getImpactoDevuelveLoAplicadoYLoQueCambioEnLasVentasDeCadaAvisoMedido() throws Exception {
        dadoUnAvisoMedidoConImpacto();

        cuandoSePideElImpactoDeLosAvisos();

        entoncesSeDevuelveElImpactoDelAvisoMedido();
    }

    @Test
    @DisplayName("GET con estado lista sólo los avisos de ese estado")
    void getConEstadoListaSoloLosAvisosDeEseEstado() throws Exception {
        dadoUnAvisoDescartadoEnElNegocio();

        cuandoSeListanLosAvisosConEstado("DESCARTADO");

        entoncesSeListaElAvisoEnEstado("DESCARTADO");
    }

    @Test
    @DisplayName("GET con un estado que no existe devuelve 400")
    void getConUnEstadoQueNoExisteDevuelve400() throws Exception {
        cuandoSeListanLosAvisosConEstado("INVENTADO");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("GET contador devuelve los pendientes y los urgentes")
    void getContadorDevuelveLosPendientesYLosUrgentes() throws Exception {
        dadoCuatroAvisosPendientesYUnoUrgente();

        cuandoSePideElContadorDeAvisos();

        entoncesSeDevuelvenCuatroPendientesYUnUrgente();
    }

    @Test
    @DisplayName("POST aplicar devuelve el aviso aplicado")
    void postAplicarDevuelveElAvisoAplicado() throws Exception {
        dadoUnAvisoQueSePuedeAplicar();

        cuandoSeAplicaElAviso();

        entoncesSeDevuelveElAvisoEnEstado("APLICADO");
    }

    @Test
    @DisplayName("POST aplicar un aviso ya resuelto devuelve 400")
    void postAplicarUnAvisoYaResueltoDevuelve400() throws Exception {
        dadoUnAvisoYaResuelto();

        cuandoSeAplicaElAviso();

        entoncesSeRechazaPorque("El aviso ya fue resuelto");
    }

    @Test
    @DisplayName("POST descartar devuelve el aviso descartado")
    void postDescartarDevuelveElAvisoDescartado() throws Exception {
        dadoUnAvisoQueSePuedeDescartar();

        cuandoSeDescartaElAviso();

        entoncesSeDevuelveElAvisoEnEstado("DESCARTADO");
        entoncesSeDescartaElAvisoDelUsuario();
    }

    @Test
    @DisplayName("POST descartar un aviso ajeno devuelve 404")
    void postDescartarUnAvisoAjenoDevuelve404() throws Exception {
        dadoUnAvisoAjeno();

        cuandoSeDescartaElAviso();

        entoncesRespondeNoEncontrado();
    }

    // --- dado ---

    private void dadoUnAvisoPendienteEnElNegocio() {
        when(listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, null)))
                .thenReturn(List.of(unAviso(EstadoAviso.PENDIENTE)));
    }

    private void dadoUnAvisoDescartadoEnElNegocio() {
        when(listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.DESCARTADO)))
                .thenReturn(List.of(unAviso(EstadoAviso.DESCARTADO)));
    }

    private void dadoUnAvisoMedidoConImpacto() {
        ImpactoAviso impacto = new ImpactoAviso(14, new BigDecimal("1.00"), new BigDecimal("2.00"),
                new BigDecimal("10.00"), new BigDecimal("20.00"));
        when(obtenerImpactoAvisos.execute(USUARIO_ID)).thenReturn(new ImpactoAvisos(3, 1, new BigDecimal("70.00"),
                List.of(new ImpactoDeAviso(AVISO_ID, TipoAviso.ESTANCADO, "Mermelada cara", AHORA, impacto))));
    }

    private void dadoCuatroAvisosPendientesYUnoUrgente() {
        when(contarAvisos.execute(USUARIO_ID)).thenReturn(new ContadorAvisos(4, 1));
    }

    private void dadoUnAvisoQueSePuedeAplicar() {
        when(aplicarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenReturn(unAviso(EstadoAviso.APLICADO));
    }

    private void dadoUnAvisoYaResuelto() {
        when(aplicarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenThrow(new BusinessRuleException("El aviso ya fue resuelto"));
    }

    private void dadoUnAvisoQueSePuedeDescartar() {
        when(descartarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenReturn(unAviso(EstadoAviso.DESCARTADO));
    }

    private void dadoUnAvisoAjeno() {
        when(descartarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID)))
                .thenThrow(new ResourceNotFoundException("El aviso no existe"));
    }

    // --- cuando ---

    private void cuandoSeListanLosAvisos() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos").principal(principal));
    }

    private void cuandoSeListanLosAvisosConEstado(String estado) throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos").param("estado", estado).principal(principal));
    }

    private void cuandoSePideElImpactoDeLosAvisos() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos/impacto").principal(principal));
    }

    private void cuandoSePideElContadorDeAvisos() throws Exception {
        respuesta = mockMvc.perform(get("/api/avisos/contador").principal(principal));
    }

    private void cuandoSeAplicaElAviso() throws Exception {
        respuesta = mockMvc.perform(post("/api/avisos/{id}/aplicar", AVISO_ID).principal(principal));
    }

    private void cuandoSeDescartaElAviso() throws Exception {
        respuesta = mockMvc.perform(post("/api/avisos/{id}/descartar", AVISO_ID).principal(principal));
    }

    // --- entonces ---

    private void entoncesSeListaElAvisoPendienteConTodosSusDatos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$[0].tipo").value("STOCK"))
                .andExpect(jsonPath("$[0].grupo").value("STOCK"))
                .andExpect(jsonPath("$[0].prioridad").value(3))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].titulo").value("Te quedaste sin yerba"))
                .andExpect(jsonPath("$[0].motivos[0]").value("motivo"))
                .andExpect(jsonPath("$[0].acciones[0].tipo").value("ABRIR"))
                .andExpect(jsonPath("$[0].datos.cantidad").value(0));
    }

    private void entoncesSeDevuelveElImpactoDelAvisoMedido() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicados").value(3))
                .andExpect(jsonPath("$.descartados").value(1))
                .andExpect(jsonPath("$.ingresoSemanalDelta").value(70.00))
                .andExpect(jsonPath("$.medidos[0].avisoId").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.medidos[0].tipo").value("ESTANCADO"))
                .andExpect(jsonPath("$.medidos[0].ventanaDias").value(14))
                .andExpect(jsonPath("$.medidos[0].ingresoSemanalDelta").value(70.00));
    }

    private void entoncesSeListaElAvisoEnEstado(String estado) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value(estado));
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesSeDevuelvenCuatroPendientesYUnUrgente() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(4))
                .andExpect(jsonPath("$.urgentes").value(1));
    }

    private void entoncesSeDevuelveElAvisoEnEstado(String estado) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
    }

    private void entoncesSeRechazaPorque(String mensaje) throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensaje));
    }

    private void entoncesSeDescartaElAvisoDelUsuario() {
        verify(descartarAviso).execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    // --- datos ---

    private static AvisoItem unAviso(EstadoAviso estado) {
        return new AvisoItem(AVISO_ID, null, TipoAviso.STOCK, GrupoAviso.STOCK, (short) 3, estado, "registro:1",
                "Te quedaste sin yerba", "No te quedan unidades", List.of("motivo"),
                List.of(new AccionAbrirRegistro("Ver producto", null, null, null)), Map.of("cantidad", 0), AHORA, null);
    }
}
