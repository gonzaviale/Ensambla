package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.time.LocalDate;
import java.util.List;

/**
 * Calcula una {@link DefinicionMetrica} sobre las tablas de un cuaderno. Es lo único que pone el número: la receta sólo
 * elige tabla, cuenta, reparto y período. Lee los datos por su clave semántica (así no importa cómo se llame la columna
 * en cada cuaderno), deja afuera las filas de ejemplo (ya vienen filtradas en {@link DatosDeProyecto}) y, si no hay
 * con qué calcular, dice por qué en vez de devolver un cero.
 */
public class MotorMetricas {

    /** Cuántas partes del reparto se muestran como máximo. */
    public static final int MAXIMO_DE_FILAS = 10;

    public ResultadoMetrica calcular(DefinicionMetrica definicion, DatosDeProyecto datos, LocalDate hoy) {
        if (!definicion.fuente().estaEn(datos)) {
            return sinValor(definicion, faltaLaTabla(definicion), hoy, 0);
        }
        List<LineaMetrica> delPeriodo = lineasDelPeriodo(definicion, datos, hoy);
        if (delPeriodo.isEmpty()) {
            return sinValor(definicion, sinFilas(definicion), hoy, 0);
        }
        List<LineaMetrica> conElDato = lineasConElDato(delPeriodo, definicion.calculo());
        long sinDato = delPeriodo.size() - (long) conElDato.size();
        if (conElDato.isEmpty()) {
            return sinValor(definicion, definicion.calculo().datoQueFalta(definicion.fuente()), hoy, sinDato);
        }
        return conValor(definicion, conElDato, deDondeSale(definicion, hoy, conElDato.size(), sinDato));
    }

    private static List<LineaMetrica> lineasDelPeriodo(DefinicionMetrica definicion, DatosDeProyecto datos,
                                                       LocalDate hoy) {
        return definicion.fuente().lineasDe(datos).stream().filter(linea -> linea.entraEn(definicion, hoy)).toList();
    }

    private static List<LineaMetrica> lineasConElDato(List<LineaMetrica> lineas, CalculoMetrica calculo) {
        return lineas.stream().filter(calculo::aporta).toList();
    }

    private static ResultadoMetrica conValor(DefinicionMetrica definicion, List<LineaMetrica> lineas,
                                             DeDondeSale deDondeSale) {
        CalculoMetrica calculo = definicion.calculo();
        return ResultadoMetrica.conValor(definicion.formato(), calculo.calcular(lineas),
                definicion.agrupar().repartir(lineas, calculo), deDondeSale);
    }

    private static ResultadoMetrica sinValor(DefinicionMetrica definicion, String motivo, LocalDate hoy,
                                             long sinDato) {
        return ResultadoMetrica.sinValor(definicion.formato(), motivo, deDondeSale(definicion, hoy, 0, sinDato));
    }

    private static DeDondeSale deDondeSale(DefinicionMetrica definicion, LocalDate hoy, long usadas, long sinDato) {
        PeriodoMetrica periodo = definicion.periodo();
        return new DeDondeSale(definicion.fuente(), usadas, sinDato, periodo.desde(hoy), periodo.hasta(hoy));
    }

    private static String faltaLaTabla(DefinicionMetrica definicion) {
        return "Todavía no cargaste " + definicion.fuente().getFilas() + " en este negocio.";
    }

    private static String sinFilas(DefinicionMetrica definicion) {
        String filas = filasQueSeBuscan(definicion);
        return definicion.periodo().miraTodo()
                ? "Todavía no cargaste " + filas + "."
                : "No hay " + filas + " en este período.";
    }

    private static String filasQueSeBuscan(DefinicionMetrica definicion) {
        String filas = definicion.fuente().getFilas();
        return definicion.soloFiado() ? filas + " fiadas" : filas;
    }
}
