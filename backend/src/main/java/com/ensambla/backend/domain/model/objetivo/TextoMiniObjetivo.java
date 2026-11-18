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

    public static TextoMiniObjetivo reponerStock(BigDecimal productos) {
        return new TextoMiniObjetivo(
                "Reponé lo que está por debajo de tu mínimo",
                "Productos en su mínimo o por debajo: hoy son " + numero(productos) + ". Reponelos y bajá ese número a "
                        + "0 para no quedarte sin.");
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
                "En los últimos 7 días vendiste " + pesos(ventasHoy) + ". Con " + pesos(ventasMeta)
                        + " por semana sumás cerca de " + pesos(aportePorMes) + " por mes.");
    }
}
