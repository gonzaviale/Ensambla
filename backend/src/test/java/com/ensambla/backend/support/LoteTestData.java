package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;
import java.util.List;

public final class LoteTestData {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000011");
    public static final NegocioId NEGOCIO_ID = NegocioId.de("00000000-0000-4000-8000-000000000012");
    public static final LoteId LOTE_ID = LoteId.de("00000000-0000-4000-8000-000000000013");
    public static final FotoId FOTO_ID = FotoId.de("00000000-0000-4000-8000-000000000014");
    public static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000015");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    public static final String CLAVE = NEGOCIO_ID + "/fotos/pagina.jpg";
    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    private LoteTestData() {
    }

    public static FotoCuaderno foto() {
        return FotoCuaderno.builder()
                .id(FOTO_ID)
                .negocioId(NEGOCIO_ID)
                .loteId(LOTE_ID)
                .claveAlmacenamiento(CLAVE)
                .tipoContenido("image/jpeg")
                .tamanioBytes((long) JPEG.length)
                .rotacion((short) 0)
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    public static LoteSubida lote(List<FotoCuaderno> fotos) {
        return LoteSubida.builder()
                .id(LOTE_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueDestinoId(BLOQUE_ID)
                .estado(EstadoLote.PENDIENTE)
                .intentos((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .fotos(fotos)
                .build();
    }
}
