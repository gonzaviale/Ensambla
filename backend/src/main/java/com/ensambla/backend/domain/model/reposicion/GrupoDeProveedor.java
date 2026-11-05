package com.ensambla.backend.domain.model.reposicion;

import java.util.List;

/** Lo que hay que pedirle a un proveedor; {@code proveedor} es {@code null} para los productos que no tienen uno. */
public record GrupoDeProveedor(String proveedor, List<ItemReposicion> items) {
}
