package com.ensambla.backend.domain.useCase.cartel.exportarCartel;

import com.ensambla.backend.domain.model.cartel.ContenidoDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

public record ExportarCartelRequest(
        UsuarioId usuarioId,
        VarianteDeComposicion variante,
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
