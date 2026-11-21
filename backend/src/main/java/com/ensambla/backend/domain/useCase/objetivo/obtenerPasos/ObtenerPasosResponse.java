package com.ensambla.backend.domain.useCase.objetivo.obtenerPasos;

import com.ensambla.backend.domain.model.objetivo.PasoObjetivo;
import com.ensambla.backend.domain.model.objetivo.PasoObjetivo.PrecioZona;
import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;

import java.math.BigDecimal;
import java.util.List;

public record ObtenerPasosResponse(List<PasoItem> content) {

    public static ObtenerPasosResponse de(List<PasoObjetivo> pasos) {
        return new ObtenerPasosResponse(pasos.stream().map(PasoItem::de).toList());
    }

    public record PasoItem(
            TipoPasoObjetivo tipo,
            String nombre,
            BigDecimal precio,
            BigDecimal costo,
            BigDecimal stock,
            BigDecimal vendidas30Dias,
            PrecioZonaItem precioZona) {

        private static PasoItem de(PasoObjetivo paso) {
            return new PasoItem(
                    paso.tipo(),
                    paso.nombre(),
                    paso.precio(),
                    paso.costo(),
                    paso.stock(),
                    paso.vendidas30Dias(),
                    PrecioZonaItem.de(paso.precioZona()));
        }
    }

    public record PrecioZonaItem(BigDecimal minimo, BigDecimal promedio, BigDecimal maximo) {

        private static PrecioZonaItem de(PrecioZona precioZona) {
            return precioZona == null
                    ? null
                    : new PrecioZonaItem(precioZona.minimo(), precioZona.promedio(), precioZona.maximo());
        }
    }
}
