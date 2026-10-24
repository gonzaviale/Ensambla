package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.useCase.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLote;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLote;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLoteRequest;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLote;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.lote.LotePresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoteControllerTest {

    private final SubirLote subirLote = mock(SubirLote.class);
    private final ObtenerLote obtenerLote = mock(ObtenerLote.class);
    private final ObtenerFotoLote obtenerFotoLote = mock(ObtenerFotoLote.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoteController controller = new LoteController(
                subirLote,
                obtenerLote,
                obtenerFotoLote,
                new LotePresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST devuelve 201 con el id del lote y su ubicación")
    void subirShouldReturnCreatedLote() throws Exception {
        givenLoteSubido();

        ResultActions result = subir(foto(JPEG));

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/lotes/" + LOTE_ID))
                .andExpect(jsonPath("$.id").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.creadoEn").exists())
                .andExpect(jsonPath("$.fotos[0].id").value(FOTO_ID.toString()))
                .andExpect(jsonPath("$.fotos[0].posicion").value(0))
                .andExpect(jsonPath("$.fotos[0].tipoContenido").value("image/jpeg"))
                .andExpect(jsonPath("$.fotos[0].tamanioBytes").value(JPEG.length));
    }

    @Test
    @DisplayName("POST manda al use case el usuario de sesión y los bytes de cada foto en orden")
    void subirShouldPassSessionUsuarioAndFotos() throws Exception {
        givenLoteSubido();

        subir(foto(JPEG), foto(PNG));

        SubirLoteRequest request = thenSubirRequest();
        assertThat(request.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.fotos()).containsExactly(JPEG, PNG);
    }

    @Test
    @DisplayName("POST con bloqueDestinoId lo manda al use case")
    void subirShouldPassBloqueDestino() throws Exception {
        givenLoteSubido();

        subir(BLOQUE_ID, foto(JPEG));

        assertThat(thenSubirRequest().bloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    @Test
    @DisplayName("POST sin bloqueDestinoId deja que la IA proponga el bloque")
    void subirShouldLeaveBloqueDestinoEmptyWhenMissing() throws Exception {
        givenLoteSubido();

        subir(foto(JPEG));

        assertThat(thenSubirRequest().bloqueDestinoId()).isNull();
    }

    @Test
    @DisplayName("POST sin fotos devuelve 400 y no llega al use case")
    void subirShouldReturn400WhenFotosMissing() throws Exception {
        ResultActions result = subir();

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro: fotos"));
        verifyNoInteractions(subirLote);
    }

    @Test
    @DisplayName("POST con una foto inválida devuelve 400 con el mensaje del dominio")
    void subirShouldReturn400WhenFotoIsInvalid() throws Exception {
        givenFotoInvalida();

        ResultActions result = subir(foto(new byte[]{1, 2}));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
    }

    @Test
    @DisplayName("GET devuelve el lote con sus fotos")
    void obtenerShouldReturnLote() throws Exception {
        givenLote();

        ResultActions result = mockMvc.perform(get("/api/lotes/{id}", LOTE_ID).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos[0].id").value(FOTO_ID.toString()));
        verify(obtenerLote).execute(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    @Test
    @DisplayName("GET de un lote de otro negocio devuelve 404")
    void obtenerShouldReturn404WhenLoteIsForeign() throws Exception {
        givenLoteAjeno();

        ResultActions result = mockMvc.perform(get("/api/lotes/{id}", LOTE_ID).principal(principal));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET devuelve 400 cuando el id no es un UUID")
    void obtenerShouldReturn400WhenIdIsNotUuid() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/lotes/id-invalido").principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerLote);
    }

    @Test
    @DisplayName("GET de una foto devuelve sus bytes con su tipo y caché privada")
    void fotoShouldReturnBytesWithTypeAndPrivateCache() throws Exception {
        when(obtenerFotoLote.execute(any())).thenReturn(new FotoArchivoResponse(JPEG, "image/jpeg"));

        ResultActions result = mockMvc.perform(
                get("/api/lotes/{id}/fotos/{fotoId}", LOTE_ID, FOTO_ID).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("Cache-Control", "max-age=300, private"));
        verify(obtenerFotoLote).execute(new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, FOTO_ID));
    }

    @Test
    @DisplayName("GET de una foto de otro negocio devuelve 404")
    void fotoShouldReturn404WhenFotoIsForeign() throws Exception {
        when(obtenerFotoLote.execute(any())).thenThrow(new ResourceNotFoundException("Foto no encontrada"));

        ResultActions result = mockMvc.perform(
                get("/api/lotes/{id}/fotos/{fotoId}", LOTE_ID, FOTO_ID).principal(principal));

        result.andExpect(status().isNotFound());
    }

    // --- arrange ---
    private void givenLoteSubido() {
        when(subirLote.execute(any())).thenReturn(lote());
    }

    private void givenFotoInvalida() {
        when(subirLote.execute(any())).thenThrow(
                new BusinessRuleException("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
    }

    private void givenLote() {
        when(obtenerLote.execute(any())).thenReturn(lote());
    }

    private void givenLoteAjeno() {
        when(obtenerLote.execute(any())).thenThrow(new ResourceNotFoundException("Lote no encontrado"));
    }

    // --- act ---
    private ResultActions subir(MockMultipartFile... fotos) throws Exception {
        return subir(null, fotos);
    }

    private ResultActions subir(UUID bloqueDestinoId, MockMultipartFile... fotos) throws Exception {
        var request = multipart("/api/lotes");
        if (bloqueDestinoId != null) {
            request.param("bloqueDestinoId", bloqueDestinoId.toString());
        }
        for (MockMultipartFile foto : fotos) {
            request.file(foto);
        }
        return mockMvc.perform(request.principal(principal));
    }

    // --- assert ---
    private SubirLoteRequest thenSubirRequest() {
        ArgumentCaptor<SubirLoteRequest> request = ArgumentCaptor.forClass(SubirLoteRequest.class);
        verify(subirLote).execute(request.capture());
        return request.getValue();
    }

    // --- helpers ---
    private MockMultipartFile foto(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina.jpg", "image/jpeg", contenido);
    }

    private LoteItem lote() {
        return new LoteItem(LOTE_ID, "PENDIENTE", AHORA, List.of(new FotoItem(
                FOTO_ID, (short) 0, "image/jpeg", (long) JPEG.length)));
    }
}
