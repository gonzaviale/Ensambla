package com.ensambla.backend.infrastructure.presentation.mapper.zona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.VistaZona;
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

    /** La pantalla de Mi zona que pide los datos: «precios» o «qué se vende cerca». */
    public ObtenerZonaRequest toObtenerRequest(UsuarioId usuarioId, String vista) {
        return new ObtenerZonaRequest(usuarioId, vistaDe(vista));
    }

    private static VistaZona vistaDe(String vista) {
        return switch (vista) {
            case "precios" -> VistaZona.PRECIOS;
            case "cerca" -> VistaZona.CERCA;
            default -> throw new BusinessRuleException("La vista tiene que ser «precios» o «cerca»");
        };
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
