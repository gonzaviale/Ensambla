package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoMetricasTest {

    private static final DefinicionMetrica PARA_TODOS = lasVentasDelMesLlamadas("Para todos");

    private static final DefinicionMetrica DEL_KIOSCO = lasVentasDelMesLlamadas("Del kiosco");

    private CatalogoMetricas catalogo;

    @Test
    @DisplayName("Para un rubro ofrece primero las de todos y después las propias")
    void paraUnRubroOfreceLasDeTodosYDespuesLasPropias() {
        dadoUnCatalogoConMetricasParaElKiosco();

        entoncesAlRubroLeOfrece(RubroNegocio.KIOSCO, PARA_TODOS, DEL_KIOSCO);
    }

    @Test
    @DisplayName("Un rubro sin métricas propias sólo recibe las de todos")
    void unRubroSinMetricasPropiasSoloRecibeLasDeTodos() {
        dadoUnCatalogoConMetricasParaElKiosco();

        entoncesAlRubroLeOfrece(RubroNegocio.OTRO, PARA_TODOS);
    }

    @Test
    @DisplayName("Un catálogo vacío no ofrece nada")
    void unCatalogoVacioNoOfreceNada() {
        dadoUnCatalogoVacio();

        entoncesAlRubroLeOfrece(RubroNegocio.ALMACEN);
    }

    private void dadoUnCatalogoConMetricasParaElKiosco() {
        catalogo = new CatalogoMetricas(List.of(PARA_TODOS), Map.of(RubroNegocio.KIOSCO, List.of(DEL_KIOSCO)));
    }

    private void dadoUnCatalogoVacio() {
        catalogo = new CatalogoMetricas(null, null);
    }

    private void entoncesAlRubroLeOfrece(RubroNegocio rubro, DefinicionMetrica... sugeridas) {
        assertThat(catalogo.paraRubro(rubro)).containsExactly(sugeridas);
    }
}
