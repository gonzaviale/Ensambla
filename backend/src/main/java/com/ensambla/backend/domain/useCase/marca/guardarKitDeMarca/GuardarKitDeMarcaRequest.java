package com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca;

import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record GuardarKitDeMarcaRequest(
        UsuarioId usuarioId,
        String colorPrincipal,
        String colorSecundario,
        Tipografia tipografia,
        TonoDeMarca tono) {

    public ColorHex principal() {
        return ColorHex.de(colorPrincipal);
    }

    public ColorHex secundario() {
        return ColorHex.de(colorSecundario);
    }
}
