package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.cartel.OpcionDeCartelItem;
import com.ensambla.backend.domain.useCase.cartel.PromocionSugeridaItem;
import com.ensambla.backend.domain.useCase.cartel.TextosDeCartelItem;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromocionesResponse;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartel;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartelRequest;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartelControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");

    private final ArmarOpcionesDeCartel armarOpciones = mock(ArmarOpcionesDeCartel.class);
    private final RedactarTextosDeCartel redactarTextos = mock(RedactarTextosDeCartel.class);
    private final SugerirPromociones sugerirPromociones = mock(SugerirPromociones.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcDePrueba.standalone(new CartelController(armarOpciones, redactarTextos, sugerirPromociones, new CartelPresentationMapper()))
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

    @Test
    @DisplayName("POST de textos redacta con el usuario de sesión y dice si lo hizo la IA")
    void redactarTextosShouldReturnTheTexts() throws Exception {
        when(redactarTextos.execute(any())).thenReturn(new TextosDeCartelItem(
                TipoDeCartel.NOVEDAD, "Llegó la yerba", "Ya está", "Posteo", true));

        respuesta = mockMvc.perform(post("/api/carteles/textos").principal(principal)
                .contentType("application/json")
                .content("{\"tiposPosibles\":[\"NOVEDAD\"],\"productos\":[\"Yerba\"],\"numeros\":[2500.5]}"));

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.plantilla").value("NOVEDAD"))
                .andExpect(jsonPath("$.titular").value("Llegó la yerba"))
                .andExpect(jsonPath("$.redactadoConIa").value(true));
        verify(redactarTextos).execute(new RedactarTextosDeCartelRequest(USUARIO_ID, List.of(TipoDeCartel.NOVEDAD),
                List.of("Yerba"), List.of(new BigDecimal("2500.5"))));
    }

    @Test
    @DisplayName("POST de textos rechaza un pedido sin tipos posibles")
    void redactarTextosShouldRejectNoTypes() throws Exception {
        respuesta = mockMvc.perform(post("/api/carteles/textos").principal(principal)
                .contentType("application/json")
                .content("{\"tiposPosibles\":[],\"productos\":[],\"numeros\":[]}"));

        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(redactarTextos);
    }

    @Test
    @DisplayName("GET de promociones sugeridas devuelve los números calculados por el servidor")
    void sugerirPromocionesShouldReturnTheCalculatedNumbers() throws Exception {
        when(sugerirPromociones.execute(any())).thenReturn(new SugerirPromocionesResponse(List.of(
                new PromocionSugeridaItem(TipoDeCartel.PROMO_PRECIO, List.of("Aceite"), new BigDecimal("2500"),
                        new BigDecimal("2000"), new BigDecimal("500"), new BigDecimal("20")))));

        respuesta = mockMvc.perform(get("/api/carteles/promociones-sugeridas").principal(principal));

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.promociones[0].tipo").value("PROMO_PRECIO"))
                .andExpect(jsonPath("$.promociones[0].productos[0]").value("Aceite"))
                .andExpect(jsonPath("$.promociones[0].ahorro").value(500))
                .andExpect(jsonPath("$.promociones[0].descuentoPorcentaje").value(20));
        verify(sugerirPromociones).execute(new SugerirPromocionesRequest(USUARIO_ID));
    }
}
