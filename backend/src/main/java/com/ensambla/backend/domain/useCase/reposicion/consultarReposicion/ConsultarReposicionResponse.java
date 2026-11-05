package com.ensambla.backend.domain.useCase.reposicion.consultarReposicion;

import com.ensambla.backend.domain.model.reposicion.Faltantes;

import java.util.List;

/** Los productos que hay que reponer, agrupados por proveedor. */
public record ConsultarReposicionResponse(List<GrupoDeProveedorItem> grupos) {

    public static ConsultarReposicionResponse de(Faltantes faltantes) {
        return new ConsultarReposicionResponse(
                faltantes.porProveedor().stream().map(GrupoDeProveedorItem::de).toList());
    }
}
