package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;
import com.ensambla.backend.domain.useCase.registro.TotalesItem;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistro;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistro;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.eliminarRegistro.EliminarRegistro;
import com.ensambla.backend.domain.useCase.registro.eliminarRegistro.EliminarRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferencias;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferenciasRequest;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ReferenciaItem;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistros;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosRequest;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosResponse;
import com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen.ObtenerFotoOrigen;
import com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistro;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.registro.RegistroPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistroControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final RegistroItem ITEM = new RegistroItem(
            REGISTRO_ID, BLOQUE_ID, Map.of("campo", "dato"), null, AHORA, AHORA);
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId OTRO_REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final String CUERPO = "{\"valores\":{\"campo\":\"dato\"}}";

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private CrearRegistro crearRegistro;
    private ListarRegistros listarRegistros;
    private ListarReferencias listarReferencias;
    private ObtenerRegistro obtenerRegistro;
    private EditarRegistro editarRegistro;
    private EliminarRegistro eliminarRegistro;
    private ObtenerFotoOrigen obtenerFotoOrigen;
    private MockMvc mockMvc;

    private ResultActions respuesta;
    private ResultActions result;
    private byte[] bytes;

    @BeforeEach
    void setUp() {
        crearRegistro = mock(CrearRegistro.class);
        listarRegistros = mock(ListarRegistros.class);
        listarReferencias = mock(ListarReferencias.class);
        obtenerRegistro = mock(ObtenerRegistro.class);
        editarRegistro = mock(EditarRegistro.class);
        eliminarRegistro = mock(EliminarRegistro.class);
        obtenerFotoOrigen = mock(ObtenerFotoOrigen.class);
        mockMvc = MockMvcDePrueba.standalone(new RegistroController(
                        crearRegistro,
                        listarRegistros,
                        listarReferencias,
                        obtenerRegistro,
                        editarRegistro,
                        eliminarRegistro,
                        obtenerFotoOrigen,
                        new RegistroPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST crea el registro con el usuario de la sesión y responde 201")
    void pOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201() throws Exception {
        dadoElEscenarioDePOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201();

        cuandoSeEjecutaPOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201();

        entoncesSeVerificaPOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201();
    }

    @Test
    @DisplayName("GET de la foto de origen devuelve sus bytes y pasa el usuario de la sesión")
    void gETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion();

        cuandoSeEjecutaGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion();

        entoncesSeVerificaGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET de la foto de origen devuelve 404 cuando el registro no tiene foto")
    void gETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto() throws Exception {
        dadoElEscenarioDeGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto();

        cuandoSeEjecutaGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto();

        entoncesSeVerificaGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto();
    }

    @Test
    @DisplayName("POST responde 400 con el error de cada campo cuando los valores no son válidos")
    void pOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos() throws Exception {
        dadoElEscenarioDePOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos();

        cuandoSeEjecutaPOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos();

        entoncesSeVerificaPOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos();
    }

    @Test
    @DisplayName("POST responde 400 cuando falta el objeto de valores")
    void pOSTResponde400CuandoFaltaElObjetoDeValores() throws Exception {
        cuandoSeEjecutaPOSTResponde400CuandoFaltaElObjetoDeValores();

        entoncesSeVerificaPOSTResponde400CuandoFaltaElObjetoDeValores();
    }

    @Test
    @DisplayName("POST responde 400 cuando el id del bloque no es un UUID")
    void pOSTResponde400CuandoElIdDelBloqueNoEsUnUUID() throws Exception {
        cuandoSeEjecutaPOSTResponde400CuandoElIdDelBloqueNoEsUnUUID();

        entoncesSeVerificaPOSTResponde400CuandoElIdDelBloqueNoEsUnUUID();
    }

    @Test
    @DisplayName("GET lista la página con las claves que espera el frontend")
    void gETListaLaPaginaConLasClavesQueEsperaElFrontend() throws Exception {
        dadoElEscenarioDeGETListaLaPaginaConLasClavesQueEsperaElFrontend();

        cuandoSeEjecutaGETListaLaPaginaConLasClavesQueEsperaElFrontend();

        entoncesSeVerificaGETListaLaPaginaConLasClavesQueEsperaElFrontend();
    }

    @Test
    @DisplayName("GET pasa la búsqueda, el orden y la página al caso de uso")
    void gETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso() throws Exception {
        dadoElEscenarioDeGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso();

        cuandoSeEjecutaGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso();

        entoncesSeVerificaGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso();
    }

    @Test
    @DisplayName("GET responde 404 cuando el bloque no es del negocio")
    void gETResponde404CuandoElBloqueNoEsDelNegocio() throws Exception {
        dadoElEscenarioDeGETResponde404CuandoElBloqueNoEsDelNegocio();

        cuandoSeEjecutaGETResponde404CuandoElBloqueNoEsDelNegocio();

        entoncesSeVerificaGETResponde404CuandoElBloqueNoEsDelNegocio();
    }

    @Test
    @DisplayName("GET de referencias devuelve id y etiqueta de cada fila y pasa el texto buscado")
    void gETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado() throws Exception {
        dadoElEscenarioDeGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado();

        cuandoSeEjecutaGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado();

        entoncesSeVerificaGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado();
    }

    @Test
    @DisplayName("GET de referencias pasa los ids separados por coma")
    void gETDeReferenciasPasaLosIdsSeparadosPorComa() throws Exception {
        dadoElEscenarioDeGETDeReferenciasPasaLosIdsSeparadosPorComa();

        cuandoSeEjecutaGETDeReferenciasPasaLosIdsSeparadosPorComa();

        entoncesSeVerificaGETDeReferenciasPasaLosIdsSeparadosPorComa();
    }

    @Test
    @DisplayName("GET de referencias devuelve 404 cuando el bloque no es del negocio")
    void gETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio() throws Exception {
        dadoElEscenarioDeGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio();

        cuandoSeEjecutaGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio();

        entoncesSeVerificaGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio();
    }

    @Test
    @DisplayName("GET de referencias devuelve 400 cuando un id no es un UUID")
    void gETDeReferenciasDevuelve400CuandoUnIdNoEsUnUUID() throws Exception {
        cuandoSeEjecutaGETDeReferenciasDevuelve400CuandoUnIdNoEsUnUUID();

        entoncesSeVerificaGETDeReferenciasDevuelve400CuandoUnIdNoEsUnUUID();
    }

    @Test
    @DisplayName("GET devuelve el registro")
    void gETDevuelveElRegistro() throws Exception {
        dadoElEscenarioDeGETDevuelveElRegistro();

        cuandoSeEjecutaGETDevuelveElRegistro();

        entoncesSeVerificaGETDevuelveElRegistro();
    }

    @Test
    @DisplayName("GET responde 404 cuando el registro no es del negocio")
    void gETResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        dadoElEscenarioDeGETResponde404CuandoElRegistroNoEsDelNegocio();

        cuandoSeEjecutaGETResponde404CuandoElRegistroNoEsDelNegocio();

        entoncesSeVerificaGETResponde404CuandoElRegistroNoEsDelNegocio();
    }

    @Test
    @DisplayName("PATCH edita el registro con el usuario de la sesión")
    void pATCHEditaElRegistroConElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDePATCHEditaElRegistroConElUsuarioDeLaSesion();

        cuandoSeEjecutaPATCHEditaElRegistroConElUsuarioDeLaSesion();

        entoncesSeVerificaPATCHEditaElRegistroConElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("PATCH responde 400 con el error por campo cuando los datos no son válidos")
    void pATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos() throws Exception {
        dadoElEscenarioDePATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos();

        cuandoSeEjecutaPATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos();

        entoncesSeVerificaPATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos();
    }

    @Test
    @DisplayName("DELETE elimina el registro y responde 204 sin cuerpo")
    void dELETEEliminaElRegistroYResponde204SinCuerpo() throws Exception {
        cuandoSeEjecutaDELETEEliminaElRegistroYResponde204SinCuerpo();

        entoncesSeVerificaDELETEEliminaElRegistroYResponde204SinCuerpo();
    }

    @Test
    @DisplayName("DELETE responde 404 cuando el registro no es del negocio")
    void dELETEResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        dadoElEscenarioDeDELETEResponde404CuandoElRegistroNoEsDelNegocio();

        cuandoSeEjecutaDELETEResponde404CuandoElRegistroNoEsDelNegocio();

        entoncesSeVerificaDELETEResponde404CuandoElRegistroNoEsDelNegocio();
    }

    // --- dado ---

    private void dadoElEscenarioDePOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201() throws Exception {
        when(crearRegistro.execute(any(CrearRegistroRequest.class)))
                .thenReturn(new CrearRegistroResponse(ITEM));
    }

    private void dadoElEscenarioDeGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion() throws Exception {
        bytes = new byte[] {1, 2, 3};
        when(obtenerFotoOrigen.execute(any(ObtenerFotoOrigenRequest.class)))
                .thenReturn(new FotoArchivoResponse(bytes, "image/png"));
    }

    private void dadoElEscenarioDeGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto() throws Exception {
        when(obtenerFotoOrigen.execute(any(ObtenerFotoOrigenRequest.class)))
                .thenThrow(new ResourceNotFoundException("El registro no tiene foto de origen"));
    }

    private void dadoElEscenarioDePOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos() throws Exception {
        when(crearRegistro.execute(any(CrearRegistroRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("campo-1", "Este dato es obligatorio")));
    }

    private void dadoElEscenarioDeGETListaLaPaginaConLasClavesQueEsperaElFrontend() throws Exception {
        when(listarRegistros.execute(any(ListarRegistrosRequest.class)))
                .thenReturn(new ListarRegistrosResponse(List.of(ITEM), 0, 20, 1, 1,
                        new TotalesItem(1, Map.of(CAMPO_ID, new BigDecimal("1500.5")))));
    }

    private void dadoElEscenarioDeGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso() throws Exception {
        when(listarRegistros.execute(any(ListarRegistrosRequest.class)))
                .thenReturn(new ListarRegistrosResponse(List.of(), 1, 5, 0, 1, new TotalesItem(0, Map.of())));
    }

    private void dadoElEscenarioDeGETResponde404CuandoElBloqueNoEsDelNegocio() throws Exception {
        when(listarRegistros.execute(any(ListarRegistrosRequest.class)))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoElEscenarioDeGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado() throws Exception {
        when(listarReferencias.execute(any(ListarReferenciasRequest.class)))
                .thenReturn(List.of(new ReferenciaItem(REGISTRO_ID, "Ana")));
    }

    private void dadoElEscenarioDeGETDeReferenciasPasaLosIdsSeparadosPorComa() throws Exception {
        when(listarReferencias.execute(any(ListarReferenciasRequest.class))).thenReturn(List.of());
    }

    private void dadoElEscenarioDeGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio() throws Exception {
        when(listarReferencias.execute(any(ListarReferenciasRequest.class)))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoElEscenarioDeGETDevuelveElRegistro() throws Exception {
        when(obtenerRegistro.execute(any(ObtenerRegistroRequest.class)))
                .thenReturn(new ObtenerRegistroResponse(ITEM));
    }

    private void dadoElEscenarioDeGETResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        when(obtenerRegistro.execute(any(ObtenerRegistroRequest.class)))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));
    }

    private void dadoElEscenarioDePATCHEditaElRegistroConElUsuarioDeLaSesion() throws Exception {
        when(editarRegistro.execute(any(EditarRegistroRequest.class)))
                .thenReturn(new EditarRegistroResponse(ITEM));
    }

    private void dadoElEscenarioDePATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos() throws Exception {
        when(editarRegistro.execute(any(EditarRegistroRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("campo-1", "Tiene que ser un número")));
    }

    private void dadoElEscenarioDeDELETEResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        doThrow(new ResourceNotFoundException("El registro no existe"))
                .when(eliminarRegistro).execute(any(EliminarRegistroRequest.class));
    }

    // --- cuando ---

    private void cuandoSeEjecutaPOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201() throws Exception {
        result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));
    }

    private void cuandoSeEjecutaGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion() throws Exception {
        result = mockMvc.perform(get("/api/registros/{id}/foto-origen", REGISTRO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto() throws Exception {
        result = mockMvc.perform(get("/api/registros/{id}/foto-origen", REGISTRO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaPOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos() throws Exception {
        result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));
    }

    private void cuandoSeEjecutaPOSTResponde400CuandoFaltaElObjetoDeValores() throws Exception {
        result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void cuandoSeEjecutaPOSTResponde400CuandoElIdDelBloqueNoEsUnUUID() throws Exception {
        result = mockMvc.perform(post("/api/bloques/no-es-uuid/registros")
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));
    }

    private void cuandoSeEjecutaGETListaLaPaginaConLasClavesQueEsperaElFrontend() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion)
                .param("q", "harina").param("orden", "creadoEn").param("direccion", "asc")
                .param("pagina", "1").param("tamanio", "5"));
    }

    private void cuandoSeEjecutaGETResponde404CuandoElBloqueNoEsDelNegocio() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/referencias", BLOQUE_ID)
                .principal(sesion).param("q", "an"));
    }

    private void cuandoSeEjecutaGETDeReferenciasPasaLosIdsSeparadosPorComa() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/referencias", BLOQUE_ID)
                .principal(sesion).param("ids", REGISTRO_ID + "," + OTRO_REGISTRO_ID));
    }

    private void cuandoSeEjecutaGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/referencias", BLOQUE_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETDeReferenciasDevuelve400CuandoUnIdNoEsUnUUID() throws Exception {
        result = mockMvc.perform(get("/api/bloques/{id}/referencias", BLOQUE_ID)
                .principal(sesion).param("ids", "no-es-uuid"));
    }

    private void cuandoSeEjecutaGETDevuelveElRegistro() throws Exception {
        result = mockMvc.perform(get("/api/registros/{id}", REGISTRO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        result = mockMvc.perform(get("/api/registros/{id}", REGISTRO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaPATCHEditaElRegistroConElUsuarioDeLaSesion() throws Exception {
        result = mockMvc.perform(patch("/api/registros/{id}", REGISTRO_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));
    }

    private void cuandoSeEjecutaPATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos() throws Exception {
        result = mockMvc.perform(patch("/api/registros/{id}", REGISTRO_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));
    }

    private void cuandoSeEjecutaDELETEEliminaElRegistroYResponde204SinCuerpo() throws Exception {
        result = mockMvc.perform(delete("/api/registros/{id}", REGISTRO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaDELETEResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        result = mockMvc.perform(delete("/api/registros/{id}", REGISTRO_ID).principal(sesion));
    }

    // --- entonces ---

    private void entoncesSeVerificaPOSTCreaElRegistroConElUsuarioDeLaSesionYResponde201() throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$.bloqueId").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$.valores.campo").value("dato"));
        ArgumentCaptor<CrearRegistroRequest> request = ArgumentCaptor.forClass(CrearRegistroRequest.class);
        verify(crearRegistro).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(request.getValue().valores()).containsEntry("campo", "dato");
    }

    private void entoncesSeVerificaGETDeLaFotoDeOrigenDevuelveSusBytesYPasaElUsuarioDeLaSesion() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(bytes));
        verify(obtenerFotoOrigen).execute(new ObtenerFotoOrigenRequest(USUARIO_ID, REGISTRO_ID));
    }

    private void entoncesSeVerificaGETDeLaFotoDeOrigenDevuelve404CuandoElRegistroNoTieneFoto() throws Exception {
        result.andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaPOSTResponde400ConElErrorDeCadaCampoCuandoLosValoresNoSonValidos() throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisá los datos marcados"))
                .andExpect(jsonPath("$.errors.campo-1").value("Este dato es obligatorio"));
    }

    private void entoncesSeVerificaPOSTResponde400CuandoFaltaElObjetoDeValores() throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.valores").value("Los valores son obligatorios"));
        verifyNoInteractions(crearRegistro);
    }

    private void entoncesSeVerificaPOSTResponde400CuandoElIdDelBloqueNoEsUnUUID() throws Exception {
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(crearRegistro);
    }

    private void entoncesSeVerificaGETListaLaPaginaConLasClavesQueEsperaElFrontend() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.totales.cantidad").value(1))
                .andExpect(jsonPath("$.totales.sumas['" + CAMPO_ID + "']").value(1500.5));
    }

    private void entoncesSeVerificaGETPasaLaBusquedaElOrdenYLaPaginaAlCasoDeUso() throws Exception {
        ArgumentCaptor<ListarRegistrosRequest> request = ArgumentCaptor.forClass(ListarRegistrosRequest.class);
        verify(listarRegistros).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(request.getValue().texto()).isEqualTo("harina");
        assertThat(request.getValue().orden()).isEqualTo("creadoEn");
        assertThat(request.getValue().direccion()).isEqualTo("asc");
        assertThat(request.getValue().pagina()).isEqualTo(1);
        assertThat(request.getValue().tamanio()).isEqualTo(5);
    }

    private void entoncesSeVerificaGETResponde404CuandoElBloqueNoEsDelNegocio() throws Exception {
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("El bloque no existe"));
    }

    private void entoncesSeVerificaGETDeReferenciasDevuelveIdYEtiquetaDeCadaFilaYPasaElTextoBuscado() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$[0].etiqueta").value("Ana"));
        verify(listarReferencias).execute(new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, "an", List.of()));
    }

    private void entoncesSeVerificaGETDeReferenciasPasaLosIdsSeparadosPorComa() throws Exception {
        result.andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(listarReferencias).execute(new ListarReferenciasRequest(
                USUARIO_ID, BLOQUE_ID, null, List.of(REGISTRO_ID, OTRO_REGISTRO_ID)));
    }

    private void entoncesSeVerificaGETDeReferenciasDevuelve404CuandoElBloqueNoEsDelNegocio() throws Exception {
        result.andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaGETDeReferenciasDevuelve400CuandoUnIdNoEsUnUUID() throws Exception {
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(listarReferencias);
    }

    private void entoncesSeVerificaGETDevuelveElRegistro() throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(REGISTRO_ID.toString()));
        ArgumentCaptor<ObtenerRegistroRequest> request = ArgumentCaptor.forClass(ObtenerRegistroRequest.class);
        verify(obtenerRegistro).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
    }

    private void entoncesSeVerificaGETResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        result.andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaPATCHEditaElRegistroConElUsuarioDeLaSesion() throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.valores.campo").value("dato"));
        ArgumentCaptor<EditarRegistroRequest> request = ArgumentCaptor.forClass(EditarRegistroRequest.class);
        verify(editarRegistro).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
        assertThat(request.getValue().valores()).containsEntry("campo", "dato");
    }

    private void entoncesSeVerificaPATCHResponde400ConElErrorPorCampoCuandoLosDatosNoSonValidos() throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.campo-1").value("Tiene que ser un número"));
    }

    private void entoncesSeVerificaDELETEEliminaElRegistroYResponde204SinCuerpo() throws Exception {
        result.andExpect(status().isNoContent());
        ArgumentCaptor<EliminarRegistroRequest> request = ArgumentCaptor.forClass(EliminarRegistroRequest.class);
        verify(eliminarRegistro).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
    }

    private void entoncesSeVerificaDELETEResponde404CuandoElRegistroNoEsDelNegocio() throws Exception {
        result.andExpect(status().isNotFound());
    }
}
