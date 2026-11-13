package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import com.ensambla.backend.domain.port.CatalogoProductosPort;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
public class CatalogoProductosAdapter implements CatalogoProductosPort {

    private final IndiceSepa indiceSepa;

    private final OpenFoodFactsCatalogo openFoodFactsCatalogo;

    @Override
    public Optional<ProductoCatalogo> buscar(FuenteCatalogo fuente, CodigoBarras codigo, Instant instante) {
        return switch (fuente) {
            case SEPA -> indiceSepa.buscar(codigo);
            case OPEN_FOOD_FACTS -> openFoodFactsCatalogo.buscar(codigo, instante);
        };
    }
}
