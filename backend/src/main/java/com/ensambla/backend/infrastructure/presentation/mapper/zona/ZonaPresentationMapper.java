package com.ensambla.backend.infrastructure.presentation.mapper.zona;

import com.ensambla.backend.domain.dto.zona.AnalizarZonaRequest;
import com.ensambla.backend.domain.dto.zona.ObtenerZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.dto.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.infrastructure.presentation.dto.zona.AnalizarZonaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.PrecioWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.ProductoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.VentasWebResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ZonaPresentationMapper {

    public ObtenerZonaRequest toObtenerRequest(UUID usuarioId) {
        return new ObtenerZonaRequest(usuarioId);
    }

    public AnalizarZonaRequest toAnalizarRequest(AnalizarZonaWebRequest request) {
        return new AnalizarZonaRequest(request.lugar());
    }

    public ZonaWebResponse toResponse(ZonaItem zona) {
        return new ZonaWebResponse(
                zona.nombre(),
                zona.comercios(),
                zona.productos().stream().map(this::toResponse).toList());
    }

    private ProductoWebResponse toResponse(ProductoItem producto) {
        return new ProductoWebResponse(
                producto.nombre(),
                producto.comercios(),
                new PrecioWebResponse(producto.precioMinimo(), producto.precioPromedio(), producto.precioMaximo()),
                new VentasWebResponse(producto.comerciosVenden(), producto.unidadesPorSemana()));
    }
}
