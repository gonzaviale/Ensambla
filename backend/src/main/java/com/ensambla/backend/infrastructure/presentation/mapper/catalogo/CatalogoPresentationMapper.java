package com.ensambla.backend.infrastructure.presentation.mapper.catalogo;

import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProductoRequest;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProductoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.catalogo.ProductoCatalogoWebResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogoPresentationMapper {

    public BuscarProductoRequest toRequest(String fuente, String codigo) {
        return new BuscarProductoRequest(fuente, codigo);
    }

    public ProductoCatalogoWebResponse toResponse(BuscarProductoResponse response) {
        var producto = response.producto();
        return new ProductoCatalogoWebResponse(producto == null ? null
                : new ProductoCatalogoWebResponse.ProductoWeb(producto.codigo(), producto.nombre(), producto.marca(),
                        producto.presentacion(), producto.imagen()));
    }
}
