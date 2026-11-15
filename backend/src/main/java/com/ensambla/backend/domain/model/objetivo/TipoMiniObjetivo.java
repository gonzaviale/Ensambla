package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.numero;
import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.pesos;

/** Qué clase de cambio o de meta es un mini-objetivo y qué métrica mide. */
public enum TipoMiniObjetivo {
    /** Ganancia semanal de un producto con margen bajo. */
    SUBIR_PRECIO(
            SentidoMiniObjetivo.SUBE, true,
            (medidor, producto, referencia) -> producto.map(medidor::gananciaSemanal),
            valor -> pesos(valor) + " por semana"),
    /** Unidades por semana de un producto parado. */
    MOVER_PARADO(
            SentidoMiniObjetivo.SUBE, true,
            (medidor, producto, referencia) -> producto.map(dato -> medidor.unidades(dato.id(), MedidorMiniObjetivo.DIAS_SEMANA)),
            valor -> numero(valor) + " por semana"),
    /** Unidades por semana del producto que más deja por unidad. */
    PONER_A_LA_VISTA(
            SentidoMiniObjetivo.SUBE, true,
            (medidor, producto, referencia) -> producto.map(dato -> medidor.unidades(dato.id(), MedidorMiniObjetivo.DIAS_SEMANA)),
            valor -> numero(valor) + " por semana"),
    /** Gasto de 30 días de una categoría. */
    RECORTAR_GASTO(
            SentidoMiniObjetivo.BAJA, false,
            (medidor, producto, referencia) -> Optional.of(medidor.gastoDe(referencia)),
            valor -> pesos(valor) + " en el mes"),
    /** Margen bruto de 30 días, en puntos. */
    MEJORAR_MARGEN(
            SentidoMiniObjetivo.SUBE, false,
            (medidor, producto, referencia) -> medidor.margen(),
            valor -> numero(valor) + " %"),
    /** Ventas de los últimos 7 días. */
    VENTAS_SEMANALES(
            SentidoMiniObjetivo.SUBE, false,
            (medidor, producto, referencia) -> Optional.of(medidor.ventasSemana()),
            valor -> pesos(valor) + " por semana"),
    /** Cuántos productos están en el mínimo que definió el negocio o por debajo. */
    PRODUCTOS_BAJO_EL_MINIMO(
            SentidoMiniObjetivo.BAJA, false,
            (medidor, producto, referencia) -> medidor.productosBajoElMinimo(),
            valor -> numero(valor) + " productos bajo el mínimo"),
    /** Pesos de mercadería, a su costo, de lo que no se vendió en 30 días. */
    CAPITAL_PARADO(
            SentidoMiniObjetivo.BAJA, false,
            (medidor, producto, referencia) -> medidor.capitalParado(),
            valor -> pesos(valor) + " en mercadería parada"),
    /** Cuántos productos distintos se vendieron en los últimos 7 días. */
    VARIEDAD_VENDIDA(
            SentidoMiniObjetivo.SUBE, false,
            (medidor, producto, referencia) -> medidor.variedadVendida(),
            valor -> numero(valor) + " productos distintos por semana");

    /** Cómo se mide esta métrica en un negocio: con el producto al que apunta o con la referencia que guarda. */
    @FunctionalInterface
    private interface Medicion {

        Optional<BigDecimal> medir(MedidorMiniObjetivo medidor, Optional<MedidorMiniObjetivo.Producto> producto,
                                   String referencia);
    }

    private final SentidoMiniObjetivo sentido;

    private final boolean deProducto;

    private final Medicion medicion;

    private final Function<BigDecimal, String> comoSeDice;

    TipoMiniObjetivo(SentidoMiniObjetivo sentido, boolean deProducto, Medicion medicion,
                     Function<BigDecimal, String> comoSeDice) {
        this.sentido = sentido;
        this.deProducto = deProducto;
        this.medicion = medicion;
        this.comoSeDice = comoSeDice;
    }

    public SentidoMiniObjetivo sentido() {
        return sentido;
    }

    /** Si apunta a un producto del cuaderno (y por eso guarda su {@code registroId}). */
    public boolean esDeProducto() {
        return deProducto;
    }

    /** El valor de esta métrica hoy; vacío si no se puede medir. */
    Optional<BigDecimal> medirEn(MedidorMiniObjetivo medidor, Optional<MedidorMiniObjetivo.Producto> producto,
                                 String referencia) {
        return medicion.medir(medidor, producto, referencia);
    }

    /** El valor como lo dice el negocio: pesos, unidades o puntos de margen. */
    String decir(BigDecimal valor) {
        return comoSeDice.apply(valor);
    }
}
