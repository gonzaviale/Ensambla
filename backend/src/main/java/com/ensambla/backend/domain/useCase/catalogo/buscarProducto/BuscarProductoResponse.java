package com.ensambla.backend.domain.useCase.catalogo.buscarProducto;

import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;

import java.util.Optional;

public record BuscarProductoResponse(ProductoItem producto) {

    public record ProductoItem(String codigo, String nombre, String marca, String presentacion, String imagen) {
    }

    public static BuscarProductoResponse de(Optional<ProductoCatalogo> producto) {
        return new BuscarProductoResponse(producto.map(p -> new ProductoItem(
                p.codigo(), p.nombre(), p.marca(), p.presentacion(), p.imagen())).orElse(null));
    }
}
