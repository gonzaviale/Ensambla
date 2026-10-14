package com.ensambla.backend.infrastructure.presentation.mapper.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.dto.lote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.dto.lote.ObtenerLoteRequest;
import com.ensambla.backend.domain.dto.lote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse.FotoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LotePresentationMapperTest {

    private LotePresentationMapper lotePresentationMapper;

    @BeforeEach
    void setUp() {
        lotePresentationMapper = new LotePresentationMapper();
    }

    @Test
    @DisplayName("Arma el pedido de subida con el usuario y los bytes de cada archivo en orden")
    void toSubirRequestShouldReadBytesInOrder() {
        List<MultipartFile> fotos = List.of(archivo(JPEG), archivo(PNG));

        SubirLoteRequest result = lotePresentationMapper.toSubirRequest(USUARIO_ID, fotos, null);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.fotos()).containsExactly(JPEG, PNG);
        assertThat(result.bloqueDestinoId()).isNull();
    }

    @Test
    @DisplayName("Arma el pedido de subida con el bloque destino elegido")
    void toSubirRequestShouldKeepBloqueDestino() {
        SubirLoteRequest result = lotePresentationMapper.toSubirRequest(
                USUARIO_ID, List.of(archivo(JPEG)), BLOQUE_ID);

        assertThat(result.bloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    @Test
    @DisplayName("Si no puede leer un archivo recibido falla con un error de lectura")
    void toSubirRequestShouldFailWhenFileCannotBeRead() throws IOException {
        List<MultipartFile> fotos = List.of(givenUnreadableFile());

        assertThatThrownBy(() -> lotePresentationMapper.toSubirRequest(USUARIO_ID, fotos, null))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessage("No se pudo leer la foto recibida");
    }

    @Test
    @DisplayName("Arma el pedido de consulta con el usuario y el lote")
    void toObtenerRequestShouldMapIds() {
        ObtenerLoteRequest result = lotePresentationMapper.toObtenerRequest(USUARIO_ID, LOTE_ID);

        assertThat(result).isEqualTo(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    @Test
    @DisplayName("Arma el pedido de una foto con el usuario, el lote y la foto")
    void toFotoRequestShouldMapIds() {
        ObtenerFotoLoteRequest result = lotePresentationMapper.toFotoRequest(USUARIO_ID, LOTE_ID, FOTO_ID);

        assertThat(result).isEqualTo(new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, FOTO_ID));
    }

    @Test
    @DisplayName("Convierte el lote y sus fotos al JSON de respuesta")
    void toResponseShouldMapLoteAndFotos() {
        LoteResponse result = lotePresentationMapper.toResponse(lote());

        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.estado()).isEqualTo("PENDIENTE");
        assertThat(result.creadoEn()).isEqualTo(AHORA);
        thenFotoIsMapped(result.fotos().getFirst());
    }

    // --- arrange ---
    private MultipartFile givenUnreadableFile() throws IOException {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.getBytes()).thenThrow(new IOException("conexión cortada"));
        return archivo;
    }

    // --- assert ---
    private void thenFotoIsMapped(FotoResponse foto) {
        assertThat(foto.id()).isEqualTo(FOTO_ID);
        assertThat(foto.posicion()).isEqualTo((short) 1);
        assertThat(foto.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.tamanioBytes()).isEqualTo(7L);
    }

    // --- helpers ---
    private MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina", "image/jpeg", contenido);
    }

    private LoteItem lote() {
        return new LoteItem(LOTE_ID, "PENDIENTE", AHORA, List.of(
                new FotoItem(FOTO_ID, (short) 1, "image/jpeg", 7L)));
    }
}
