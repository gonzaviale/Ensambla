package com.ensambla.backend.infrastructure.presentation.dto.reposicion;

import java.util.List;

/** {@code proveedor} es {@code null} en el grupo de los productos que no tienen proveedor cargado. */
public record GrupoReposicionResponse(String proveedor, List<ProductoFaltanteResponse> productos) {
}
