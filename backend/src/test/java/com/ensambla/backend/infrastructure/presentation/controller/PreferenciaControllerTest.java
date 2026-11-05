package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp.ConfigurarAvisosPorWhatsapp;
import com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp.ConfigurarAvisosPorWhatsappRequest;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferenciasRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.preferencia.PreferenciaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ObtenerPreferencias obtenerPreferencias = mock(ObtenerPreferencias.class);
    private final GuardarPreferencias guardarPreferencias = mock(GuardarPreferencias.class);
    private final ConfigurarAvisosPorWhatsapp configurarAvisosPorWhatsapp = mock(ConfigurarAvisosPorWhatsapp.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new PreferenciaController(
                obtenerPreferencias, guardarPreferencias, configurarAvisosPorWhatsapp,
                new PreferenciaPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve las preferencias del usuario de sesión")
    void obtenerShouldReturnPreferencias() throws Exception {
        when(obtenerPreferencias.execute(any())).thenReturn(
                new PreferenciasItem(List.of("a1"), Map.of("STOCK", false), true, "Gonza", false));

        mockMvc.perform(get("/api/preferencias").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descartadas[0]").value("a1"))
                .andExpect(jsonPath("$.grupos.STOCK").value(false))
                .andExpect(jsonPath("$.bienvenidaVista").value(true))
                .andExpect(jsonPath("$.nombre").value("Gonza"))
                .andExpect(jsonPath("$.avisosPorWhatsapp").value(false));
        verify(obtenerPreferencias).execute(new ObtenerPreferenciasRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET omite el nombre cuando no hay uno elegido")
    void obtenerShouldOmitMissingNombre() throws Exception {
        when(obtenerPreferencias.execute(any())).thenReturn(
                new PreferenciasItem(List.of(), Map.of(), false, null, false));

        mockMvc.perform(get("/api/preferencias").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    @DisplayName("PUT guarda las preferencias con el usuario de sesión")
    void guardarShouldSaveForSessionUsuario() throws Exception {
        when(guardarPreferencias.execute(any())).thenReturn(
                new PreferenciasItem(List.of("a1"), Map.of(), true, "Gonza", false));

        mockMvc.perform(put("/api/preferencias").principal(principal).contentType("application/json")
                        .content("{\"descartadas\":[\"a1\"],\"grupos\":{},\"bienvenidaVista\":true,\"nombre\":\"Gonza\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Gonza"));
        verify(guardarPreferencias)
                .execute(new GuardarPreferenciasRequest(USUARIO_ID, List.of("a1"), Map.of(), true, "Gonza"));
    }

    @Test
    @DisplayName("PUT rechaza un cuerpo inválido sin tocar el caso de uso")
    void guardarShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(put("/api/preferencias").principal(principal).contentType("application/json")
                        .content("{\"descartadas\":[],\"grupos\":{},\"bienvenidaVista\":true,"
                                + "\"nombre\":\"un nombre que supera por mucho los cuarenta caracteres\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guardarPreferencias);
    }

    @Test
    @DisplayName("PUT de avisos por WhatsApp los prende para el usuario de sesión")
    void configurarAvisosShouldTurnThemOnForSessionUsuario() throws Exception {
        when(configurarAvisosPorWhatsapp.execute(any())).thenReturn(
                new PreferenciasItem(List.of(), Map.of(), true, null, true));

        mockMvc.perform(put("/api/preferencias/avisos-por-whatsapp").principal(principal)
                        .contentType("application/json").content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisosPorWhatsapp").value(true));
        verify(configurarAvisosPorWhatsapp).execute(new ConfigurarAvisosPorWhatsappRequest(USUARIO_ID, true));
    }

    @Test
    @DisplayName("PUT de avisos por WhatsApp responde 400 cuando falta el valor")
    void configurarAvisosShouldRejectAMissingValue() throws Exception {
        mockMvc.perform(put("/api/preferencias/avisos-por-whatsapp").principal(principal)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(configurarAvisosPorWhatsapp);
    }

    @Test
    @DisplayName("PUT de avisos por WhatsApp responde 400 cuando no hay número vinculado")
    void configurarAvisosShouldReturn400WhenThereIsNoLinkedNumber() throws Exception {
        when(configurarAvisosPorWhatsapp.execute(any()))
                .thenThrow(new BusinessRuleException("Primero vinculá tu número de WhatsApp para recibir los avisos"));

        mockMvc.perform(put("/api/preferencias/avisos-por-whatsapp").principal(principal)
                        .contentType("application/json").content("{\"activo\":true}"))
                .andExpect(status().isBadRequest());
    }
}
