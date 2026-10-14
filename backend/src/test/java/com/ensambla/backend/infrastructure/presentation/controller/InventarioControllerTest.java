package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.inventario.ConsultarStockRequest;
import com.ensambla.backend.domain.dto.inventario.ConsultarStockResponse;
import com.ensambla.backend.domain.dto.inventario.ListarMovimientosRequest;
import com.ensambla.backend.domain.dto.inventario.MovimientoItem;
import com.ensambla.backend.domain.dto.inventario.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.dto.inventario.RegistrarMovimientoResponse;
import com.ensambla.backend.domain.dto.inventario.StockItem;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.useCase.inventario.ConsultarStockUseCase;
import com.ensambla.backend.domain.useCase.inventario.ListarMovimientosUseCase;
import com.ensambla.backend.domain.useCase.inventario.RegistrarMovimientoUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.inventario.InventarioPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID MOVIMIENTO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");
    private static final MovimientoItem ITEM = new MovimientoItem(
            MOVIMIENTO_ID, PRODUCTO_ID, "Harina", "ENTRADA", new BigDecimal("2.5"),
            LocalDate.parse("2026-10-13"), "Compra", AHORA);

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private RegistrarMovimientoUseCase registrarMovimientoUseCase;
    private ListarMovimientosUseCase listarMovimientosUseCase;
    private ConsultarStockUseCase consultarStockUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registrarMovimientoUseCase = mock(RegistrarMovimientoUseCase.class);
        listarMovimientosUseCase = mock(ListarMovimientosUseCase.class);
        consultarStockUseCase = mock(ConsultarStockUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InventarioController(
                        registrarMovimientoUseCase,
                        listarMovimientosUseCase,
                        consultarStockUseCase,
                        new InventarioPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST registra el movimiento con el usuario de la sesión y responde 201 con el stock")
    void registrarShouldReturn201WithStockAndPassSessionUser() throws Exception {
        when(registrarMovimientoUseCase.execute(any(RegistrarMovimientoRequest.class)))
                .thenReturn(new RegistrarMovimientoResponse(ITEM, new BigDecimal("12")));
        String cuerpo = "{\"registroId\":\"" + PRODUCTO_ID + "\",\"tipo\":\"ENTRADA\",\"cantidad\":2.5,"
                + "\"fecha\":\"2026-10-13\",\"nota\":\"Compra\"}";

        mockMvc.perform(post("/api/bloques/{id}/movimientos", BLOQUE_ID)
                        .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimiento.id").value(MOVIMIENTO_ID.toString()))
                .andExpect(jsonPath("$.movimiento.producto").value("Harina"))
                .andExpect(jsonPath("$.movimiento.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.stockActual").value(12));
        ArgumentCaptor<RegistrarMovimientoRequest> request = ArgumentCaptor.forClass(RegistrarMovimientoRequest.class);
        verify(registrarMovimientoUseCase).execute(request.capture());
        assertThat(request.getValue()).isEqualTo(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", new BigDecimal("2.5"),
                LocalDate.parse("2026-10-13"), "Compra"));
    }

    @Test
    @DisplayName("POST con datos inválidos responde 400 con el mensaje de cada dato")
    void registrarShouldReturn400WithFieldErrors() throws Exception {
        when(registrarMovimientoUseCase.execute(any(RegistrarMovimientoRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("cantidad", "Tiene que ser mayor a cero")));

        mockMvc.perform(post("/api/bloques/{id}/movimientos", BLOQUE_ID)
                        .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cantidad").value("Tiene que ser mayor a cero"));
    }

    @Test
    @DisplayName("GET de movimientos devuelve la lista y pasa el producto elegido")
    void movimientosShouldReturnListAndPassProduct() throws Exception {
        when(listarMovimientosUseCase.execute(any(ListarMovimientosRequest.class))).thenReturn(List.of(ITEM));

        mockMvc.perform(get("/api/bloques/{id}/movimientos", BLOQUE_ID)
                        .param("registroId", PRODUCTO_ID.toString()).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].producto").value("Harina"))
                .andExpect(jsonPath("$[0].cantidad").value(2.5))
                .andExpect(jsonPath("$[0].fecha").value("2026-10-13"));
        verify(listarMovimientosUseCase).execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, PRODUCTO_ID));
    }

    @Test
    @DisplayName("GET de movimientos sin producto trae los de todo el bloque")
    void movimientosShouldNotFilterWithoutProduct() throws Exception {
        when(listarMovimientosUseCase.execute(any(ListarMovimientosRequest.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/bloques/{id}/movimientos", BLOQUE_ID).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verify(listarMovimientosUseCase).execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, null));
    }

    @Test
    @DisplayName("GET del stock devuelve la página con las claves que espera el frontend")
    void stockShouldReturnPageWithFrontendKeys() throws Exception {
        when(consultarStockUseCase.execute(any(ConsultarStockRequest.class))).thenReturn(new ConsultarStockResponse(
                List.of(new StockItem(PRODUCTO_ID, "Harina", new BigDecimal("8"))), 1, 20, 41, 3));

        mockMvc.perform(get("/api/bloques/{id}/stock", BLOQUE_ID)
                        .param("q", "har").param("pagina", "1").param("tamanio", "20").principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.content[0].producto").value("Harina"))
                .andExpect(jsonPath("$.content[0].stock").value(8))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
        verify(consultarStockUseCase).execute(new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, "har", 1, 20));
    }

    @Test
    @DisplayName("GET del stock de una sola página marca primera y última")
    void stockShouldFlagFirstAndLastPage() throws Exception {
        when(consultarStockUseCase.execute(any(ConsultarStockRequest.class)))
                .thenReturn(new ConsultarStockResponse(List.of(), 0, 20, 0, 1));

        mockMvc.perform(get("/api/bloques/{id}/stock", BLOQUE_ID).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}
