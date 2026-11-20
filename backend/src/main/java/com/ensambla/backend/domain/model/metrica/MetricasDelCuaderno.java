package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Las métricas que ya guardó un cuaderno, en el orden del panel: deciden si entra una más, en qué lugar va y cómo
 * queda el panel cuando una se cambia de lugar.
 */
public record MetricasDelCuaderno(List<Metrica> metricas) {

    public MetricasDelCuaderno {
        metricas = List.copyOf(Objects.requireNonNullElse(metricas, List.of()));
    }

    static final String YA_LA_TENES = "Esa ya la tenés en el panel: probá con otra";

    /** Rechaza la receta que ya está a la vista (guardada o en el panel de ventas) y la que pasa el máximo. */
    public void exigirLugarPara(DefinicionMetrica definicion) {
        if (estaCompleto()) {
            throw new BusinessRuleException(
                    "Ya tenés " + Metrica.MAXIMO_POR_CUADERNO + " métricas: quitá alguna para agregar otra");
        }
        if (incluye(definicion)) {
            throw new BusinessRuleException(YA_LA_TENES);
        }
    }

    /** El lugar que sigue a la última del panel. */
    public int siguienteOrden() {
        return metricas.stream().mapToInt(Metrica::getOrden).max().orElse(-1) + 1;
    }

    /** {@code true} si ya está a la vista: alguna guardada pide lo mismo (aunque se llame distinto) o es del panel. */
    public boolean incluye(DefinicionMetrica definicion) {
        return definicion.laMuestraElPanelDeVentas() || algunaGuardadaPide(definicion);
    }

    private boolean algunaGuardadaPide(DefinicionMetrica definicion) {
        return metricas.stream().anyMatch(guardada -> guardada.getDefinicion().esIgualA(definicion));
    }

    /**
     * Pone la métrica en el lugar pedido (el primero es el 0) y corre las demás, sin dejar huecos. Devuelve el panel
     * entero, ya numerado, para guardarlo.
     */
    public List<Metrica> conLaMetricaEnElLugar(MetricaId metricaId, int lugar) {
        exigirQueElLugarExista(lugar);
        List<Metrica> enOrden = new ArrayList<>(metricas);
        Metrica movida = laMetrica(metricaId);
        enOrden.remove(movida);
        enOrden.add(lugar, movida);
        numerarDesdeCero(enOrden);
        return List.copyOf(enOrden);
    }

    private void exigirQueElLugarExista(int lugar) {
        if (lugar < 0 || lugar >= metricas.size()) {
            throw new BusinessRuleException("Ese lugar no está en el panel: elegí uno entre el primero y el último");
        }
    }

    private Metrica laMetrica(MetricaId metricaId) {
        return metricas.stream()
                .filter(guardada -> metricaId.equals(guardada.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Métrica no encontrada"));
    }

    private static void numerarDesdeCero(List<Metrica> enOrden) {
        IntStream.range(0, enOrden.size()).forEach(lugar -> enOrden.get(lugar).setOrden(lugar));
    }

    private boolean estaCompleto() {
        return metricas.size() >= Metrica.MAXIMO_POR_CUADERNO;
    }
}
