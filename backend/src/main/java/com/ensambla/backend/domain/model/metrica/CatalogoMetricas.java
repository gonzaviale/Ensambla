package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las métricas sugeridas, de {@code metricas/catalogo-metricas.json}: unas para todos los negocios y otras según el
 * rubro. Son datos, no código: sumar una sugerencia es sumar una receta al archivo.
 */
public record CatalogoMetricas(List<DefinicionMetrica> base, Map<RubroNegocio, List<DefinicionMetrica>> porRubro) {

    public CatalogoMetricas {
        base = List.copyOf(Objects.requireNonNullElse(base, List.of()));
        porRubro = Map.copyOf(Objects.requireNonNullElse(porRubro, Map.of()));
    }

    /** Las de todos los negocios y, después, las propias del rubro. */
    public List<DefinicionMetrica> paraRubro(RubroNegocio rubro) {
        List<DefinicionMetrica> sugeridas = new ArrayList<>(base);
        sugeridas.addAll(porRubro.getOrDefault(rubro, List.of()));
        return List.copyOf(sugeridas);
    }
}
