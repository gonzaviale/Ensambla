package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.model.lote.ArchivoFoto;

public record FotoArchivoResponse(byte[] contenido, String tipoContenido) {

    public static FotoArchivoResponse de(ArchivoFoto foto) {
        return new FotoArchivoResponse(foto.contenido(), foto.tipoContenido());
    }
}
