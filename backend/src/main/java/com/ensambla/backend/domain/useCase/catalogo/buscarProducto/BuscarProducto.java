package com.ensambla.backend.domain.useCase.catalogo.buscarProducto;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.port.CatalogoProductosPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@RequiredArgsConstructor
public class BuscarProducto {

    private final CatalogoProductosPort catalogoProductosPort;

    private final Clock clock;

    public BuscarProductoResponse execute(BuscarProductoRequest request) {
        CodigoBarras codigo = new CodigoBarras(request.codigo());
        FuenteCatalogo fuente = FuenteCatalogo.de(request.fuente());
        return BuscarProductoResponse.de(catalogoProductosPort.buscar(fuente, codigo, clock.instant()));
    }
}
