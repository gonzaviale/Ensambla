package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.model.lote.LoteSubida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LotesDePrueba.unLoteConUnaFoto;
import static com.ensambla.backend.support.LotesDePrueba.unLoteSinFotos;
import static org.assertj.core.api.Assertions.assertThat;

class LoteItemTest {

    private LoteSubida lote;
    private LoteItem item;

    @Test
    @DisplayName("El item copia el lote y cada foto con todos sus datos")
    void elItemCopiaElLoteYCadaFotoConTodosSusDatos() {
        dadoUnLoteConUnaFoto();

        cuandoSeArmaElItemDelLote();

        entoncesElItemCopiaLosDatosDelLote();
        entoncesElItemCopiaLosDatosDeLaFoto();
    }

    @Test
    @DisplayName("Un lote sin fotos se copia con una lista de fotos vacía")
    void unLoteSinFotosSeCopiaConUnaListaDeFotosVacia() {
        dadoUnLoteSinFotos();

        cuandoSeArmaElItemDelLote();

        entoncesElItemNoTieneFotos();
    }

    // --- dado ---
    private void dadoUnLoteConUnaFoto() {
        lote = unLoteConUnaFoto();
    }

    private void dadoUnLoteSinFotos() {
        lote = unLoteSinFotos();
    }

    // --- cuando ---
    private void cuandoSeArmaElItemDelLote() {
        item = LoteItem.de(lote);
    }

    // --- entonces ---
    private void entoncesElItemCopiaLosDatosDelLote() {
        assertThat(item.id()).isEqualTo(LOTE_ID);
        assertThat(item.estado()).isEqualTo("PENDIENTE");
        assertThat(item.creadoEn()).isEqualTo(AHORA);
        assertThat(item.fotos()).hasSize(1);
    }

    private void entoncesElItemCopiaLosDatosDeLaFoto() {
        LoteItem.FotoItem foto = item.fotos().getFirst();
        assertThat(foto.id()).isEqualTo(FOTO_ID);
        assertThat(foto.posicion()).isZero();
        assertThat(foto.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.tamanioBytes()).isEqualTo(JPEG.length);
    }

    private void entoncesElItemNoTieneFotos() {
        assertThat(item.fotos()).isEmpty();
    }
}
