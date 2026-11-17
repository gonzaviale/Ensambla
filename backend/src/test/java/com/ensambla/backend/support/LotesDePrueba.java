package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.PNG;

/** Lotes, fotos y archivos de cuaderno listos para los tests (los ids y constantes viven en {@link LoteTestData}). */
public final class LotesDePrueba {

    private LotesDePrueba() {
    }

    public static FotoCuaderno unaFotoDelLote() {
        return LoteTestData.foto();
    }

    public static LoteSubida unLoteSinFotos() {
        return LoteTestData.lote(List.of());
    }

    public static LoteSubida unLoteConUnaFoto() {
        return LoteTestData.lote(List.of(LoteTestData.foto()));
    }

    public static LoteSubida unLoteEn(EstadoLote estado) {
        LoteSubida lote = unLoteSinFotos();
        lote.setEstado(estado);
        return lote;
    }

    public static byte[] unaFotoJpeg() {
        return JPEG;
    }

    public static byte[] unaFotoPng() {
        return PNG;
    }

    public static byte[] unArchivoVacio() {
        return new byte[0];
    }

    public static byte[] unArchivoDeTexto() {
        return "no soy una foto".getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] unArchivoDeMasDeDiezMegas(long maximoPermitido) {
        return new byte[(int) maximoPermitido + 1];
    }

    public static List<byte[]> fotosJpeg(int cantidad) {
        return Collections.nCopies(cantidad, JPEG);
    }

    public static byte[] bytesDeTexto(String texto) {
        return texto.getBytes(StandardCharsets.ISO_8859_1);
    }
}
