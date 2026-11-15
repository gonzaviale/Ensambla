package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;

import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.numero;
import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.pesos;

/**
 * El título y la justificación de un mini-objetivo. Las fábricas arman el texto de reserva, sin IA y con los números
 * ya calculados; la IA sólo puede redactarlo distinto, nunca cambia un número.
 */
public record TextoMiniObjetivo(String titulo, String justificacion) {

    public static TextoMiniObjetivo subirPrecio(
            String producto, BigDecimal aumento, BigDecimal gananciaPorUnidad, BigDecimal margenPorciento,
            BigDecimal unidadesPorSemana, BigDecimal aportePorMes) {
        return new TextoMiniObjetivo(
                "Subile " + pesos(aumento) + " a " + producto,
                "Te deja " + pesos(gananciaPorUnidad) + " por unidad (" + numero(margenPorciento) + " %) y vendés "
                        + "cerca de " + numero(unidadesPorSemana) + " por semana. Con " + pesos(aumento)
                        + " más, aunque se vendan algunos menos, sumás cerca de " + pesos(aportePorMes)
                        + " por mes.");
    }

    public static TextoMiniObjetivo moverParado(
            String producto, BigDecimal stock, BigDecimal capital, BigDecimal rebaja, BigDecimal unidadesPorSemana) {
        return new TextoMiniObjetivo(
                "Mové «" + producto + "»",
                "En el último mes no se vendió y tenés " + numero(stock) + " guardados (" + pesos(capital)
                        + " en mercadería). Bajale " + pesos(rebaja) + " y apuntá a " + numero(unidadesPorSemana)
                        + " por semana.");
    }

    public static TextoMiniObjetivo ponerALaVista(
            String producto, BigDecimal gananciaPorUnidad, BigDecimal unidadesHoy, BigDecimal unidadesMeta,
            BigDecimal aportePorMes) {
        return new TextoMiniObjetivo(
                "Poné a la vista «" + producto + "»",
                "Es de los que más te dejan por unidad: " + pesos(gananciaPorUnidad) + ". Si pasás de "
                        + numero(unidadesHoy) + " a " + numero(unidadesMeta) + " por semana, sumás cerca de "
                        + pesos(aportePorMes) + " por mes.");
    }

    public static TextoMiniObjetivo recortarGasto(
            String categoria, BigDecimal gastoHoy, BigDecimal gastoMeta, BigDecimal ahorroPorMes) {
        return new TextoMiniObjetivo(
                "Gastar un 10 % menos en " + categoria,
                "Gastaste " + pesos(gastoHoy) + " en el último mes en " + categoria + ". Con " + pesos(gastoMeta)
                        + " ahorrás " + pesos(ahorroPorMes) + " por mes.");
    }

    public static TextoMiniObjetivo mejorarMargen(
            BigDecimal margenHoy, BigDecimal margenMeta, BigDecimal aportePorMes) {
        return new TextoMiniObjetivo(
                "Ganar " + numero(margenMeta.subtract(margenHoy)) + " puntos más en lo que vendés",
                "Hoy de cada $100 que vendés te quedan $" + numero(margenHoy) + " después de pagar la mercadería. "
                        + "Con $" + numero(margenMeta) + " sumás cerca de " + pesos(aportePorMes) + " por mes.");
    }

    public static TextoMiniObjetivo ventasSemanales(
            BigDecimal ventasHoy, BigDecimal ventasMeta, BigDecimal aportePorMes) {
        return new TextoMiniObjetivo(
                "Vender un 10 % más por semana",
                "En los últimos 7 días vendiste " + pesos(ventasHoy) + ". Con " + pesos(ventasMeta)
                        + " por semana sumás cerca de " + pesos(aportePorMes) + " por mes.");
    }
}
