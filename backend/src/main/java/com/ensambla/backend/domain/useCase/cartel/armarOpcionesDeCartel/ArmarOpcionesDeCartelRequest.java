package com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel;

import com.ensambla.backend.domain.model.cartel.ContenidoDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

public record ArmarOpcionesDeCartelRequest(
        UsuarioId usuarioId,
        TipoDeCartel tipo,
        FormatoDeCartel formato,
        String titular,
        String bajada,
        String protagonista,
        String detalle,
        List<String> lineas) {

    public ContenidoDeCartel contenidoDe(String negocio) {
        return new ContenidoDeCartel(negocio, titular, bajada, protagonista, detalle, lineas);
    }
}
