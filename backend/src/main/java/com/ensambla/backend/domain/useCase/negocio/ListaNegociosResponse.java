package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.List;

public record ListaNegociosResponse(
        NegocioId activoId, int limite, boolean puedeCrear, List<NegocioDeLaCuentaItem> negocios) {

    public static ListaNegociosResponse de(List<Negocio> negocios, NegocioId activoId, int limite) {
        return new ListaNegociosResponse(activoId, limite, negocios.size() < limite,
                negocios.stream().map(negocio -> NegocioDeLaCuentaItem.de(negocio, activoId)).toList());
    }
}
