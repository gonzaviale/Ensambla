package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteSubidaTest {

    @Test
    @DisplayName("Un lote nuevo queda pendiente, sin intentos y con las fotos en el orden recibido")
    void nuevoShouldCreatePendingLoteWithOrderedFotos() {
        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, List.of(JPEG, PNG), null, AHORA);

        thenLoteIsPending(lote);
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getTipoContenido)
                .containsExactly("image/jpeg", "image/png");
    }

    @Test
    @DisplayName("Un lote nuevo recuerda el bloque destino que eligió la persona")
    void nuevoShouldKeepBloqueDestino() {
        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, List.of(JPEG), BLOQUE_ID, AHORA);

        assertThat(lote.getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    @Test
    @DisplayName("Un lote nuevo sin bloque destino deja que la IA proponga uno")
    void nuevoShouldLeaveBloqueDestinoEmptyWhenNoneChosen() {
        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, List.of(JPEG), null, AHORA);

        assertThat(lote.getBloqueDestinoId()).isNull();
    }

    @Test
    @DisplayName("Rechaza un lote sin fotos")
    void nuevoShouldRejectEmptyLote() {
        List<byte[]> contenidos = List.of();

        assertThatThrownBy(() -> LoteSubida.nuevo(NEGOCIO_ID, contenidos, null, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Subí al menos una foto");
    }

    @Test
    @DisplayName("Rechaza un lote con más fotos de las que la IA lee de una vez")
    void nuevoShouldRejectTooManyFotos() {
        List<byte[]> contenidos = Collections.nCopies(LoteSubida.MAX_FOTOS + 1, JPEG);

        assertThatThrownBy(() -> LoteSubida.nuevo(NEGOCIO_ID, contenidos, null, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Podés subir hasta 10 fotos por lote");
    }

    @Test
    @DisplayName("Acepta exactamente el máximo de fotos por lote")
    void nuevoShouldAcceptMaxFotos() {
        List<byte[]> contenidos = Collections.nCopies(LoteSubida.MAX_FOTOS, JPEG);

        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, contenidos, null, AHORA);

        assertThat(lote.getFotos()).hasSize(LoteSubida.MAX_FOTOS);
    }

    @Test
    @DisplayName("Un lote con la lectura terminada se puede confirmar")
    void validarConfirmableShouldAcceptLoteListo() {
        LoteSubida lote = givenLoteEn(EstadoLote.LISTO);

        assertThatCode(lote::validarConfirmable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un lote ya confirmado no se puede confirmar de nuevo")
    void validarConfirmableShouldRejectLoteConfirmado() {
        LoteSubida lote = givenLoteEn(EstadoLote.CONFIRMADO);

        assertThatThrownBy(lote::validarConfirmable)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este cuaderno ya se guardó");
    }

    @Test
    @DisplayName("Un lote que todavía se está leyendo no se puede confirmar")
    void validarConfirmableShouldRejectLoteSinTerminar() {
        LoteSubida lote = givenLoteEn(EstadoLote.PROCESANDO);

        assertThatThrownBy(lote::validarConfirmable)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este cuaderno todavía no terminó de leerse");
    }

    @Test
    @DisplayName("La página 1 es la foto de posición 0")
    void fotoDePaginaShouldReturnFotoByPosition() {
        LoteSubida lote = LoteTestData.lote(List.of(LoteTestData.foto()));

        Optional<FotoCuaderno> result = lote.fotoDePagina(1);

        assertThat(result).map(FotoCuaderno::getId).contains(LoteTestData.FOTO_ID);
    }

    @Test
    @DisplayName("Una página que el lote no tiene no devuelve foto")
    void fotoDePaginaShouldReturnEmptyWhenPageIsMissing() {
        LoteSubida lote = LoteTestData.lote(List.of(LoteTestData.foto()));

        assertThat(lote.fotoDePagina(2)).isEmpty();
    }

    // --- arrange ---
    private LoteSubida givenLoteEn(EstadoLote estado) {
        LoteSubida lote = LoteTestData.lote(List.of());
        lote.setEstado(estado);
        return lote;
    }

    // --- assert ---
    private void thenLoteIsPending(LoteSubida lote) {
        assertThat(lote.getId()).isNull();
        assertThat(lote.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(lote.getIntentos()).isZero();
        assertThat(lote.getCreadoEn()).isEqualTo(AHORA);
        assertThat(lote.getActualizadoEn()).isEqualTo(AHORA);
    }
}
