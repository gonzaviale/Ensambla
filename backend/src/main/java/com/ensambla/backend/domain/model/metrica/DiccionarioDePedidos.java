package com.ensambla.backend.domain.model.metrica;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las frases con las que la gente pide cada parte de una métrica («vendí», «por producto», «esta semana», «me
 * deben»). Son datos, de {@code metricas/diccionario-pedidos.json}: entender una forma nueva de pedir es sumar una
 * frase al archivo. Lo que no se menciona toma el valor por defecto (sumar, sin repartir, el mes).
 */
public record DiccionarioDePedidos(
        Map<FuenteMetrica, List<String>> fuentes,
        Map<CalculoMetrica, List<String>> calculos,
        Map<AgrupacionMetrica, List<String>> agrupaciones,
        Map<PeriodoMetrica, List<String>> periodos,
        List<String> fiado) {

    public DiccionarioDePedidos {
        fuentes = Map.copyOf(Objects.requireNonNullElse(fuentes, Map.of()));
        calculos = Map.copyOf(Objects.requireNonNullElse(calculos, Map.of()));
        agrupaciones = Map.copyOf(Objects.requireNonNullElse(agrupaciones, Map.of()));
        periodos = Map.copyOf(Objects.requireNonNullElse(periodos, Map.of()));
        fiado = List.copyOf(Objects.requireNonNullElse(fiado, List.of()));
    }
}
