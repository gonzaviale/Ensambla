package com.ensambla.backend.domain.useCase.reposicion.consultarReposicion;

import com.ensambla.backend.domain.model.reposicion.GrupoDeProveedor;

import java.util.List;

/** Lo que hay que pedirle a un proveedor; {@code proveedor} es {@code null} para los productos sin proveedor. */
public record GrupoDeProveedorItem(String proveedor, List<ProductoFaltanteItem> productos) {

    public static GrupoDeProveedorItem de(GrupoDeProveedor grupo) {
        return new GrupoDeProveedorItem(
                grupo.proveedor(),
                grupo.items().stream().map(ProductoFaltanteItem::de).toList());
    }
}
