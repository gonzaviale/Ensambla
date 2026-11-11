package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.LineaPedida;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import com.ensambla.backend.domain.port.InterpretePedidoIaPort;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta sin IA, con una regla simple, para desarrollar y probar sin gastar: separa el mensaje en cosas por las
 * comas, los «y» y los «+»; de cada una toma la cantidad del principio (un número o «un», «dos», «medio»…) y deja el
 * resto como producto, sin la unidad («kilo de», «litro de»…).
 */
public class InterpretePedidoSimulado implements InterpretePedidoIaPort {

    private static final Pattern PEDIDO_DE_CORTESIA = Pattern.compile(
            "^(?:hola[ ,!]*)?(?:(?:dame|damelo|quiero|necesito|queria|quisiera|mandame|pasame|me das|me pasas|"
                    + "me mandas|haceme un pedido de|pedido de)\\s+)?");
    /** Una coma entre dos dígitos es un decimal («2,5 litros»), no el fin de una cosa. */
    private static final Pattern SEPARADOR = Pattern.compile("\\s*(?:(?<!\\d),|,(?!\\d)|;|\\+|\\sy\\s)\\s*");
    private static final Pattern RENGLON = Pattern.compile(
            "^(?<cantidad>\\d+(?:[.,]\\d+)?|(?:un|una|uno|dos|tres|cuatro|cinco|medio|media)\\b)?\\s*"
                    + "(?:(?:kilos?|kgs?|litros?|lts?|unidades?|paquetes?|packs?)\\s+)?(?:de\\s+)?(?<producto>.+)$");
    private static final Map<String, BigDecimal> PALABRAS = Map.of("un", BigDecimal.ONE, "una", BigDecimal.ONE,
            "uno", BigDecimal.ONE, "dos", BigDecimal.valueOf(2), "tres", BigDecimal.valueOf(3),
            "cuatro", BigDecimal.valueOf(4), "cinco", BigDecimal.valueOf(5), "medio", new BigDecimal("0.5"),
            "media", new BigDecimal("0.5"));

    @Override
    public PedidoInterpretado interpretar(ConsultaPedido consulta) {
        String texto = PEDIDO_DE_CORTESIA.matcher(TextoNormalizado.de(consulta.texto())).replaceFirst("");
        List<LineaPedida> lineas = Arrays.stream(SEPARADOR.split(texto))
                .map(String::strip)
                .map(RENGLON::matcher)
                .filter(Matcher::matches)
                .map(InterpretePedidoSimulado::linea)
                .toList();
        return new PedidoInterpretado(lineas, null);
    }

    private static LineaPedida linea(Matcher renglon) {
        String cantidad = renglon.group("cantidad");
        BigDecimal valor = cantidad == null ? null
                : PALABRAS.getOrDefault(cantidad, cantidad.matches("\\d.*") ? numero(cantidad) : null);
        return new LineaPedida(renglon.group("producto").strip(), valor);
    }

    private static BigDecimal numero(String texto) {
        return new BigDecimal(texto.replace(',', '.'));
    }
}
