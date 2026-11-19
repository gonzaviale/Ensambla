package com.ensambla.backend.domain.model.metrica;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Las frases con las que la gente pide cada parte de una métrica («vendí», «por producto», «esta semana», «me
 * deben»). Son datos, de {@code metricas/diccionario-pedidos.json}: entender una forma nueva de pedir es sumar una
 * frase al archivo. Lo que no se menciona toma el valor por defecto (sumar, sin repartir, el mes).
 *
 * @param noSeCalculan   lo que se reconoce pero el motor no sabe calcular («el finde», «ayer»): se avisa que no se
 *                       entendió en lugar de inventar un período parecido
 * @param palabrasVacias las que no cambian el pedido («de», «el», «cuánto»): no se avisan como no entendidas
 */
public record DiccionarioDePedidos(
        Map<FuenteMetrica, List<String>> fuentes,
        Map<CalculoMetrica, List<String>> calculos,
        Map<AgrupacionMetrica, List<String>> agrupaciones,
        Map<PeriodoMetrica, List<String>> periodos,
        List<String> fiado,
        List<String> noSeCalculan,
        List<String> palabrasVacias) {

    public DiccionarioDePedidos {
        fuentes = Map.copyOf(Objects.requireNonNullElse(fuentes, Map.of()));
        calculos = Map.copyOf(Objects.requireNonNullElse(calculos, Map.of()));
        agrupaciones = Map.copyOf(Objects.requireNonNullElse(agrupaciones, Map.of()));
        periodos = Map.copyOf(Objects.requireNonNullElse(periodos, Map.of()));
        fiado = List.copyOf(Objects.requireNonNullElse(fiado, List.of()));
        noSeCalculan = List.copyOf(Objects.requireNonNullElse(noSeCalculan, List.of()));
        palabrasVacias = List.copyOf(Objects.requireNonNullElse(palabrasVacias, List.of()));
    }

    /** Todas las frases que el pedido puede decir sin que haga falta avisar que no se entendieron. */
    public List<String> frasesConocidas() {
        return Stream.of(fuentes.values(), calculos.values(), agrupaciones.values(), periodos.values(),
                        List.of(fiado, palabrasVacias))
                .flatMap(Collection::stream)
                .flatMap(List::stream)
                .toList();
    }
}
