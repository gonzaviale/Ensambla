package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.marca.KitDeMarcaItem;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaResponse;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.marca.MarcaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarcaControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final KitDeMarcaItem KIT =
            new KitDeMarcaItem("#CC0000", "#00CC00", Tipografia.NUNITO, TonoDeMarca.DIVERTIDO, true);
    private static final byte[] IMAGEN = {1, 2, 3};

    private final ObtenerKitDeMarca obtenerKit = mock(ObtenerKitDeMarca.class);
    private final GuardarKitDeMarca guardarKit = mock(GuardarKitDeMarca.class);
    private final SubirLogoDeMarca subirLogo = mock(SubirLogoDeMarca.class);
    private final ObtenerLogoDeMarca obtenerLogo = mock(ObtenerLogoDeMarca.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        var controller = new MarcaController(
                obtenerKit, guardarKit, subirLogo, obtenerLogo, new MarcaPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve el kit de marca del usuario de sesión")
    void obtenerShouldReturnTheKit() throws Exception {
        givenQueElKitEs(KIT);

        whenSePideElKit();

        thenSeResponde200ConElKit();
        verify(obtenerKit).execute(new ObtenerKitDeMarcaRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("PUT guarda el kit con el usuario de sesión")
    void guardarShouldSaveForSessionUser() throws Exception {
        when(guardarKit.execute(any())).thenReturn(KIT);

        whenSeGuardaElKit("#CC0000");

        thenSeResponde200ConElKit();
        verify(guardarKit).execute(
                new GuardarKitDeMarcaRequest(USUARIO_ID, "#CC0000", "#00CC00", Tipografia.NUNITO, TonoDeMarca.DIVERTIDO));
    }

    @Test
    @DisplayName("PUT rechaza un color mal escrito sin tocar el caso de uso")
    void guardarShouldRejectBadColor() throws Exception {
        whenSeGuardaElKit("rojo");

        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(guardarKit);
    }

    @Test
    @DisplayName("PUT del logo devuelve el kit y los colores sugeridos")
    void subirLogoShouldReturnKitAndSuggestions() throws Exception {
        when(subirLogo.execute(any())).thenReturn(new SubirLogoDeMarcaResponse(KIT, "#F08C14", "#00506A"));

        whenSeSubeElLogo();

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.kit.tieneLogo").value(true))
                .andExpect(jsonPath("$.colorPrincipalSugerido").value("#F08C14"))
                .andExpect(jsonPath("$.colorSecundarioSugerido").value("#00506A"));
        verify(subirLogo).execute(new SubirLogoDeMarcaRequest(USUARIO_ID, IMAGEN, "image/png"));
    }

    @Test
    @DisplayName("GET del logo devuelve la imagen con su tipo")
    void obtenerLogoShouldReturnTheImage() throws Exception {
        when(obtenerLogo.execute(any())).thenReturn(new ObtenerLogoDeMarcaResponse(IMAGEN, "image/png"));

        respuesta = mockMvc.perform(get("/api/marca/logo").principal(principal));

        respuesta.andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(IMAGEN));
        verify(obtenerLogo).execute(new ObtenerLogoDeMarcaRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET del logo responde 404 si el negocio no tiene logo")
    void obtenerLogoShouldRespond404WithoutLogo() throws Exception {
        when(obtenerLogo.execute(any())).thenThrow(new ResourceNotFoundException("sin logo"));

        respuesta = mockMvc.perform(get("/api/marca/logo").principal(principal));

        respuesta.andExpect(status().isNotFound());
    }

    private void givenQueElKitEs(KitDeMarcaItem kit) {
        when(obtenerKit.execute(any())).thenReturn(kit);
    }

    private void whenSePideElKit() throws Exception {
        respuesta = mockMvc.perform(get("/api/marca").principal(principal));
    }

    private void whenSeGuardaElKit(String colorPrincipal) throws Exception {
        respuesta = mockMvc.perform(put("/api/marca").principal(principal).contentType("application/json")
                .content("{\"colorPrincipal\":\"" + colorPrincipal + "\",\"colorSecundario\":\"#00CC00\","
                        + "\"tipografia\":\"NUNITO\",\"tono\":\"DIVERTIDO\"}"));
    }

    private void whenSeSubeElLogo() throws Exception {
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", IMAGEN);
        respuesta = mockMvc.perform(multipart("/api/marca/logo").file(logo).principal(principal)
                .with(pedido -> {
                    pedido.setMethod("PUT");
                    return pedido;
                }));
    }

    private void thenSeResponde200ConElKit() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.colorPrincipal").value("#CC0000"))
                .andExpect(jsonPath("$.colorSecundario").value("#00CC00"))
                .andExpect(jsonPath("$.tipografia").value("NUNITO"))
                .andExpect(jsonPath("$.tono").value("DIVERTIDO"))
                .andExpect(jsonPath("$.tieneLogo").value(true));
    }
}
