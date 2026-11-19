package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Convierte un pedido en palabras («¿qué producto vendo más este mes?») en una receta de métrica, buscando en el
 * {@link DiccionarioDePedidos} qué tabla, qué cuenta, qué reparto y qué período menciona. Es un algoritmo fijo: no usa
 * IA y nunca pone un número; el número lo calcula después el {@link MotorMetricas}. Si no entiende de qué tabla se
 * trata, o la combinación no se puede calcular, lo dice con un motivo claro. Junto con la receta cuenta qué tomó por
 * defecto y qué palabras no entendió, para que la persona vea si se la entendió bien.
 */
public class InterpreteDePedidos {

    static final String NO_SE_ENTIENDE_LA_TABLA = "No me di cuenta de qué querés mirar. Decime si es de tus ventas, "
            + "tus gastos o tu stock, por ejemplo: «cuánto vendí esta semana».";

    private final DiccionarioDePedidos diccionario;

    public InterpreteDePedidos(DiccionarioDePedidos diccionario) {
        this.diccionario = diccionario;
    }

    public PedidoInterpretado interpretar(String texto) {
        PedidoEnPalabras escrito = PedidoEnPalabras.de(texto);
        PedidoEnPalabras calculable = escrito.sin(diccionario.noSeCalculan());
        PedidoInterpretado interpretado = PedidoInterpretado.de(
                loQueDice(calculable), escrito.nombreSugerido(), loQueNoEntendio(escrito, calculable));
        interpretado.receta().validar();
        return interpretado;
    }

    private LoQueDiceElPedido loQueDice(PedidoEnPalabras pedido) {
        FuenteMetrica fuente = laTablaQuePide(pedido);
        return new LoQueDiceElPedido(fuente, laCuentaQuePide(pedido), elRepartoQuePide(pedido),
                elPeriodoQuePide(pedido, fuente), pedido.menciona(diccionario.fiado()));
    }

    private FuenteMetrica laTablaQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(FuenteMetrica.values(), diccionario.fuentes())
                .orElseThrow(() -> new BusinessRuleException(NO_SE_ENTIENDE_LA_TABLA));
    }

    private Optional<CalculoMetrica> laCuentaQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(CalculoMetrica.values(), diccionario.calculos());
    }

    private Optional<AgrupacionMetrica> elRepartoQuePide(PedidoEnPalabras pedido) {
        return pedido.loPrimeroQueMenciona(AgrupacionMetrica.values(), diccionario.agrupaciones());
    }

    /** El stock es lo que hay hoy: no tiene un período que pedir. */
    private Optional<PeriodoMetrica> elPeriodoQuePide(PedidoEnPalabras pedido, FuenteMetrica fuente) {
        if (!fuente.tieneFecha()) {
            return Optional.empty();
        }
        return pedido.loPrimeroQueMenciona(PeriodoMetrica.values(), diccionario.periodos());
    }

    /** Primero lo que pide algo que no se calcula («el finde»), después las palabras que el diccionario no conoce. */
    private List<String> loQueNoEntendio(PedidoEnPalabras escrito, PedidoEnPalabras calculable) {
        return Stream.concat(escrito.loQueEscribioDe(diccionario.noSeCalculan()).stream(),
                        calculable.palabrasFueraDe(diccionario.frasesConocidas()).stream())
                .distinct()
                .toList();
    }
}
