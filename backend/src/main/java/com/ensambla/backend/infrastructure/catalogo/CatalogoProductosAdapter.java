package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import com.ensambla.backend.domain.port.CatalogoProductosPort;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class CatalogoProductosAdapter implements CatalogoProductosPort {

    private final Map<FuenteCatalogo, BuscadorDeCatalogo> buscadores = new EnumMap<>(FuenteCatalogo.class);

    public CatalogoProductosAdapter(IndiceSepa indiceSepa, OpenFoodFactsCatalogo openFoodFactsCatalogo) {
        buscadores.put(FuenteCatalogo.SEPA, (codigo, instante) -> indiceSepa.buscar(codigo));
        buscadores.put(FuenteCatalogo.OPEN_FOOD_FACTS, openFoodFactsCatalogo::buscar);
    }

    @Override
    public Optional<ProductoCatalogo> buscar(FuenteCatalogo fuente, CodigoBarras codigo, Instant instante) {
        return buscadores.get(fuente).buscar(codigo, instante);
    }
}
