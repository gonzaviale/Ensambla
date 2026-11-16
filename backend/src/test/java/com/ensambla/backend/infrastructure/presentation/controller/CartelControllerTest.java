package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.cartel.OpcionDeCartelItem;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelRequest;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.cartel.CartelPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartelControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ArmarOpcionesDeCartel armarOpciones = mock(ArmarOpcionesDeCartel.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcDePrueba.standalone(new CartelController(armarOpciones, new CartelPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST arma las opciones del usuario de sesión y devuelve el dibujo de cada una")
    void armarOpcionesShouldReturnTheDrawings() throws Exception {
        when(armarOpciones.execute(any())).thenReturn(new ArmarOpcionesDeCartelResponse(
                List.of(new OpcionDeCartelItem(VarianteDeComposicion.BLOQUE, "<svg/>"))));

        whenSePide("\"lineas\":[\"a\"]");

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones[0].variante").value("BLOQUE"))
                .andExpect(jsonPath("$.opciones[0].dibujo").value("<svg/>"));
        verify(armarOpciones).execute(new ArmarOpcionesDeCartelRequest(USUARIO_ID, TipoDeCartel.PROMO_PRECIO,
                FormatoDeCartel.A4, "Titular", "Bajada", "$1", "Detalle", List.of("a")));
    }

    @Test
    @DisplayName("POST sin líneas las toma como una lista vacía")
    void armarOpcionesShouldDefaultMissingLines() throws Exception {
        when(armarOpciones.execute(any())).thenReturn(new ArmarOpcionesDeCartelResponse(List.of()));

        whenSePide("\"extra\":1");

        respuesta.andExpect(status().isOk());
        verify(armarOpciones).execute(new ArmarOpcionesDeCartelRequest(USUARIO_ID, TipoDeCartel.PROMO_PRECIO,
                FormatoDeCartel.A4, "Titular", "Bajada", "$1", "Detalle", List.of()));
    }

    @Test
    @DisplayName("POST rechaza un titular vacío sin tocar el caso de uso")
    void armarOpcionesShouldRejectBlankTitular() throws Exception {
        respuesta = mockMvc.perform(post("/api/carteles/opciones").principal(principal)
                .contentType("application/json")
                .content("{\"tipo\":\"PROMO_PRECIO\",\"formato\":\"A4\",\"titular\":\"\"}"));

        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(armarOpciones);
    }

    private void whenSePide(String extra) throws Exception {
        respuesta = mockMvc.perform(post("/api/carteles/opciones").principal(principal)
                .contentType("application/json")
                .content("{\"tipo\":\"PROMO_PRECIO\",\"formato\":\"A4\",\"titular\":\"Titular\",\"bajada\":\"Bajada\","
                        + "\"protagonista\":\"$1\",\"detalle\":\"Detalle\"," + extra + "}"));
    }
}
