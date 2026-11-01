package com.ensambla.backend.infrastructure.presentation.mapper.zona;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZonaRequest;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZonaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.AnalizarZonaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.PrecioWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.ProductoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse.VentasWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse;
import org.springframework.stereotype.Component;

@Component
public class ZonaPresentationMapper {

    public ObtenerZonaRequest toObtenerRequest(UsuarioId usuarioId) {
        return new ObtenerZonaRequest(usuarioId);
    }

    public AnalizarZonaRequest toAnalizarRequest(AnalizarZonaWebRequest request, UsuarioId usuarioId) {
        return new AnalizarZonaRequest(request.lugar(), request.latitud(), request.longitud(), usuarioId);
    }

    public ZonaWebResponse toResponse(ZonaItem zona) {
        return new ZonaWebResponse(
                zona.nombre(),
                zona.comercios(),
                zona.productos().stream().map(this::toResponse).toList(), zona.acceso());
    }

    private ProductoWebResponse toResponse(ProductoItem producto) {
        return new ProductoWebResponse(
                producto.nombre(),
                producto.comercios(),
                new PrecioWebResponse(producto.precioMinimo(), producto.precioPromedio(), producto.precioMaximo()),
                new VentasWebResponse(producto.comerciosVenden(), producto.unidadesPorSemana()));
    }
}
