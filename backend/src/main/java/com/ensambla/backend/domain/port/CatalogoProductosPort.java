package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;

import java.time.Instant;
import java.util.Optional;

public interface CatalogoProductosPort {

    Optional<ProductoCatalogo> buscar(FuenteCatalogo fuente, CodigoBarras codigo, Instant instante);
}
