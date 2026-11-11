package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.orden.LineaOrdenItem;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrden;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrdenRequest;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenes;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenesRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.orden.OrdenPresentationMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdenControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final OrdenCompraId ORDEN_ID = OrdenCompraId.de("f0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");

    private final ListarOrdenes listarOrdenes = mock(ListarOrdenes.class);
    private final CambiarEstadoOrden cambiarEstadoOrden = mock(CambiarEstadoOrden.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrdenController controller = new OrdenController(listarOrdenes, cambiarEstadoOrden,
                new OrdenPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("GET lista las órdenes y mapea todos sus datos, con los ítems, a JSON")
    void listarShouldReturnMappedOrders() throws Exception {
        when(listarOrdenes.execute(any())).thenReturn(List.of(givenOrden(EstadoOrdenCompra.PENDIENTE, null)));

        ResultActions result = mockMvc.perform(get("/api/ordenes").principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDEN_ID.toString()))
                .andExpect(jsonPath("$[0].clienteTelefono").value("5491100000001"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].total").value(7000.0))
                .andExpect(jsonPath("$[0].motivoRechazo").doesNotExist())
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists())
                .andExpect(jsonPath("$[0].items[0].nombre").value("Yerba Playadito 1 kg"))
                .andExpect(jsonPath("$[0].items[0].cantidad").value(2))
                .andExpect(jsonPath("$[0].items[0].precioUnitario").value(3500.0))
                .andExpect(jsonPath("$[0].items[0].subtotal").value(7000.0));
        verify(listarOrdenes).execute(new ListarOrdenesRequest(USUARIO_ID, null));
    }

    @Test
    @DisplayName("GET con un estado lo pasa al caso de uso")
    void listarShouldPassTheStateFilter() throws Exception {
        when(listarOrdenes.execute(any())).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get("/api/ordenes?estado=LISTA").principal(principal));

        result.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        verify(listarOrdenes).execute(new ListarOrdenesRequest(USUARIO_ID, EstadoOrdenCompra.LISTA));
    }

    @Test
    @DisplayName("PATCH cambia el estado con el usuario de la sesión y devuelve la orden")
    void cambiarEstadoShouldReturnTheUpdatedOrder() throws Exception {
        when(cambiarEstadoOrden.execute(any())).thenReturn(givenOrden(EstadoOrdenCompra.RECHAZADA, "No hay stock"));

        ResultActions result = patchEstado("{\"estado\":\"RECHAZADA\",\"motivo\":\"No hay stock\"}");

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("No hay stock"));
        verify(cambiarEstadoOrden).execute(new CambiarEstadoOrdenRequest(USUARIO_ID, ORDEN_ID,
                EstadoOrdenCompra.RECHAZADA, "No hay stock"));
    }

    @Test
    @DisplayName("PATCH sin motivo lo pasa vacío")
    void cambiarEstadoShouldAllowNoReason() throws Exception {
        when(cambiarEstadoOrden.execute(any())).thenReturn(givenOrden(EstadoOrdenCompra.ACEPTADA, null));

        patchEstado("{\"estado\":\"ACEPTADA\"}").andExpect(status().isOk());

        verify(cambiarEstadoOrden).execute(
                new CambiarEstadoOrdenRequest(USUARIO_ID, ORDEN_ID, EstadoOrdenCompra.ACEPTADA, null));
    }

    @Test
    @DisplayName("PATCH sin estado, con un estado inexistente o con un motivo larguísimo devuelve 400 sin llegar al caso de uso")
    void cambiarEstadoShouldValidateTheBody() throws Exception {
        patchEstado("{}").andExpect(status().isBadRequest());
        patchEstado("{\"estado\":\"VOLANDO\"}").andExpect(status().isBadRequest());
        patchEstado("{\"estado\":\"RECHAZADA\",\"motivo\":\"" + "x".repeat(301) + "\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cambiarEstadoOrden);
    }

    @Test
    @DisplayName("PATCH con un paso que no está permitido devuelve 400 con el motivo")
    void cambiarEstadoShouldReturn400ForAnInvalidStep() throws Exception {
        when(cambiarEstadoOrden.execute(any())).thenThrow(
                new BusinessRuleException("No se puede pasar una orden pendiente a lista para retirar"));

        patchEstado("{\"estado\":\"LISTA\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede pasar una orden pendiente a lista para retirar"));
    }

    @Test
    @DisplayName("PATCH sobre una orden ajena o inexistente devuelve 404")
    void cambiarEstadoShouldReturn404ForForeignOrders() throws Exception {
        when(cambiarEstadoOrden.execute(any())).thenThrow(new ResourceNotFoundException("La orden no existe"));

        patchEstado("{\"estado\":\"ACEPTADA\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH con un id que no es un UUID devuelve 400")
    void cambiarEstadoShouldRejectAMalformedId() throws Exception {
        mockMvc.perform(patch("/api/ordenes/{id}/estado", "no-es-un-uuid").principal(principal)
                .contentType("application/json").content("{\"estado\":\"ACEPTADA\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cambiarEstadoOrden);
    }

    private ResultActions patchEstado(String body) throws Exception {
        return mockMvc.perform(patch("/api/ordenes/{id}/estado", ORDEN_ID).principal(principal)
                .contentType("application/json").content(body));
    }

    private static OrdenItem givenOrden(EstadoOrdenCompra estado, String motivo) {
        return new OrdenItem(ORDEN_ID, "5491100000001", estado, List.of(new LineaOrdenItem("Yerba Playadito 1 kg",
                BigDecimal.valueOf(2), new BigDecimal("3500"), new BigDecimal("7000"))), new BigDecimal("7000"),
                motivo, AHORA, AHORA);
    }
}
