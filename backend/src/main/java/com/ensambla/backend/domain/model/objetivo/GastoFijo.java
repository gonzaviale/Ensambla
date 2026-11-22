package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.List;

/**
 * Los gastos que un comercio no puede bajar de una semana a la otra (alquiler, sueldos, impuestos, cuotas): proponer
 * «gastá un 10 % menos de alquiler» es pedir algo que no depende de la persona. Se reconocen por las palabras de la
 * categoría, escrita como venga («Alquiler», «alquileres», «Sueldo empleada»).
 */
public final class GastoFijo {

    private static final List<List<String>> FRASES = List.of(
                    "alquiler", "expensas", "sueldo", "salario", "aguinaldo", "cargas sociales", "impuestos",
                    "monotributo", "ingresos brutos", "iibb", "afip", "arca", "abl", "tasa municipal", "seguro",
                    "cuota", "prestamo", "credito", "hipoteca", "leasing")
            .stream()
            .map(TextoNormalizado::palabras)
            .toList();

    private GastoFijo() {
    }

    /** Si la categoría es un gasto fijo, que no se le propone recortar. */
    public static boolean es(String categoria) {
        List<String> palabras = TextoNormalizado.palabras(categoria);
        return FRASES.stream().anyMatch(frase -> TextoNormalizado.contiene(palabras, frase));
    }

    /** Si el gasto es de los que la persona sí puede bajar. */
    public static boolean sePuedeRecortar(MedidorMiniObjetivo.Gasto gasto) {
        return !es(gasto.categoria());
    }
}
