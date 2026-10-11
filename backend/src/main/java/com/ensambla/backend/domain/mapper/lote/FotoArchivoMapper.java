package com.ensambla.backend.domain.mapper.lote;

import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;

public class FotoArchivoMapper {

    public FotoArchivoResponse toResponse(ArchivoFoto foto) {
        return new FotoArchivoResponse(foto.contenido(), foto.tipoContenido());
    }
}
