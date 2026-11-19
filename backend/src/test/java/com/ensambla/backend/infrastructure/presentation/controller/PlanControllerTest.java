package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanes;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPrueba;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.PlanPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.ResultActions;
import java.util.List;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.USUARIO_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanControllerTest {

    private ResultActions resultado;

    private final IniciarPrueba iniciarPrueba = mock(IniciarPrueba.class);
    private final MockMvc mockMvc = MockMvcDePrueba.standalone(
            new PlanController(iniciarPrueba, new PlanPresentationMapper(), mock(ConsultarPlanes.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("Devuelve el período premium y envía el usuario autenticado al caso de uso")
    void devuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso() throws Exception {
        dadoElEscenarioDeDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso();

        cuandoSeEjecutaDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso();

        entoncesSeVerificaDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso();
    }

    @Test
    @DisplayName("Una prueba ya utilizada devuelve 403 con un código estable")
    void unaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable() throws Exception {
        dadoElEscenarioDeUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable();

        cuandoSeEjecutaUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable();

        entoncesSeVerificaUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable();
    }

    // --- dado ---
    private UsernamePasswordAuthenticationToken dadoAuthentication() {
        return UsernamePasswordAuthenticationToken.authenticated(NEGOCIO_ID.toString(), null, List.of());
    }

    // --- dado ---

    private void dadoElEscenarioDeDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso() throws Exception {
        when(iniciarPrueba.execute(new IniciarPruebaRequest(USUARIO_ID)))
                        .thenReturn(new PruebaPremiumResponse(INSTANTE, INSTANTE.plusDays(7), 0, 10));
    }

    private void dadoElEscenarioDeUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable() throws Exception {
        when(iniciarPrueba.execute(new IniciarPruebaRequest(USUARIO_ID)))
                        .thenThrow(new RestriccionPlanException("PRUEBA_UTILIZADA", "Ya utilizada"));
    }

    // --- cuando ---

    private void cuandoSeEjecutaDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso() throws Exception {
        resultado = mockMvc.perform(post("/api/planes/prueba").principal(dadoAuthentication()));
    }

    private void cuandoSeEjecutaUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable() throws Exception {
        resultado = mockMvc.perform(post("/api/planes/prueba").principal(dadoAuthentication()));
    }

    // --- entonces ---

    private void entoncesSeVerificaDevuelveElPeriodoPremiumYEnviaElUsuarioAutenticadoAlCasoDeUso() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.limitePaginasIa").value(10))
                        .andExpect(jsonPath("$.paginasIaUtilizadas").value(0))
                        .andExpect(header().string("Cache-Control", "no-store"));
        verify(iniciarPrueba).execute(new IniciarPruebaRequest(USUARIO_ID));
    }

    private void entoncesSeVerificaUnaPruebaYaUtilizadaDevuelve403ConUnCodigoEstable() throws Exception {
        resultado.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.codigo").value("PRUEBA_UTILIZADA"));
    }
}
