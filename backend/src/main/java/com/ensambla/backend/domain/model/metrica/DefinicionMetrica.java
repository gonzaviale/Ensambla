package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Objects;

/**
 * La "receta" de una métrica: de qué tabla sale, qué se calcula, cómo se reparte y de qué período. Son datos, no una
 * fórmula: todo sale de listas cerradas y el servidor hace la cuenta, así que nadie puede pedir un número inventado.
 *
 * @param soloFiado sólo las ventas anotadas como fiadas (únicamente para la tabla de ventas)
 */
public record DefinicionMetrica(
        String nombre,
        FuenteMetrica fuente,
        CalculoMetrica calculo,
        AgrupacionMetrica agrupar,
        PeriodoMetrica periodo,
        boolean soloFiado) {

    public static final int LARGO_MAXIMO_DEL_NOMBRE = 80;

    public DefinicionMetrica {
        nombre = Objects.requireNonNullElse(nombre, "").trim();
        agrupar = Objects.requireNonNullElse(agrupar, AgrupacionMetrica.NINGUNA);
        periodo = Objects.requireNonNullElse(periodo, PeriodoMetrica.MES);
    }

    /** Rechaza con un motivo claro la receta que no se puede calcular; no hace nada si es válida. */
    public void validar() {
        rechazarSi(!tieneUnNombreValido(),
                "El nombre de la métrica es obligatorio y tiene hasta " + LARGO_MAXIMO_DEL_NOMBRE + " letras");
        rechazarSi(fuente == null || calculo == null, "Elegí de qué tabla sale la métrica y qué querés calcular");
        rechazarSi(!calculo.sirvePara(fuente), "Los gastos no tienen unidades: elegí sumar, promediar o contar");
        rechazarSi(!fuente.permiteRepartirPor(agrupar), "No se puede repartir " + fuente.getFilas() + " de esa manera");
        rechazarSi(soloFiado && !fuente.admiteFiado(), "Sólo las ventas se pueden marcar como fiadas");
        rechazarSi(!fuente.tieneFecha() && !periodo.miraTodo(), "El inventario es lo que hay hoy: no tiene períodos");
    }

    public FormatoMetrica formato() {
        return calculo.formato();
    }

    /** Dos recetas son la misma si piden lo mismo, aunque el nombre sea distinto. */
    public boolean esIgualA(DefinicionMetrica otra) {
        return fuente == otra.fuente
                && calculo == otra.calculo
                && agrupar == otra.agrupar
                && periodo == otra.periodo
                && soloFiado == otra.soloFiado;
    }

    private boolean tieneUnNombreValido() {
        return !nombre.isEmpty() && nombre.length() <= LARGO_MAXIMO_DEL_NOMBRE;
    }

    private static void rechazarSi(boolean noSePuede, String motivo) {
        if (noSePuede) {
            throw new BusinessRuleException(motivo);
        }
    }
}
