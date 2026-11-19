package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;

import java.time.Instant;
import java.util.Optional;

/** Sabe buscar un código de barras en una fuente concreta del catálogo. */
@FunctionalInterface
public interface BuscadorDeCatalogo {

    Optional<ProductoCatalogo> buscar(CodigoBarras codigo, Instant instante);
}
