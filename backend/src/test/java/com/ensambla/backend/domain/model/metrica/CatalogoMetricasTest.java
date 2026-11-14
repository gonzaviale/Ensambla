package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoMetricasTest {

    private static final DefinicionMetrica BASE = definicion("Base");
    private static final DefinicionMetrica DEL_KIOSCO = definicion("Del kiosco");

    @Test
    @DisplayName("Para un rubro ofrece primero las de todos y después las propias")
    void paraRubroShouldListBaseThenOwn() {
        CatalogoMetricas catalogo = catalogo();

        assertThat(catalogo.paraRubro(RubroNegocio.KIOSCO)).containsExactly(BASE, DEL_KIOSCO);
    }

    @Test
    @DisplayName("Un rubro sin métricas propias sólo recibe las de todos")
    void paraRubroShouldFallBackToBase() {
        CatalogoMetricas catalogo = catalogo();

        assertThat(catalogo.paraRubro(RubroNegocio.OTRO)).containsExactly(BASE);
    }

    @Test
    @DisplayName("Un catálogo vacío no ofrece nada")
    void constructorShouldDefaultToEmpty() {
        assertThat(new CatalogoMetricas(null, null).paraRubro(RubroNegocio.ALMACEN)).isEmpty();
    }

    private static CatalogoMetricas catalogo() {
        return new CatalogoMetricas(List.of(BASE), Map.of(RubroNegocio.KIOSCO, List.of(DEL_KIOSCO)));
    }

    private static DefinicionMetrica definicion(String nombre) {
        return new DefinicionMetrica(nombre, FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, false);
    }
}
