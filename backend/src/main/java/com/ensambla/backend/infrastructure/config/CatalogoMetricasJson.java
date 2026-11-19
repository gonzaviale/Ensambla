package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** El catálogo tal como está escrito en {@code metricas/catalogo-metricas.json}, donde cada dato puede faltar. */
record CatalogoMetricasJson(List<DefinicionJson> base, Map<RubroNegocio, List<DefinicionJson>> porRubro) {

    CatalogoMetricasJson {
        base = Objects.requireNonNullElse(base, List.of());
        porRubro = Objects.requireNonNullElse(porRubro, Map.of());
    }

    CatalogoMetricas aCatalogo() {
        return new CatalogoMetricas(definiciones(base), definicionesPorRubro());
    }

    private Map<RubroNegocio, List<DefinicionMetrica>> definicionesPorRubro() {
        return porRubro.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, rubro -> definiciones(rubro.getValue())));
    }

    private static List<DefinicionMetrica> definiciones(List<DefinicionJson> recetas) {
        return recetas.stream().map(DefinicionJson::aDefinicion).toList();
    }

    /** Una receta del archivo; si no dice {@code soloFiado}, mira todas las filas. */
    record DefinicionJson(String nombre, FuenteMetrica fuente, CalculoMetrica calculo, AgrupacionMetrica agrupar,
                          PeriodoMetrica periodo, Boolean soloFiado) {

        DefinicionMetrica aDefinicion() {
            return new DefinicionMetrica(nombre, fuente, calculo, agrupar, periodo, Boolean.TRUE.equals(soloFiado));
        }
    }
}
