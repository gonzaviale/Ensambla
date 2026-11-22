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
                        + " más, aunque se vendan algunos menos, ganás cerca de " + pesos(aportePorMes)
                        + " más por mes.");
    }

    public static TextoMiniObjetivo moverParado(
            String producto, BigDecimal stock, BigDecimal capital, BigDecimal rebaja, BigDecimal unidadesPorSemana) {
        return new TextoMiniObjetivo(
                "Volvé a vender «" + producto + "»",
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
                        + numero(unidadesHoy) + " a " + numero(unidadesMeta) + " por semana, ganás cerca de "
                        + pesos(aportePorMes) + " más por mes.");
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
                "Hoy de cada $1.000 que vendés te quedan " + deCadaMil(margenHoy) + " después de pagar la "
                        + "mercadería. Con " + deCadaMil(margenMeta) + " ganás cerca de " + pesos(aportePorMes)
                        + " más por mes.");
    }

    public static TextoMiniObjetivo reponerStock(BigDecimal productos) {
        return new TextoMiniObjetivo(
                "Reponé lo que está por debajo de tu mínimo",
                "Productos en su mínimo o por debajo: hoy son " + numero(productos) + ". Reponelos y bajá ese número a "
                        + "0 para no quedarte sin stock.");
    }

    public static TextoMiniObjetivo capitalParado(BigDecimal capital, BigDecimal meta) {
        return new TextoMiniObjetivo(
                "Liberá plata parada en mercadería",
                "Tenés " + pesos(capital) + " en mercadería que no se vendió en el último mes. Si la bajás a "
                        + pesos(meta) + " liberás " + pesos(capital.subtract(meta)) + " para comprar lo que sí rota.");
    }

    public static TextoMiniObjetivo variedadVendida(BigDecimal hoy, BigDecimal meta) {
        return new TextoMiniObjetivo(
                "Vendé más variedad por semana",
                "Productos distintos que vendiste en los últimos 7 días: " + numero(hoy) + ". Si llegás a "
                        + numero(meta) + " le mostrás más opciones a tus clientes.");
    }

    public static TextoMiniObjetivo ventasSemanales(
            BigDecimal ventasHoy, BigDecimal ventasMeta, BigDecimal aportePorMes) {
        return new TextoMiniObjetivo(
                "Vender un 10 % más por semana",
                "Vendiste " + pesos(ventasHoy) + " en los últimos 7 días. Si vendés "
                        + pesos(ventasMeta.subtract(ventasHoy)) + " más por semana (llegás a " + pesos(ventasMeta)
                        + "), ganás cerca de " + pesos(aportePorMes) + " más por mes.");
    }

    /** Lo que queda de cada $1.000 vendidos con un margen en puntos: 25,9 puntos son $259. */
    private static String deCadaMil(BigDecimal puntos) {
        return pesos(puntos.multiply(BigDecimal.TEN));
    }
}
