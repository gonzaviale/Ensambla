package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto real con la cadena de filtros de Spring Security (PostgreSQL en Testcontainers). */
@AutoConfigureMockMvc
class SeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions respuesta;

    @Test
    @DisplayName("El catálogo de rubros exige sesión")
    void elCatalogoDeRubrosExigeSesion() throws Exception {
        cuandoSePideSinSesion(get("/api/ubicacion/rubros"));

        entoncesFaltaLaSesion();
    }

    @Test
    @DisplayName("La búsqueda geográfica exige sesión incluso antes de validar CSRF")
    void laBusquedaGeograficaExigeSesionAntesDelCsrf() throws Exception {
        cuandoSePideSinSesion(post("/api/ubicacion/direcciones"));

        entoncesFaltaLaSesion();
    }

    @Test
    @DisplayName("Un endpoint privado sin sesión responde 401 con el cuerpo de error estándar")
    void unEndpointPrivadoSinSesionRespondeElErrorEstandar() throws Exception {
        cuandoSePideSinSesion(get("/api/proyectos"));

        entoncesFaltaLaSesionConElMensaje("Se requiere una sesión válida");
    }

    @Test
    @DisplayName("Los registros de un bloque exigen sesión")
    void losRegistrosDeUnBloqueExigenSesion() throws Exception {
        cuandoSePideSinSesion(get("/api/bloques/b0000000-0000-4000-8000-000000000001/registros"));

        entoncesFaltaLaSesion();
    }

    @Test
    @DisplayName("Las plantillas son públicas")
    void lasPlantillasSonPublicas() throws Exception {
        cuandoSePideSinSesion(get("/api/plantillas"));

        entoncesRespondeUnaLista();
    }

    @Test
    @DisplayName("El token CSRF es público")
    void elTokenCsrfEsPublico() throws Exception {
        cuandoSePideSinSesion(get("/api/auth/csrf"));

        entoncesRespondeElHeaderCsrf("X-CSRF-TOKEN");
    }

    @Test
    @DisplayName("Una escritura sin token CSRF se rechaza con 403")
    void unaEscrituraSinTokenCsrfSeRechaza() throws Exception {
        cuandoSePideSinSesion(post("/api/auth/login"));

        entoncesSeRechazaPorCsrf();
    }

    private void cuandoSePideSinSesion(RequestBuilder pedido) throws Exception {
        respuesta = mockMvc.perform(pedido);
    }

    private void entoncesFaltaLaSesion() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesFaltaLaSesionConElMensaje(String mensaje) throws Exception {
        respuesta.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    private void entoncesRespondeUnaLista() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    private void entoncesRespondeElHeaderCsrf(String header) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value(header));
    }

    private void entoncesSeRechazaPorCsrf() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }
}