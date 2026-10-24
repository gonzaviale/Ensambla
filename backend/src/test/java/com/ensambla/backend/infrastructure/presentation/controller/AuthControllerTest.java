package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.auth.iniciarSesion.IniciarSesion;
import com.ensambla.backend.domain.useCase.auth.obtenerSesion.ObtenerSesion;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import com.ensambla.backend.infrastructure.presentation.mapper.auth.AuthPresentationMapper;
import com.ensambla.backend.infrastructure.security.SesionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                mock(RegistrarUsuario.class),
                mock(IniciarSesion.class),
                mock(ObtenerSesion.class),
                new AuthPresentationMapper(),
                new SesionAdapter())).build();
    }

    @Test
    @DisplayName("CSRF entrega el token y el nombre del header sin caché")
    void csrfShouldReturnTokenAndHeaderName() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/auth/csrf")
                .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
