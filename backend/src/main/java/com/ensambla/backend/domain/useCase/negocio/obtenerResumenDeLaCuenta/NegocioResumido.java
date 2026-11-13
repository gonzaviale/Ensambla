package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.EstadoDelNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;

/** {@code margen} es lo que queda de lo vendido después de pagar la mercadería y los gastos. */
public record NegocioResumido(
        NegocioId id,
        String nombre,
        RubroNegocio rubro,
        long ventas,
        long margen,
        long avisosPendientes,
        EstadoDelNegocio estado) {

    public static NegocioResumido de(Negocio negocio, ComoVieneElNegocio comoViene, long avisosPendientes) {
        return new NegocioResumido(
                negocio.getId(),
                negocio.getNombre(),
                negocio.getRubro(),
                comoViene.vendiste(),
                comoViene.queda(),
                avisosPendientes,
                EstadoDelNegocio.de(comoViene.hayVentas(), comoViene.queda()));
    }
}
