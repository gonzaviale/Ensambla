package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Por qué se reparte el número: por producto, por cliente, por categoría de gasto o por día de la semana. */
public enum AgrupacionMetrica {

    NINGUNA(null) {
        @Override
        List<Fila> repartir(List<LineaMetrica> lineas, CalculoMetrica calculo) {
            return List.of();
        }

        @Override
        public boolean reparte() {
            return false;
        }

        @Override
        public GraficoMetrica graficoDeSiempre() {
            return GraficoMetrica.NUMERO;
        }
    },
    PRODUCTO(LineaMetrica::producto),
    CLIENTE(LineaMetrica::cliente),
    CATEGORIA(LineaMetrica::categoria),
    DIA_DE_LA_SEMANA(LineaMetrica::diaDeLaSemana) {
        @Override
        List<Fila> ordenar(List<Fila> filas) {
            return filas.stream()
                    .sorted(Comparator.comparingInt(fila -> LineaMetrica.lugarEnLaSemana(fila.etiqueta())))
                    .toList();
        }

        @Override
        public boolean vaEnOrden() {
            return true;
        }
    };

    private static final Comparator<Fila> DE_MAYOR_A_MENOR =
            Comparator.comparing(Fila::valor).reversed().thenComparing(Fila::etiqueta);

    private final Function<LineaMetrica, String> etiqueta;

    AgrupacionMetrica(Function<LineaMetrica, String> etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** {@code true} si el número se parte en filas (por producto, cliente…); sin repartir es un solo número. */
    public boolean reparte() {
        return true;
    }

    /** {@code true} si las partes tienen un orden propio, como los días de la semana: se pueden unir con una línea. */
    public boolean vaEnOrden() {
        return false;
    }

    /** El gráfico con que se muestra si la persona no eligió otro: el número solo o las barras del reparto. */
    public GraficoMetrica graficoDeSiempre() {
        return GraficoMetrica.BARRAS;
    }

    /** El número de cada parte del reparto, ya ordenado para mostrar. */
    List<Fila> repartir(List<LineaMetrica> lineas, CalculoMetrica calculo) {
        return ordenar(filasPorEtiqueta(lineas, calculo));
    }

    /** Las partes más grandes primero, hasta {@link MotorMetricas#MAXIMO_DE_FILAS}. */
    List<Fila> ordenar(List<Fila> filas) {
        return filas.stream().sorted(DE_MAYOR_A_MENOR).limit(MotorMetricas.MAXIMO_DE_FILAS).toList();
    }

    private List<Fila> filasPorEtiqueta(List<LineaMetrica> lineas, CalculoMetrica calculo) {
        Map<String, List<LineaMetrica>> grupos = lineas.stream()
                .collect(Collectors.groupingBy(etiqueta, LinkedHashMap::new, Collectors.toList()));
        return grupos.entrySet().stream()
                .map(grupo -> new Fila(grupo.getKey(), calculo.calcular(grupo.getValue())))
                .toList();
    }
}
