package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoteItem(
        UUID id,
        String estado,
        OffsetDateTime creadoEn,
        List<FotoItem> fotos
) {

    public static LoteItem de(LoteSubida lote) {
        return new LoteItem(
                lote.getId(),
                lote.getEstado().name(),
                lote.getCreadoEn(),
                lote.getFotos().stream().map(FotoItem::de).toList());
    }

    public record FotoItem(UUID id, Short posicion, String tipoContenido, Long tamanioBytes) {

        static FotoItem de(FotoCuaderno foto) {
            return new FotoItem(
                    foto.getId(),
                    foto.getPosicion(),
                    foto.getTipoContenido(),
                    foto.getTamanioBytes());
        }
    }
}
