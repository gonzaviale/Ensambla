package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.useCase.lote.LoteItem.FotoItem;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static org.assertj.core.api.Assertions.assertThat;

class LoteItemTest {

    @Test
    @DisplayName("Mapea el lote y cada foto con todos sus datos")
    void deShouldMapLoteAndFoto() {
        LoteItem result = LoteItem.de(LoteTestData.lote(List.of(LoteTestData.foto())));

        thenLoteIsMapped(result);
        thenFotoIsMapped(result.fotos().getFirst());
    }

    @Test
    @DisplayName("Mapea un lote sin fotos a una lista vacía")
    void deShouldMapEmptyFotos() {
        LoteItem result = LoteItem.de(LoteTestData.lote(List.of()));

        assertThat(result.fotos()).isEmpty();
    }

    // --- assert ---
    private void thenLoteIsMapped(LoteItem result) {
        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.estado()).isEqualTo("PENDIENTE");
        assertThat(result.creadoEn()).isEqualTo(AHORA);
        assertThat(result.fotos()).hasSize(1);
    }

    private void thenFotoIsMapped(FotoItem foto) {
        assertThat(foto.id()).isEqualTo(FOTO_ID);
        assertThat(foto.posicion()).isZero();
        assertThat(foto.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.tamanioBytes()).isEqualTo(JPEG.length);
    }
}
