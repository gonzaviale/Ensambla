package com.ensambla.backend.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto real con la cadena de filtros de Spring Security (H2 en memoria). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2-test")
class SeguridadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un endpoint privado sin sesión responde 401 con el cuerpo de error estándar")
    void privateEndpointShouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/proyectos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Se requiere una sesión válida"));
    }

    @Test
    @DisplayName("Las plantillas son públicas")
    void plantillasShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/plantillas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("El token CSRF es público")
    void csrfShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    @DisplayName("Una escritura sin token CSRF se rechaza con 403")
    void writeWithoutCsrfShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isForbidden());
    }
}
