package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.EstadoLote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteSubidaTest {

    @Test
    @DisplayName("Un lote nuevo queda pendiente, sin intentos y con las fotos en el orden recibido")
    void nuevoShouldCreatePendingLoteWithOrderedFotos() {
        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, List.of(JPEG, PNG), AHORA);

        thenLoteIsPending(lote);
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(lote.getFotos()).extracting(FotoCuaderno::getTipoContenido)
                .containsExactly("image/jpeg", "image/png");
    }

    @Test
    @DisplayName("Rechaza un lote sin fotos")
    void nuevoShouldRejectEmptyLote() {
        List<byte[]> contenidos = List.of();

        assertThatThrownBy(() -> LoteSubida.nuevo(NEGOCIO_ID, contenidos, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Subí al menos una foto");
    }

    @Test
    @DisplayName("Rechaza un lote con más fotos de las que la IA lee de una vez")
    void nuevoShouldRejectTooManyFotos() {
        List<byte[]> contenidos = Collections.nCopies(LoteSubida.MAX_FOTOS + 1, JPEG);

        assertThatThrownBy(() -> LoteSubida.nuevo(NEGOCIO_ID, contenidos, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Podés subir hasta 10 fotos por lote");
    }

    @Test
    @DisplayName("Acepta exactamente el máximo de fotos por lote")
    void nuevoShouldAcceptMaxFotos() {
        List<byte[]> contenidos = Collections.nCopies(LoteSubida.MAX_FOTOS, JPEG);

        LoteSubida lote = LoteSubida.nuevo(NEGOCIO_ID, contenidos, AHORA);

        assertThat(lote.getFotos()).hasSize(LoteSubida.MAX_FOTOS);
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
