package com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel;

import com.ensambla.backend.domain.model.cartel.DatosDePromocion;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.List;

public record RedactarTextosDeCartelRequest(
        UsuarioId usuarioId,
        List<TipoDeCartel> tiposPosibles,
        List<String> productos,
        List<BigDecimal> numeros) {

    public DatosDePromocion datos() {
        return new DatosDePromocion(tiposPosibles, productos, numeros);
    }
}
