package com.ensambla.backend.domain.mapper.zona;

import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.dto.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

public class ZonaItemMapper {

    public ZonaItem toResponse(AnalisisZona analisis) {
        return new ZonaItem(
                analisis.nombre(),
                analisis.comercios(),
                analisis.productos().stream().map(this::toResponse).toList());
    }

    private ProductoItem toResponse(AgregadoZonaProducto producto) {
        return new ProductoItem(
                producto.nombre(),
                producto.comercios(),
                producto.precioMinimo(),
                producto.precioPromedio(),
                producto.precioMaximo(),
                producto.comerciosVenden(),
                producto.unidadesSemana());
    }
}
