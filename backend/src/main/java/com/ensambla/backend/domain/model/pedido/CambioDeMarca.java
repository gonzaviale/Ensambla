package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.whatsapp.CoincidenciaDeFilas;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cómo pide el cliente cambiar de marca un renglón de la lista: «cambiar 2» (el número del renglón) o «cambiar el aceite»
 * (el producto, aunque lo escriba como pueda).
 */
public final class CambioDeMarca {

    private static final Pattern PEDIDO = Pattern.compile("^(?:cambiar|cambia|cambio)\\s+(?:(?:el|la|los|las|de|del)\\s+)*(.+)$");
    private static final Pattern POR = Pattern.compile("\\s+por\\s+");
    private static final Pattern NUMERO = Pattern.compile("\\d{1,2}");

    private CambioDeMarca() {
    }

    /** Lo que quiere cambiar según el mensaje («2», «aceite»); vacío si el mensaje no pide un cambio. */
    public static Optional<String> pedido(String texto) {
        Matcher pedido = PEDIDO.matcher(TextoNormalizado.de(texto));
        return pedido.matches() ? Optional.of(POR.split(pedido.group(1).strip())[0]) : Optional.empty();
    }

    /** El número del renglón (desde 1) al que se refiere; vacío si no hay un renglón con ese número o nombre. */
    public static OptionalInt renglon(String quiere, List<LineaDeCompra> lista) {
        if (NUMERO.matcher(quiere).matches()) {
            int numero = Integer.parseInt(quiere);
            return numero >= 1 && numero <= lista.size() ? OptionalInt.of(numero) : OptionalInt.empty();
        }
        return CoincidenciaDeFilas.buscar(quiere, lista.stream().map(linea -> linea.producto().nombre()).toList())
                .map(posicion -> OptionalInt.of(posicion + 1))
                .orElseGet(OptionalInt::empty);
    }
}
