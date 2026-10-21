package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.preferencia.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.dto.preferencia.ObtenerPreferenciasRequest;
import com.ensambla.backend.domain.dto.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.useCase.preferencia.GuardarPreferenciasUseCase;
import com.ensambla.backend.domain.useCase.preferencia.ObtenerPreferenciasUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.preferencia.PreferenciaPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreferenciaControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");

    private final ObtenerPreferenciasUseCase obtenerPreferenciasUseCase = mock(ObtenerPreferenciasUseCase.class);
    private final GuardarPreferenciasUseCase guardarPreferenciasUseCase = mock(GuardarPreferenciasUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new PreferenciaController(
                obtenerPreferenciasUseCase, guardarPreferenciasUseCase, new PreferenciaPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve las preferencias del usuario de sesión")
    void obtenerShouldReturnPreferencias() throws Exception {
        when(obtenerPreferenciasUseCase.execute(any())).thenReturn(
                new PreferenciasItem(List.of("a1"), Map.of("STOCK", false), true, "Gonza"));

        mockMvc.perform(get("/api/preferencias").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descartadas[0]").value("a1"))
                .andExpect(jsonPath("$.grupos.STOCK").value(false))
                .andExpect(jsonPath("$.bienvenidaVista").value(true))
                .andExpect(jsonPath("$.nombre").value("Gonza"));
        verify(obtenerPreferenciasUseCase).execute(new ObtenerPreferenciasRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET omite el nombre cuando no hay uno elegido")
    void obtenerShouldOmitMissingNombre() throws Exception {
        when(obtenerPreferenciasUseCase.execute(any())).thenReturn(
                new PreferenciasItem(List.of(), Map.of(), false, null));

        mockMvc.perform(get("/api/preferencias").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    @DisplayName("PUT guarda las preferencias con el usuario de sesión")
    void guardarShouldSaveForSessionUsuario() throws Exception {
        when(guardarPreferenciasUseCase.execute(any())).thenReturn(
                new PreferenciasItem(List.of("a1"), Map.of(), true, "Gonza"));

        mockMvc.perform(put("/api/preferencias").principal(principal).contentType("application/json")
                        .content("{\"descartadas\":[\"a1\"],\"grupos\":{},\"bienvenidaVista\":true,\"nombre\":\"Gonza\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Gonza"));
        verify(guardarPreferenciasUseCase)
                .execute(new GuardarPreferenciasRequest(USUARIO_ID, List.of("a1"), Map.of(), true, "Gonza"));
    }

    @Test
    @DisplayName("PUT rechaza un cuerpo inválido sin tocar el caso de uso")
    void guardarShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(put("/api/preferencias").principal(principal).contentType("application/json")
                        .content("{\"descartadas\":[],\"grupos\":{},\"bienvenidaVista\":true,"
                                + "\"nombre\":\"un nombre que supera por mucho los cuarenta caracteres\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guardarPreferenciasUseCase);
    }
}
