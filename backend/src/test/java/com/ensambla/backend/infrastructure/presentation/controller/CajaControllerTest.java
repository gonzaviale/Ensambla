package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CajaPresentationMapper;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CajaControllerTest {

    private final CobrarVenta cobrarVenta = mock(CobrarVenta.class);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(new CajaController(cobrarVenta,
            new CajaPresentationMapper(), mock(ConsultarCaja.class), mock(CerrarCaja.class), mock(ObtenerCierre.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("El cobro usa al usuario autenticado y devuelve el comprobante con todas sus líneas")
    void cobrarShouldReturnReceipt() throws Exception {
        givenComprobante();

        mockMvc.perform(post("/api/caja/ventas").principal(UsernamePasswordAuthenticationToken.authenticated(
                        CajaTestData.USUARIO.toString(), null, List.of()))
                        .contentType("application/json").content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2000))
                .andExpect(jsonPath("$.medio").value("EFECTIVO"))
                .andExpect(jsonPath("$.recibido").value(5000)).andExpect(jsonPath("$.vuelto").value(3000))
                .andExpect(jsonPath("$.lineas[0].nombre").value("Pan"))
                .andExpect(jsonPath("$.lineas[0].modo").value("PESO"))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(0.5))
                .andExpect(jsonPath("$.lineas[0].precio").value(4000))
                .andExpect(jsonPath("$.lineas[0].importe").value(2000));

        ArgumentCaptor<CobrarVentaRequest> captor = ArgumentCaptor.forClass(CobrarVentaRequest.class);
        verify(cobrarVenta).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(CajaTestData.request("0.5"));
    }

    @Test
    @DisplayName("Una cantidad nula no llega al caso de uso")
    void cobrarShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/caja/ventas").contentType("application/json")
                        .content(body().replace("0.5", "0")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cobrarVenta);
    }

    @Test
    @DisplayName("Los pagos mixtos llegan al dominio con cada importe separado")
    void cobrarShouldMapSplitPayments() throws Exception {
        givenComprobante();

        mockMvc.perform(post("/api/caja/ventas").principal(UsernamePasswordAuthenticationToken.authenticated(
                        CajaTestData.USUARIO.toString(), null, List.of())).contentType("application/json")
                        .content(body().replace("\"recibido\":5000", "\"recibido\":5000,\"pagos\":[{\"medio\":\"EFECTIVO\",\"importe\":1000},{\"medio\":\"TRANSFERENCIA\",\"importe\":1000}]")))
                .andExpect(status().isOk());

        ArgumentCaptor<CobrarVentaRequest> captor = ArgumentCaptor.forClass(CobrarVentaRequest.class);
        verify(cobrarVenta).execute(captor.capture());
        assertThat(captor.getValue().pagos()).containsExactly(
                new CobrarVentaRequest.PagoRequest("EFECTIVO", new BigDecimal("1000")),
                new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("1000")));
    }

    // --- arrange ---
    private void givenComprobante() {
        when(cobrarVenta.execute(any())).thenReturn(new CobrarVentaResponse(CajaTestData.OPERACION,
                List.of(new CobrarVentaResponse.LineaItem(CajaTestData.PRODUCTO, "Pan", "PESO",
                        new BigDecimal("0.5"), new BigDecimal("4000"), new BigDecimal("2000"))),
                "EFECTIVO", new BigDecimal("2000"), new BigDecimal("5000"), new BigDecimal("3000"),
                CajaTestData.AHORA, List.of(new CobrarVentaResponse.PagoItem("EFECTIVO",new BigDecimal("2000")))));
    }

    private String body() {
        return """
                {"id":"%s","proyectoId":"%s","inventarioId":"%s","ventasId":"%s",
                "medio":"EFECTIVO","recibido":5000,"lineas":[{"productoId":"%s",
                "cantidad":0.5,"precio":4000,"modo":"PESO"}]}
                """.formatted(CajaTestData.OPERACION, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                CajaTestData.VENTAS, CajaTestData.PRODUCTO);
    }
}
