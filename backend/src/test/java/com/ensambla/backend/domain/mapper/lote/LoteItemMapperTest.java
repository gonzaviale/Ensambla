package com.ensambla.backend.domain.mapper.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.LoteItem.FotoItem;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.URL_EXPIRA_EN;
import static com.ensambla.backend.support.LoteTestData.URL_LECTURA;
import static org.assertj.core.api.Assertions.assertThat;

class LoteItemMapperTest {

    private LoteItemMapper loteItemMapper;

    @BeforeEach
    void setUp() {
        loteItemMapper = new LoteItemMapper();
    }

    @Test
    @DisplayName("Mapea el lote y cada foto con su URL firmada de lectura")
    void toResponseShouldMapLoteAndFotoWithUrl() {
        LoteItem result = loteItemMapper.toResponse(LoteTestData.lote(List.of(LoteTestData.fotoConUrl())));

        thenLoteIsMapped(result);
        thenFotoIsMapped(result.fotos().getFirst());
        assertThat(result.fotos().getFirst().url()).isEqualTo(URL_LECTURA);
        assertThat(result.fotos().getFirst().urlExpiraEn()).isEqualTo(URL_EXPIRA_EN);
    }

    @Test
    @DisplayName("Deja la URL vacía cuando la foto no se firmó, como al subir el lote")
    void toResponseShouldLeaveUrlNullWhenNotSigned() {
        LoteItem result = loteItemMapper.toResponse(LoteTestData.lote(List.of(LoteTestData.foto())));

        thenFotoIsMapped(result.fotos().getFirst());
        assertThat(result.fotos().getFirst().url()).isNull();
        assertThat(result.fotos().getFirst().urlExpiraEn()).isNull();
    }

    @Test
    @DisplayName("Mapea un lote sin fotos a una lista vacía")
    void toResponseShouldMapEmptyFotos() {
        LoteItem result = loteItemMapper.toResponse(LoteTestData.lote(List.of()));

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
