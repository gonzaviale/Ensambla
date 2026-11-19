package com.ensambla.backend.infrastructure.presentation.mapper.lote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.domain.useCase.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLoteRequest;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse.FotoResponse;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LotePresentationMapperTest {

    private final LotePresentationMapper mapper = new LotePresentationMapper();
    private SubirLoteRequest subida;
    private Throwable error;

    @Test
    @DisplayName("Arma el pedido de subida con el usuario y los bytes de cada archivo en orden")
    void armaElPedidoDeSubidaConLosBytesEnOrden() {
        cuandoSeSuben(List.of(unArchivo(JPEG), unArchivo(PNG)), null);

        entoncesLaSubidaEsDelUsuarioConLasFotos(JPEG, PNG);
        entoncesLaSubidaNoTieneDestino();
    }

    @Test
    @DisplayName("Arma el pedido de subida con el bloque destino elegido")
    void armaElPedidoDeSubidaConElBloqueDestino() {
        cuandoSeSuben(List.of(unArchivo(JPEG)), BLOQUE_ID);

        entoncesLaSubidaVaAlBloque();
    }

    @Test
    @DisplayName("Si no puede leer un archivo recibido falla con un error de lectura")
    void siNoPuedeLeerUnArchivoFalla() {
        cuandoSeIntentanSubir(List.of(unArchivoIlegible()));

        entoncesFallaLaLecturaDeLaFoto();
    }

    @Test
    @DisplayName("Arma el pedido de consulta con el usuario y el lote")
    void armaElPedidoDeConsulta() {
        entoncesElPedidoEs(mapper.toObtenerRequest(USUARIO_ID, LOTE_ID), new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    @Test
    @DisplayName("Arma el pedido de una foto con el usuario, el lote y la foto")
    void armaElPedidoDeUnaFoto() {
        entoncesElPedidoEs(mapper.toFotoRequest(USUARIO_ID, LOTE_ID, FOTO_ID),
                new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, FOTO_ID));
    }

    @Test
    @DisplayName("Convierte el lote y sus fotos al JSON de respuesta")
    void convierteElLoteYSusFotosALaRespuesta() {
        entoncesElLoteSeRespondeComo(new LoteResponse(LOTE_ID, "PENDIENTE", AHORA,
                List.of(new FotoResponse(FOTO_ID, (short) 1, "image/jpeg", 7L))));
    }

    private static MockMultipartFile unArchivo(byte[] contenido) {
        return new MockMultipartFile("fotos", "pagina", "image/jpeg", contenido);
    }

    private static MultipartFile unArchivoIlegible() {
        MultipartFile archivo = mock(MultipartFile.class);
        try {
            when(archivo.getBytes()).thenThrow(new IOException("conexión cortada"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return archivo;
    }

    private void cuandoSeSuben(List<MultipartFile> fotos, BloqueId destino) {
        subida = mapper.toSubirRequest(USUARIO_ID, fotos, destino);
    }

    private void cuandoSeIntentanSubir(List<MultipartFile> fotos) {
        error = catchThrowable(() -> cuandoSeSuben(fotos, null));
    }

    private void entoncesLaSubidaEsDelUsuarioConLasFotos(byte[]... fotos) {
        assertThat(subida.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(subida.fotos()).containsExactly(fotos);
    }

    private void entoncesLaSubidaNoTieneDestino() {
        assertThat(subida.bloqueDestinoId()).isNull();
    }

    private void entoncesLaSubidaVaAlBloque() {
        assertThat(subida.bloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    private void entoncesFallaLaLecturaDeLaFoto() {
        assertThat(error).isInstanceOf(UncheckedIOException.class).hasMessage("No se pudo leer la foto recibida");
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private void entoncesElLoteSeRespondeComo(LoteResponse esperada) {
        LoteItem lote = new LoteItem(LOTE_ID, "PENDIENTE", AHORA, List.of(new FotoItem(FOTO_ID, (short) 1, "image/jpeg", 7L)));
        assertThat(mapper.toResponse(lote)).usingRecursiveComparison().isEqualTo(esperada);
    }
}