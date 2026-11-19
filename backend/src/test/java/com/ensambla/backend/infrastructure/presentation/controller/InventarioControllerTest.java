package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStock;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.StockItem;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientos;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimiento;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.inventario.InventarioPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventarioControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final MovimientoStockId MOVIMIENTO_ID = MovimientoStockId.de("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");
    private static final MovimientoItem ITEM = new MovimientoItem(
            MOVIMIENTO_ID, PRODUCTO_ID, "Harina", "ENTRADA", new BigDecimal("2.5"),
            LocalDate.parse("2026-10-13"), "Compra", AHORA);

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private RegistrarMovimiento registrarMovimiento;
    private ListarMovimientos listarMovimientos;
    private ConsultarStock consultarStock;
    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        registrarMovimiento = mock(RegistrarMovimiento.class);
        listarMovimientos = mock(ListarMovimientos.class);
        consultarStock = mock(ConsultarStock.class);
        mockMvc = MockMvcDePrueba.standalone(new InventarioController(
                        registrarMovimiento,
                        listarMovimientos,
                        consultarStock,
                        new InventarioPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST registra el movimiento con el usuario de la sesión y responde 201 con el stock")
    void postRegistraElMovimientoConElUsuarioDeLaSesionYResponde201ConElStock() throws Exception {
        dadoUnMovimientoRegistrable();

        cuandoSeRegistraUnaEntradaDeHarina();

        entoncesSeResponde201ConElMovimientoYElStock();
        entoncesElCasoDeUsoRecibeLaEntradaDelUsuario();
    }

    @Test
    @DisplayName("POST con datos inválidos responde 400 con el mensaje de cada dato")
    void postConDatosInvalidosResponde400ConElMensajeDeCadaDato() throws Exception {
        dadoUnMovimientoInvalido();

        cuandoSeRegistraUnMovimientoSinDatos();

        entoncesSeResponde400ConElMensajeDeLaCantidad();
    }

    @Test
    @DisplayName("GET de movimientos devuelve la lista y pasa el producto elegido")
    void getDeMovimientosDevuelveLaListaYPasaElProductoElegido() throws Exception {
        dadoUnBloqueConUnMovimiento();

        cuandoSeListanLosMovimientosDelProducto();

        entoncesSeDevuelveElMovimientoDeHarina();
        entoncesSeConsultanLosMovimientosDelProducto();
    }

    @Test
    @DisplayName("GET de movimientos sin producto trae los de todo el bloque")
    void getDeMovimientosSinProductoTraeLosDeTodoElBloque() throws Exception {
        dadoUnBloqueSinMovimientos();

        cuandoSeListanLosMovimientosDelBloque();

        entoncesSeDevuelveUnaListaVacia();
        entoncesSeConsultanLosMovimientosSinFiltrarPorProducto();
    }

    @Test
    @DisplayName("GET del stock devuelve la página con las claves que espera el frontend")
    void getDelStockDevuelveLaPaginaConLasClavesQueEsperaElFrontend() throws Exception {
        dadoUnaPaginaDeStockDeHarina();

        cuandoSeConsultaElStockBuscandoHarina();

        entoncesSeDevuelveLaPaginaConLasClavesDelFrontend();
        entoncesSeConsultaElStockBuscandoHarina();
    }

    @Test
    @DisplayName("GET del stock de una sola página marca primera y última")
    void getDelStockDeUnaSolaPaginaMarcaPrimeraYUltima() throws Exception {
        dadoUnStockDeUnaSolaPagina();

        cuandoSeConsultaElStockDelBloque();

        entoncesLaPaginaEsPrimeraYUltima();
    }

    // --- dado ---

    private void dadoUnMovimientoRegistrable() {
        when(registrarMovimiento.execute(any(RegistrarMovimientoRequest.class)))
                .thenReturn(new RegistrarMovimientoResponse(ITEM, new BigDecimal("12")));
    }

    private void dadoUnMovimientoInvalido() {
        when(registrarMovimiento.execute(any(RegistrarMovimientoRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("cantidad", "Tiene que ser mayor a cero")));
    }

    private void dadoUnBloqueConUnMovimiento() {
        when(listarMovimientos.execute(any(ListarMovimientosRequest.class))).thenReturn(List.of(ITEM));
    }

    private void dadoUnBloqueSinMovimientos() {
        when(listarMovimientos.execute(any(ListarMovimientosRequest.class))).thenReturn(List.of());
    }

    private void dadoUnaPaginaDeStockDeHarina() {
        when(consultarStock.execute(any(ConsultarStockRequest.class))).thenReturn(new ConsultarStockResponse(
                List.of(new StockItem(PRODUCTO_ID, "Harina", new BigDecimal("8"))), 1, 20, 41, 3));
    }

    private void dadoUnStockDeUnaSolaPagina() {
        when(consultarStock.execute(any(ConsultarStockRequest.class)))
                .thenReturn(new ConsultarStockResponse(List.of(), 0, 20, 0, 1));
    }

    // --- cuando ---

    private void cuandoSeRegistraUnaEntradaDeHarina() throws Exception {
        String cuerpo = "{\"registroId\":\"" + PRODUCTO_ID + "\",\"tipo\":\"ENTRADA\",\"cantidad\":2.5,"
                + "\"fecha\":\"2026-10-13\",\"nota\":\"Compra\"}";
        respuesta = mockMvc.perform(post("/api/bloques/{id}/movimientos", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void cuandoSeRegistraUnMovimientoSinDatos() throws Exception {
        respuesta = mockMvc.perform(post("/api/bloques/{id}/movimientos", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void cuandoSeListanLosMovimientosDelProducto() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/movimientos", BLOQUE_ID)
                .param("registroId", PRODUCTO_ID.toString()).principal(sesion));
    }

    private void cuandoSeListanLosMovimientosDelBloque() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/movimientos", BLOQUE_ID).principal(sesion));
    }

    private void cuandoSeConsultaElStockBuscandoHarina() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/stock", BLOQUE_ID)
                .param("q", "har").param("pagina", "1").param("tamanio", "20").principal(sesion));
    }

    private void cuandoSeConsultaElStockDelBloque() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/stock", BLOQUE_ID).principal(sesion));
    }

    // --- entonces ---

    private void entoncesSeResponde201ConElMovimientoYElStock() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimiento.id").value(MOVIMIENTO_ID.toString()))
                .andExpect(jsonPath("$.movimiento.producto").value("Harina"))
                .andExpect(jsonPath("$.movimiento.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.stockActual").value(12));
    }

    private void entoncesElCasoDeUsoRecibeLaEntradaDelUsuario() {
        ArgumentCaptor<RegistrarMovimientoRequest> pedido = ArgumentCaptor.forClass(RegistrarMovimientoRequest.class);
        verify(registrarMovimiento).execute(pedido.capture());
        assertThat(pedido.getValue()).isEqualTo(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", new BigDecimal("2.5"),
                LocalDate.parse("2026-10-13"), "Compra"));
    }

    private void entoncesSeResponde400ConElMensajeDeLaCantidad() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cantidad").value("Tiene que ser mayor a cero"));
    }

    private void entoncesSeDevuelveElMovimientoDeHarina() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].producto").value("Harina"))
                .andExpect(jsonPath("$[0].cantidad").value(2.5))
                .andExpect(jsonPath("$[0].fecha").value("2026-10-13"));
    }

    private void entoncesSeConsultanLosMovimientosDelProducto() {
        verify(listarMovimientos).execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, PRODUCTO_ID));
    }

    private void entoncesSeDevuelveUnaListaVacia() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private void entoncesSeConsultanLosMovimientosSinFiltrarPorProducto() {
        verify(listarMovimientos).execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, null));
    }

    private void entoncesSeDevuelveLaPaginaConLasClavesDelFrontend() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.content[0].producto").value("Harina"))
                .andExpect(jsonPath("$.content[0].stock").value(8))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    private void entoncesSeConsultaElStockBuscandoHarina() {
        verify(consultarStock).execute(new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, "har", 1, 20));
    }

    private void entoncesLaPaginaEsPrimeraYUltima() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}
