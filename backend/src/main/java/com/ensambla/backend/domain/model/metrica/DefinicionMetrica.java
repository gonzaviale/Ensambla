package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    static final String CONTAR_CADA_PRODUCTO =
            "Cada producto es uno solo: contarlos por producto siempre da 1. Elegí la cantidad o el valor al costo";

    /** Lo que el panel de ventas del inicio ya muestra siempre: lo vendido hoy, esta semana y este mes. */
    private static final List<DefinicionMetrica> EL_PANEL_DE_VENTAS = Stream
            .of(PeriodoMetrica.HOY, PeriodoMetrica.SEMANA, PeriodoMetrica.MES)
            .map(periodo -> new DefinicionMetrica("Ventas", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                    AgrupacionMetrica.NINGUNA, periodo, false))
            .toList();

    public DefinicionMetrica {
        nombre = Objects.requireNonNullElse(nombre, "").trim();
        agrupar = Objects.requireNonNullElse(agrupar, AgrupacionMetrica.NINGUNA);
        periodo = Objects.requireNonNullElse(periodo, PeriodoMetrica.MES);
    }

    /** Rechaza con un motivo claro la receta que no se puede calcular; no hace nada si es válida. */
    public void validar() {
        rechazarSi(!tieneUnNombreValido(),
                "El nombre de la métrica es obligatorio y tiene hasta " + LARGO_MAXIMO_DEL_NOMBRE + " letras");
        rechazarSi(fuente == null || calculo == null, "Elegí qué querés mirar y qué cuenta querés hacer");
        rechazarSi(!calculo.sirvePara(fuente), calculo.motivoSiNoSirvePara(fuente));
        rechazarSi(!fuente.permiteRepartirPor(agrupar), "No se puede repartir " + fuente.getFilas() + " de esa manera");
        rechazarSi(soloFiado && !fuente.admiteFiado(), "Sólo las ventas se pueden marcar como fiadas");
        rechazarSi(!fuente.tieneFecha() && !periodo.miraTodo(), "El stock es lo que hay hoy: no tiene períodos");
        rechazarSi(calculo.cuentaFilas() && fuente.tieneUnaFilaPor(agrupar), CONTAR_CADA_PRODUCTO);
    }

    /** La misma receta con otro nombre: renombrar no cambia lo que se calcula. */
    public DefinicionMetrica conNombre(String otroNombre) {
        return new DefinicionMetrica(otroNombre, fuente, calculo, agrupar, periodo, soloFiado);
    }

    public FormatoMetrica formato() {
        return calculo.formato();
    }

    /** {@code true} si pide lo mismo que el panel de ventas del inicio, que ya está siempre a la vista. */
    public boolean laMuestraElPanelDeVentas() {
        return EL_PANEL_DE_VENTAS.stream().anyMatch(this::esIgualA);
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
