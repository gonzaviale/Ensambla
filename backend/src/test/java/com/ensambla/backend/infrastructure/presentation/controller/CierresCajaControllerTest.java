package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaResponse;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierreRequest;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CajaPresentationMapper;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CierresCajaControllerTest {

    private final CobrarVenta cobrar = mock(CobrarVenta.class);
    private final ConsultarCaja consultar = mock(ConsultarCaja.class);
    private final CerrarCaja cerrar = mock(CerrarCaja.class);
    private final ObtenerCierre obtener = mock(ObtenerCierre.class);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(new CajaController(cobrar,
            new CajaPresentationMapper(), consultar, cerrar, obtener))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("El resumen devuelve el día, el período y el historial con sus pagos")
    void resumenShouldMapEntireResponse() throws Exception {
        var resumen = ResumenCaja.de(List.of(CierreCajaTestData.venta()));
        LocalDate fecha = LocalDate.of(2026, 11, 12);
        when(consultar.execute(any())).thenReturn(ConsultarCajaResponse.de(fecha, resumen, resumen,
                List.of(CierreCajaTestData.cierre()), 0));

        mockMvc.perform(get("/api/caja/resumen").principal(sesion())
                        .param("proyectoId", CajaTestData.PROYECTO.toString())
                        .param("inventarioId", CajaTestData.INVENTARIO.toString()).param("fecha", fecha.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fecha").value("2026-11-12"))
                .andExpect(jsonPath("$.abierta.total").value(2000)).andExpect(jsonPath("$.dia.efectivo").value(1000))
                .andExpect(jsonPath("$.abierta.transferencia").value(700)).andExpect(jsonPath("$.abierta.tarjeta").value(300))
                .andExpect(jsonPath("$.abierta.ventas[0].pagos[0].medio").value("EFECTIVO"))
                .andExpect(jsonPath("$.abierta.ventas[0].pagos[0].importe").value(1000))
                .andExpect(jsonPath("$.cierres[0].id").value(CierreCajaTestData.CIERRE.toString()))
                .andExpect(jsonPath("$.cierres[0].cantidadVentas").value(1))
                .andExpect(jsonPath("$.cierres[0].diferencia").value(0))
                .andExpect(jsonPath("$.hayMas").value(false));

        ArgumentCaptor<ConsultarCajaRequest> captor = ArgumentCaptor.forClass(ConsultarCajaRequest.class);
        verify(consultar).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ConsultarCajaRequest(CajaTestData.USUARIO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, fecha, 0));
    }

    @Test
    @DisplayName("El cierre toma el usuario de la sesión y devuelve la copia histórica")
    void cerrarShouldMapReceiptAndRequest() throws Exception {
        when(cerrar.execute(any())).thenReturn(CierreCajaItem.de(CierreCajaTestData.cierre()));

        mockMvc.perform(post("/api/caja/cierres").principal(sesion()).contentType("application/json").content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resumen.total").value(2000))
                .andExpect(jsonPath("$.fondoInicial").value(500)).andExpect(jsonPath("$.entradas").value(100))
                .andExpect(jsonPath("$.retiros").value(200)).andExpect(jsonPath("$.esperado").value(1400))
                .andExpect(jsonPath("$.contado").value(1400)).andExpect(jsonPath("$.diferencia").value(0))
                .andExpect(jsonPath("$.nota").value("Revisado"));

        ArgumentCaptor<CerrarCajaRequest> captor = ArgumentCaptor.forClass(CerrarCajaRequest.class);
        verify(cerrar).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(CierreCajaTestData.request());
    }

    @Test
    @DisplayName("El detalle usa el cierre tipado y el negocio de la sesión")
    void obtenerShouldMapSnapshot() throws Exception {
        when(obtener.execute(any())).thenReturn(CierreCajaItem.de(CierreCajaTestData.cierre()));

        mockMvc.perform(get("/api/caja/cierres/" + CierreCajaTestData.CIERRE).principal(sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(CierreCajaTestData.CIERRE.toString()));

        verify(obtener).execute(new ObtenerCierreRequest(CajaTestData.USUARIO, CierreCajaTestData.CIERRE));
    }

    @Test
    @DisplayName("Un cierre con retiros negativos no llega al dominio")
    void cerrarShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/caja/cierres").principal(sesion()).contentType("application/json")
                        .content(body().replace("\"retiros\":200", "\"retiros\":-1")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cerrar);
    }

    // --- helpers ---
    private UsernamePasswordAuthenticationToken sesion() {
        return UsernamePasswordAuthenticationToken.authenticated(CajaTestData.USUARIO.toString(), null, List.of());
    }

    private String body() {
        return """
                {"id":"%s","proyectoId":"%s","inventarioId":"%s","revision":"%s",
                "fondoInicial":500,"entradas":100,"retiros":200,"contado":1400,"nota":"Revisado"}
                """.formatted(CierreCajaTestData.CIERRE, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                CierreCajaTestData.request().revision());
    }
}
