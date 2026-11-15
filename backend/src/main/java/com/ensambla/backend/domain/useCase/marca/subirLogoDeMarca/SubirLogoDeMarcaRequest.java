package com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca;

import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record SubirLogoDeMarcaRequest(UsuarioId usuarioId, byte[] contenido, String tipoMime) {

    public LogoDeMarca logo() {
        return new LogoDeMarca(contenido, tipoMime);
    }
}
