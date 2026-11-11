package com.ensambla.backend.domain.model.pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** Lo que Sami le contesta al cliente que le pide por WhatsApp. */
public final class TextosPedido {

    public static final String BIENVENIDA = "¡Hola! Soy Sami, de Ensambla. Te ayudo a armar tu pedido y a retirarlo "
            + "cuando esté listo.\n¿A qué comercio querés pedirle? Escribime el nombre.";
    public static final String PREGUNTAR_COMERCIO = "Dale. ¿A qué comercio querés pedirle? Escribime el nombre.";
    public static final String COMERCIO_NO_DISPONIBLE = "Ese comercio ya no recibe pedidos por acá. ¿A cuál otro "
            + "querés pedirle? Escribime el nombre.";
    public static final String PEDIDO_EN_CONSTRUCCION = "Anoté tu mensaje, pero todavía no sé armar la lista de "
            + "compra. Si querés elegir otro comercio, escribí *CAMBIAR*.";

    private static final String BIENVENIDA_CON_ULTIMO = "¡Hola de nuevo! Soy Sami, de Ensambla. ¿Seguimos con *%s*? "
            + "Respondé *SÍ* para seguir con ese comercio o escribime el nombre de otro.";
    private static final String NO_ENCONTRADO = "No encontré ningún comercio que se llame «%s». Probá con el nombre "
            + "tal como figura en el cartel o en la app.";
    private static final String OPCION_INVALIDA = "Elegí un número entre 1 y %d, o respondé NO para escribir otro nombre.";
    private static final String COMERCIO_ELEGIDO = "Listo, te atiende *%s*. ¿Qué necesitás? Escribime lo que querés "
            + "pedir con tus palabras, por ejemplo «1 kilo de yerba, aceite y fideos». Si querés otro comercio, "
            + "escribí *CAMBIAR*.";

    private TextosPedido() {
    }

    public static String bienvenidaConUltimo(String comercio) {
        return BIENVENIDA_CON_ULTIMO.formatted(comercio);
    }

    public static String comercioNoEncontrado(String escrito) {
        return NO_ENCONTRADO.formatted(escrito.strip());
    }

    /** La lista numerada de comercios parecidos entre los que el cliente tiene que elegir. */
    public static String variosComercios(List<Comercio> comercios) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Encontré varios comercios parecidos. Respondé con el número:");
        IntStream.range(0, comercios.size())
                .forEach(i -> lineas.add("%d) %s".formatted(i + 1, comercios.get(i).nombre())));
        lineas.add("O respondé NO para escribir otro nombre.");
        return String.join("\n", lineas);
    }

    public static String opcionInvalida(int cantidad) {
        return OPCION_INVALIDA.formatted(cantidad);
    }

    public static String comercioElegido(String comercio) {
        return COMERCIO_ELEGIDO.formatted(comercio);
    }
}
