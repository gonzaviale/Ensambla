package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.NegocioResumido;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuenta;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaRequest;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse.Total;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumenDeLaCuentaControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId KIOSCO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final NegocioId FERIA_ID = NegocioId.de("a0000000-0000-4000-8000-000000000003");

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
        givenResumen(null);

        ResultActions result = mockMvc.perform(get("/api/negocios/resumen").principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-14"))
                .andExpect(jsonPath("$.hasta").value("2026-11-12"))
                .andExpect(jsonPath("$.dias").value(30))
                .andExpect(jsonPath("$.total.negocios").value(2))
                .andExpect(jsonPath("$.total.vendiste").value(280000))
                .andExpect(jsonPath("$.total.gastos").value(52000))
                .andExpect(jsonPath("$.total.queda").value(-72000))
                .andExpect(jsonPath("$.total.teDeben").value(15000))
                .andExpect(jsonPath("$.total.avisosPendientes").value(3))
                .andExpect(jsonPath("$.negocios[0].id").value(KIOSCO_ID.toString()))
                .andExpect(jsonPath("$.negocios[0].nombre").value("Kiosco de la esquina"))
                .andExpect(jsonPath("$.negocios[0].rubro").value("KIOSCO"))
                .andExpect(jsonPath("$.negocios[0].activo").value(true))
                .andExpect(jsonPath("$.negocios[0].hayDatos").value(true))
                .andExpect(jsonPath("$.negocios[0].vendiste").value(280000))
                .andExpect(jsonPath("$.negocios[0].gastos").value(52000))
                .andExpect(jsonPath("$.negocios[0].queda").value(-72000))
                .andExpect(jsonPath("$.negocios[0].teDeben").value(15000))
                .andExpect(jsonPath("$.negocios[0].avisosPendientes").value(2));
    }

    @Test
    @DisplayName("GET manda en null las métricas de un negocio sin datos, en lugar de omitirlas o ponerlas en cero")
    void obtenerShouldSendNullMetricsForABusinessWithoutData() throws Exception {
        givenResumen(null);

        ResultActions result = mockMvc.perform(get("/api/negocios/resumen").principal(sesion));

        result.andExpect(jsonPath("$.negocios[1].hayDatos").value(false))
                .andExpect(jsonPath("$.negocios[1].vendiste").value(nullValue()))
                .andExpect(jsonPath("$.negocios[1].gastos").value(nullValue()))
                .andExpect(jsonPath("$.negocios[1].queda").value(nullValue()))
                .andExpect(jsonPath("$.negocios[1].teDeben").value(nullValue()))
                .andExpect(jsonPath("$.negocios[1].avisosPendientes").value(1));
    }

    @Test
    @DisplayName("GET pasa los días pedidos al caso de uso")
    void obtenerShouldPassTheRequestedDays() throws Exception {
        givenResumen(7);

        ResultActions result = mockMvc.perform(get("/api/negocios/resumen").param("dias", "7").principal(sesion));

        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET con días fuera de rango responde 400")
    void obtenerShouldReturnBadRequestWhenTheDaysAreOutOfRange() throws Exception {
        when(obtenerResumenDeLaCuenta.execute(new ObtenerResumenDeLaCuentaRequest(USUARIO_ID, 400)))
                .thenThrow(new BusinessRuleException("El resumen no puede cubrir más de 366 días"));

        ResultActions result = mockMvc.perform(get("/api/negocios/resumen").param("dias", "400").principal(sesion));

        result.andExpect(status().isBadRequest());
    }

    // --- arrange ---
    private void givenResumen(Integer dias) {
        when(obtenerResumenDeLaCuenta.execute(new ObtenerResumenDeLaCuentaRequest(USUARIO_ID, dias)))
                .thenReturn(new ObtenerResumenDeLaCuentaResponse(
                        LocalDate.of(2026, 10, 14), LocalDate.of(2026, 11, 12), 30,
                        new Total(2, pesos(280000), pesos(52000), pesos(-72000), pesos(15000), 3),
                        List.of(
                                new NegocioResumido(KIOSCO_ID, "Kiosco de la esquina", RubroNegocio.KIOSCO, true, true,
                                        pesos(280000), pesos(52000), pesos(-72000), pesos(15000), 2),
                                new NegocioResumido(FERIA_ID, "Puesto de la feria", RubroNegocio.OTRO, false, false,
                                        null, null, null, null, 1))));
    }

    private static BigDecimal pesos(long valor) {
        return BigDecimal.valueOf(valor);
    }
}
