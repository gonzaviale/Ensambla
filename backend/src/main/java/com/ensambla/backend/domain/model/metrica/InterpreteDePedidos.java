package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/**
 * Convierte un pedido en palabras («¿qué producto vendo más este mes?») en una receta de métrica, buscando en el
 * {@link DiccionarioDePedidos} qué tabla, qué cuenta, qué reparto y qué período menciona. Es un algoritmo fijo: no usa
 * IA y nunca pone un número; el número lo calcula después el {@link MotorMetricas}. Si no entiende de qué tabla se
 * trata, o la combinación no se puede calcular, lo dice con un motivo claro.
 */
public class InterpreteDePedidos {

    static final String NO_SE_ENTIENDE_LA_TABLA = "No me di cuenta de qué querés mirar. Decime si es de tus ventas, "
            + "tus gastos o tu stock, por ejemplo: «cuánto vendí esta semana».";

    private final DiccionarioDePedidos diccionario;

    public InterpreteDePedidos(DiccionarioDePedidos diccionario) {
        this.diccionario = diccionario;
    }

    public DefinicionMetrica interpretar(String texto) {
        PedidoEnPalabras pedido = PedidoEnPalabras.de(texto);
        FuenteMetrica fuente = laTablaQuePide(pedido);
        boolean soloFiado = pedido.menciona(diccionario.fiado());
        DefinicionMetrica receta = new DefinicionMetrica(pedido.nombreSugerido(), fuente, laCuentaQuePide(pedido),
                elRepartoQuePide(pedido), elPeriodoQuePide(pedido, fuente, soloFiado), soloFiado);
        receta.validar();
        return receta;
    }

    private FuenteMetrica laTablaQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(FuenteMetrica.values(), diccionario.fuentes())
                .orElseThrow(() -> new BusinessRuleException(NO_SE_ENTIENDE_LA_TABLA));
    }

    private CalculoMetrica laCuentaQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(CalculoMetrica.values(), diccionario.calculos())
                .orElse(CalculoMetrica.SUMA);
    }

    private AgrupacionMetrica elRepartoQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(AgrupacionMetrica.values(), diccionario.agrupaciones())
                .orElse(AgrupacionMetrica.NINGUNA);
    }

    /** El stock es lo que hay hoy y lo que te deben se acumula: sin un período dicho, miran todo lo cargado. */
    private PeriodoMetrica elPeriodoQuePide(PedidoEnPalabras pedido, FuenteMetrica fuente, boolean soloFiado) {
        if (!fuente.tieneFecha()) {
            return PeriodoMetrica.TODO;
        }
        PeriodoMetrica sinDecirlo = soloFiado ? PeriodoMetrica.TODO : PeriodoMetrica.MES;
        return pedido.loPrimeroQueMenciona(PeriodoMetrica.values(), diccionario.periodos()).orElse(sinDecirlo);
    }
}
