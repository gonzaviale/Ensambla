package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.receta.RecetaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecetaControllerTest {

    private String cuerpo;
    private ResultActions resultado;

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final BloqueId BLOQUE_INSUMOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final RecetaItem ITEM = new RecetaItem(
            BLOQUE_INSUMOS_ID,
            PRECIO_ID,
            List.of(new RecetaItem.LineaItem(
                    HARINA_ID, "Harina", new BigDecimal("0.5"), "kg", new BigDecimal("1200"), new BigDecimal("600"))),
            new BigDecimal("600"),
            false);

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ObtenerReceta obtenerReceta;
    private GuardarReceta guardarReceta;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        obtenerReceta = mock(ObtenerReceta.class);
        guardarReceta = mock(GuardarReceta.class);
        mockMvc = MockMvcDePrueba.standalone(new RecetaController(
                        obtenerReceta, guardarReceta, new RecetaPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve la receta con el costo y pasa el usuario de la sesión")
    void gETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion();

        cuandoSeEjecutaGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion();

        entoncesSeVerificaGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET de un producto ajeno responde 404")
    void gETDeUnProductoAjenoResponde404() throws Exception {
        dadoElEscenarioDeGETDeUnProductoAjenoResponde404();

        cuandoSeEjecutaGETDeUnProductoAjenoResponde404();

        entoncesSeVerificaGETDeUnProductoAjenoResponde404();
    }

    @Test
    @DisplayName("PUT guarda la receta con las líneas y devuelve el costo")
    void pUTGuardaLaRecetaConLasLineasYDevuelveElCosto() throws Exception {
        dadoElEscenarioDePUTGuardaLaRecetaConLasLineasYDevuelveElCosto();

        cuandoSeEjecutaPUTGuardaLaRecetaConLasLineasYDevuelveElCosto();

        entoncesSeVerificaPUTGuardaLaRecetaConLasLineasYDevuelveElCosto();
    }

    @Test
    @DisplayName("PUT sin líneas quita la receta")
    void pUTSinLineasQuitaLaReceta() throws Exception {
        dadoElEscenarioDePUTSinLineasQuitaLaReceta();

        cuandoSeEjecutaPUTSinLineasQuitaLaReceta();

        entoncesSeVerificaPUTSinLineasQuitaLaReceta();
    }

    @Test
    @DisplayName("PUT con datos inválidos responde 400 con el mensaje de cada línea")
    void pUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea() throws Exception {
        dadoElEscenarioDePUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea();

        cuandoSeEjecutaPUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea();

        entoncesSeVerificaPUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea();
    }

    // --- dado ---

    private void dadoElEscenarioDeGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion() throws Exception {
        when(obtenerReceta.execute(any(ObtenerRecetaRequest.class))).thenReturn(ITEM);
    }

    private void dadoElEscenarioDeGETDeUnProductoAjenoResponde404() throws Exception {
        when(obtenerReceta.execute(any(ObtenerRecetaRequest.class)))
                        .thenThrow(new ResourceNotFoundException("El registro no existe"));
    }

    private void dadoElEscenarioDePUTGuardaLaRecetaConLasLineasYDevuelveElCosto() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class))).thenReturn(ITEM);
        cuerpo = "{\"campoPrecioId\":\"" + PRECIO_ID + "\",\"lineas\":[{\"insumoId\":\"" + HARINA_ID
                        + "\",\"cantidad\":0.5,\"unidad\":\"kg\"}]}";
    }

    private void dadoElEscenarioDePUTSinLineasQuitaLaReceta() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class))).thenReturn(ITEM);
    }

    private void dadoElEscenarioDePUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea() throws Exception {
        when(guardarReceta.execute(any(GuardarRecetaRequest.class)))
                        .thenThrow(new RegistroInvalidoException(Map.of("lineas[0]", "Escribí la cantidad")));
    }

    // --- cuando ---

    private void cuandoSeEjecutaGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion() throws Exception {
        resultado = mockMvc.perform(get("/api/registros/{id}/receta", PRODUCTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaGETDeUnProductoAjenoResponde404() throws Exception {
        resultado = mockMvc.perform(get("/api/registros/{id}/receta", PRODUCTO_ID).principal(sesion));
    }

    private void cuandoSeEjecutaPUTGuardaLaRecetaConLasLineasYDevuelveElCosto() throws Exception {
        resultado = mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void cuandoSeEjecutaPUTSinLineasQuitaLaReceta() throws Exception {
        resultado = mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void cuandoSeEjecutaPUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea() throws Exception {
        resultado = mockMvc.perform(put("/api/registros/{id}/receta", PRODUCTO_ID)
                                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    // --- entonces ---

    private void entoncesSeVerificaGETDevuelveLaRecetaConElCostoYPasaElUsuarioDeLaSesion() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.bloqueInsumosId").value(BLOQUE_INSUMOS_ID.toString()))
                        .andExpect(jsonPath("$.campoPrecioId").value(PRECIO_ID.toString()))
                        .andExpect(jsonPath("$.costo").value(600))
                        .andExpect(jsonPath("$.incompleta").value(false))
                        .andExpect(jsonPath("$.lineas[0].insumo").value("Harina"))
                        .andExpect(jsonPath("$.lineas[0].cantidad").value(0.5))
                        .andExpect(jsonPath("$.lineas[0].precioUnitario").value(1200))
                        .andExpect(jsonPath("$.lineas[0].subtotal").value(600));
        verify(obtenerReceta).execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));
    }

    private void entoncesSeVerificaGETDeUnProductoAjenoResponde404() throws Exception {
        resultado.andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaPUTGuardaLaRecetaConLasLineasYDevuelveElCosto() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.costo").value(600));
        verify(guardarReceta).execute(new GuardarRecetaRequest(
                        USUARIO_ID, PRODUCTO_ID, PRECIO_ID,
                        List.of(new LineaRecetaRequest(HARINA_ID, new BigDecimal("0.5"), "kg"))));
    }

    private void entoncesSeVerificaPUTSinLineasQuitaLaReceta() throws Exception {
        resultado.andExpect(status().isOk());
        verify(guardarReceta).execute(new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of()));
    }

    private void entoncesSeVerificaPUTConDatosInvalidosResponde400ConElMensajeDeCadaLinea() throws Exception {
        resultado.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errors['lineas[0]']").value("Escribí la cantidad"));
    }
}
