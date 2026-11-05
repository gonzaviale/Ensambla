package com.ensambla.backend.domain.useCase.reposicion.consultarReposicion;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reposicion.ItemReposicion;

import java.math.BigDecimal;

/** Un producto que hay que reponer, con la cantidad que se sugiere pedir. */
public record ProductoFaltanteItem(
        RegistroId id,
        String nombre,
        BigDecimal cantidadActual,
        BigDecimal cantidadAPedir,
        String situacion) {

    public static ProductoFaltanteItem de(ItemReposicion item) {
        return new ProductoFaltanteItem(
                item.productoId(),
                item.nombre(),
                item.cantidadActual(),
                item.cantidadAPedir(),
                item.situacion());
    }
}
