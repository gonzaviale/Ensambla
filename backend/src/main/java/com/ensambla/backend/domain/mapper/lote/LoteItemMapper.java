package com.ensambla.backend.domain.mapper.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.model.archivo.UrlFirmada;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;

public class LoteItemMapper {

    public LoteItem toResponse(LoteSubida lote) {
        return new LoteItem(
                lote.getId(),
                lote.getEstado().name(),
                lote.getCreadoEn(),
                lote.getFotos().stream().map(this::toResponse).toList());
    }

    private FotoItem toResponse(FotoCuaderno foto) {
        UrlFirmada url = foto.getUrlLectura();
        return new FotoItem(
                foto.getId(),
                foto.getPosicion(),
                foto.getTipoContenido(),
                foto.getTamanioBytes(),
                url == null ? null : url.url(),
                url == null ? null : url.expiraEn());
    }
}
