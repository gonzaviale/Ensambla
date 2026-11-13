package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.EstadoDelNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.NegocioResumido;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuenta;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaRequest;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse.Periodo;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse.Totales;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumenDeLaCuentaControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId KIOSCO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ObtenerResumenDeLaCuenta obtenerResumenDeLaCuenta;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        obtenerResumenDeLaCuenta = mock(ObtenerResumenDeLaCuenta.class);
        mockMvc = MockMvcDePrueba.standalone(new ResumenDeLaCuentaController(obtenerResumenDeLaCuenta))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve el resumen de la cuenta de la sesión con los nombres del contrato")
    void obtenerShouldReturnTheSummaryOfTheSessionAccount() throws Exception {
        givenResumen();

        ResultActions result = mockMvc.perform(get("/api/negocios/resumen").principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value("2026-10-14"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-11-12"))
                .andExpect(jsonPath("$.totales.ventas").value(280000))
                .andExpect(jsonPath("$.totales.margen").value(-72000))
                .andExpect(jsonPath("$.totales.avisosPendientes").value(2))
                .andExpect(jsonPath("$.negocios[0].id").value(KIOSCO_ID.toString()))
                .andExpect(jsonPath("$.negocios[0].nombre").value("Kiosco de la esquina"))
                .andExpect(jsonPath("$.negocios[0].rubro").value("KIOSCO"))
                .andExpect(jsonPath("$.negocios[0].ventas").value(280000))
                .andExpect(jsonPath("$.negocios[0].margen").value(-72000))
                .andExpect(jsonPath("$.negocios[0].avisosPendientes").value(2))
                .andExpect(jsonPath("$.negocios[0].estado").value("ATENCION"));
    }

    // --- arrange ---
    private void givenResumen() {
        when(obtenerResumenDeLaCuenta.execute(new ObtenerResumenDeLaCuentaRequest(USUARIO_ID)))
                .thenReturn(new ObtenerResumenDeLaCuentaResponse(
                        new Periodo(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 11, 12)),
                        new Totales(280000, -72000, 2),
                        List.of(new NegocioResumido(KIOSCO_ID, "Kiosco de la esquina", RubroNegocio.KIOSCO,
                                280000, -72000, 2, EstadoDelNegocio.ATENCION))));
    }
}
