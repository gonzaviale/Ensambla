package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.OffsetDateTime;

public record NegocioDeLaCuentaItem(
        NegocioId id, String nombre, String rubro, boolean activo, OffsetDateTime creadoEn) {

    public static NegocioDeLaCuentaItem de(Negocio negocio, NegocioId activoId) {
        return new NegocioDeLaCuentaItem(negocio.getId(), negocio.getNombre(), negocio.getRubro().name(),
                negocio.getId().equals(activoId), negocio.getCreadoEn());
    }
}
