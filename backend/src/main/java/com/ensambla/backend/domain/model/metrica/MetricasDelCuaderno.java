package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;
import java.util.Objects;

/** Las métricas que ya guardó un cuaderno: deciden si entra una más y en qué lugar del panel. */
public record MetricasDelCuaderno(List<Metrica> metricas) {

    public MetricasDelCuaderno {
        metricas = List.copyOf(Objects.requireNonNullElse(metricas, List.of()));
    }

    /** Rechaza la receta que ya está guardada y la que pasa el máximo por cuaderno. */
    public void exigirLugarPara(DefinicionMetrica definicion) {
        if (estaCompleto()) {
            throw new BusinessRuleException(
                    "Ya tenés " + Metrica.MAXIMO_POR_CUADERNO + " métricas: quitá alguna para agregar otra");
        }
        if (incluye(definicion)) {
            throw new BusinessRuleException("Ya tenés esa métrica en el cuaderno");
        }
    }

    /** El lugar que sigue a la última del panel. */
    public int siguienteOrden() {
        return metricas.stream().mapToInt(Metrica::getOrden).max().orElse(-1) + 1;
    }

    /** {@code true} si alguna guardada pide lo mismo, aunque tenga otro nombre. */
    public boolean incluye(DefinicionMetrica definicion) {
        return metricas.stream().anyMatch(guardada -> guardada.getDefinicion().esIgualA(definicion));
    }

    private boolean estaCompleto() {
        return metricas.size() >= Metrica.MAXIMO_POR_CUADERNO;
    }
}
