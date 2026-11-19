package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.domain.useCase.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLote;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLote;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLoteRequest;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLote;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.lote.LotePresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

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
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        LoteController controller = new LoteController(
                subirLote,
                obtenerLote,
                obtenerFotoLote,
                new LotePresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST devuelve 201 con el id del lote y su ubicación")
    void postDevuelve201ConElIdDelLoteYSuUbicacion() throws Exception {
        dadoUnLoteQueSePuedeSubir();

        cuandoSeSubenLasFotos(foto(JPEG));

        entoncesSeCreaElLoteConSuUbicacion();
    }

    @Test
    @DisplayName("POST manda al use case el usuario de sesión y los bytes de cada foto en orden")
    void postMandaAlUseCaseElUsuarioDeSesionYLosBytesDeCadaFotoEnOrden() throws Exception {
        dadoUnLoteQueSePuedeSubir();

        cuandoSeSubenLasFotos(foto(JPEG), foto(PNG));

        entoncesElUseCaseRecibeAlUsuarioYLasFotosEnOrden();
    }

    @Test
    @DisplayName("POST con bloqueDestinoId lo manda al use case")
    void postConBloqueDestinoIdLoMandaAlUseCase() throws Exception {
        dadoUnLoteQueSePuedeSubir();

        cuandoSeSubenLasFotosAlBloque(BLOQUE_ID, foto(JPEG));

        entoncesElUseCaseRecibeElBloqueDeDestino();
    }

    @Test
    @DisplayName("POST sin bloqueDestinoId deja que la IA proponga el bloque")
    void postSinBloqueDestinoIdDejaQueLaIaProponga() throws Exception {
        dadoUnLoteQueSePuedeSubir();

        cuandoSeSubenLasFotos(foto(JPEG));

        entoncesElUseCaseRecibeElBloqueDeDestinoVacio();
    }

    @Test
    @DisplayName("POST sin fotos devuelve 400 y no llega al use case")
    void postSinFotosDevuelve400YNoLlegaAlUseCase() throws Exception {
        cuandoSeSubenLasFotos();

        entoncesSeRechazaPorqueFaltanLasFotos();
    }

    @Test
    @DisplayName("POST con una foto inválida devuelve 400 con el mensaje del dominio")
    void postConUnaFotoInvalidaDevuelve400ConElMensajeDelDominio() throws Exception {
        dadoUnaFotoInvalida();

        cuandoSeSubenLasFotos(foto(new byte[]{1, 2}));

        entoncesSeRechazaLaFotoConElMensajeDelDominio();
    }

    @Test
    @DisplayName("GET devuelve el lote con sus fotos")
    void getDevuelveElLoteConSusFotos() throws Exception {
        dadoUnLoteExistente();

        cuandoSePideElLote();

        entoncesSeDevuelveElLoteConSuFoto();
        entoncesSeConsultaElLoteDelUsuario();
    }

    @Test
    @DisplayName("GET de un lote de otro negocio devuelve 404")
    void getDeUnLoteDeOtroNegocioDevuelve404() throws Exception {
        dadoUnLoteAjeno();

        cuandoSePideElLote();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("GET devuelve 400 cuando el id no es un UUID")
    void getDevuelve400CuandoElIdNoEsUnUuid() throws Exception {
        cuandoSePideUnLoteConIdInvalido();

        entoncesSeRechazaElIdSinConsultarElLote();
    }

    @Test
    @DisplayName("GET de una foto devuelve sus bytes con su tipo y caché privada")
    void getDeUnaFotoDevuelveSusBytesConSuTipoYCachePrivada() throws Exception {
        dadoUnaFotoDeLote();

        cuandoSePideLaFotoDelLote();

        entoncesSeDevuelvenLosBytesConSuTipoYCachePrivada();
        entoncesSeConsultaLaFotoDelUsuario();
    }

    @Test
    @DisplayName("GET de una foto de otro negocio devuelve 404")
    void getDeUnaFotoDeOtroNegocioDevuelve404() throws Exception {
        dadoUnaFotoAjena();

        cuandoSePideLaFotoDelLote();

        entoncesRespondeNoEncontrado();
    }

    // --- dado ---

    private void dadoUnLoteQueSePuedeSubir() {
        when(subirLote.execute(any())).thenReturn(unLote());
    }

    private void dadoUnaFotoInvalida() {
        when(subirLote.execute(any())).thenThrow(
                new BusinessRuleException("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
    }

    private void dadoUnLoteExistente() {
        when(obtenerLote.execute(any())).thenReturn(unLote());
    }

    private void dadoUnLoteAjeno() {
        when(obtenerLote.execute(any())).thenThrow(new ResourceNotFoundException("Lote no encontrado"));
    }

    private void dadoUnaFotoDeLote() {
        when(obtenerFotoLote.execute(any())).thenReturn(new FotoArchivoResponse(JPEG, "image/jpeg"));
    }

    private void dadoUnaFotoAjena() {
        when(obtenerFotoLote.execute(any())).thenThrow(new ResourceNotFoundException("Foto no encontrada"));
    }

    // --- cuando ---

    private void cuandoSeSubenLasFotos(MockMultipartFile... fotos) throws Exception {
        cuandoSeSubenLasFotosAlBloque(null, fotos);
    }

    private void cuandoSeSubenLasFotosAlBloque(BloqueId bloqueDestinoId, MockMultipartFile... fotos) throws Exception {
        var pedido = multipart("/api/lotes");
        if (bloqueDestinoId != null) {
            pedido.param("bloqueDestinoId", bloqueDestinoId.toString());
        }
        for (MockMultipartFile foto : fotos) {
            pedido.file(foto);
        }
        respuesta = mockMvc.perform(pedido.principal(principal));
    }

    private void cuandoSePideElLote() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}", LOTE_ID).principal(principal));
    }

    private void cuandoSePideUnLoteConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/id-invalido").principal(principal));
    }

    private void cuandoSePideLaFotoDelLote() throws Exception {
        respuesta = mockMvc.perform(get("/api/lotes/{id}/fotos/{fotoId}", LOTE_ID, FOTO_ID).principal(principal));
    }

    // --- entonces ---

    private void entoncesSeCreaElLoteConSuUbicacion() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/lotes/" + LOTE_ID))
                .andExpect(jsonPath("$.id").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.creadoEn").exists())
                .andExpect(jsonPath("$.fotos[0].id").value(FOTO_ID.toString()))
                .andExpect(jsonPath("$.fotos[0].posicion").value(0))
                .andExpect(jsonPath("$.fotos[0].tipoContenido").value("image/jpeg"))
                .andExpect(jsonPath("$.fotos[0].tamanioBytes").value(JPEG.length));
    }

    private void entoncesElUseCaseRecibeAlUsuarioYLasFotosEnOrden() {
        assertThat(pedidoDeSubida().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeSubida().fotos()).containsExactly(JPEG, PNG);
    }

    private void entoncesElUseCaseRecibeElBloqueDeDestino() {
        assertThat(pedidoDeSubida().bloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    private void entoncesElUseCaseRecibeElBloqueDeDestinoVacio() {
        assertThat(pedidoDeSubida().bloqueDestinoId()).isNull();
    }

    private void entoncesSeRechazaPorqueFaltanLasFotos() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro: fotos"));
        verifyNoInteractions(subirLote);
    }

    private void entoncesSeRechazaLaFotoConElMensajeDelDominio() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
    }

    private void entoncesSeDevuelveElLoteConSuFoto() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.fotos[0].id").value(FOTO_ID.toString()));
    }

    private void entoncesSeConsultaElLoteDelUsuario() {
        verify(obtenerLote).execute(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeRechazaElIdSinConsultarElLote() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerLote);
    }

    private void entoncesSeDevuelvenLosBytesConSuTipoYCachePrivada() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("Cache-Control", "max-age=300, private"));
    }

    private void entoncesSeConsultaLaFotoDelUsuario() {
        verify(obtenerFotoLote).execute(new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, FOTO_ID));
    }

    // --- datos ---

    private SubirLoteRequest pedidoDeSubida() {
        ArgumentCaptor<SubirLoteRequest> pedido = ArgumentCaptor.forClass(SubirLoteRequest.class);
        verify(subirLote).execute(pedido.capture());
        return pedido.getValue();
    }

    private MockMultipartFile foto(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina.jpg", "image/jpeg", contenido);
    }

    private LoteItem unLote() {
        return new LoteItem(LOTE_ID, "PENDIENTE", AHORA, List.of(new FotoItem(
                FOTO_ID, (short) 0, "image/jpeg", (long) JPEG.length)));
    }
}
