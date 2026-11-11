package com.ensambla.backend.domain.model.pedido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Lo que Sami le contesta al cliente que le pide por WhatsApp. */
public final class TextosPedido {

    public static final String BIENVENIDA = "¡Hola! Soy Sami, de Ensambla. Te ayudo a armar tu pedido y a retirarlo "
            + "cuando esté listo.\n¿A qué comercio querés pedirle? Escribime el nombre.";
    public static final String PREGUNTAR_COMERCIO = "Dale. ¿A qué comercio querés pedirle? Escribime el nombre.";
    public static final String COMERCIO_NO_DISPONIBLE = "Ese comercio ya no recibe pedidos por acá. ¿A cuál otro "
            + "querés pedirle? Escribime el nombre.";
    public static final String NO_ENTENDI_PEDIDO = "No entendí qué querés pedir. Escribime algo como «1 kilo de "
            + "yerba, aceite y fideos».";

    public static final String PEDIDO_DESCARTADO = "Listo, descarté la lista. Escribime qué querés pedir cuando "
            + "quieras.";
    public static final String RENGLON_NO_ENCONTRADO = "No encontré ese renglón de la lista. Escribí *CAMBIAR* y el "
            + "número del renglón, por ejemplo *CAMBIAR 1*.";

    private static final String CONFIRMAR = "Respondé *SÍ* para confirmar tu pedido o *NO* para descartarlo.";
    private static final String CONFIRMAR_Y_CAMBIAR = "Respondé *SÍ* para confirmar tu pedido, *NO* para descartarlo "
            + "o *CAMBIAR 1* (el número del renglón) para elegir otra marca.";
    private static final String PIE_DE_LISTA = "Para pedir otra cosa, escribímelo de nuevo. Para elegir otro "
            + "comercio, escribí *OTRO COMERCIO*.";
    private static final String CAMBIO_DE_STOCK = "Mientras tanto cambió el precio o el stock de: %s. Escribime de "
            + "nuevo qué querés pedir y armo la lista con lo que hay.";
    private static final String PEDIDO_CONFIRMADO = "¡Listo! Mandé tu pedido a *%s* (total %s). Te aviso por acá cuando "
            + "el comercio lo acepte y cuando esté listo para retirar. Podés pedir otra cosa cuando quieras.";
    private static final String SIN_ALTERNATIVAS = "No tengo otras marcas de *%s*. Si querés, escribime otro pedido.";
    private static final String OPCION_DE_MARCA_INVALIDA = "Elegí un número entre 1 y %d, o respondé NO para dejarlo "
            + "como está.";
    private static final String SIN_PRODUCTOS = "*%s* todavía no tiene productos con precio y stock cargados para "
            + "pedir por acá. Si querés otro comercio, escribí *OTRO COMERCIO*.";
    private static final String SIN_COINCIDENCIAS_PIE = "Probá con otra cosa, o escribí *OTRO COMERCIO* para elegir otro "
            + "comercio.";

    private static final Locale ARGENTINA = Locale.of("es", "AR");
    private static final String BIENVENIDA_CON_ULTIMO = "¡Hola de nuevo! Soy Sami, de Ensambla. ¿Seguimos con *%s*? "
            + "Respondé *SÍ* para seguir con ese comercio o escribime el nombre de otro.";
    private static final String NO_ENCONTRADO = "No encontré ningún comercio que se llame «%s». Probá con el nombre "
            + "tal como figura en el cartel o en la app.";
    private static final String OPCION_INVALIDA = "Elegí un número entre 1 y %d, o respondé NO para escribir otro nombre.";
    private static final String COMERCIO_ELEGIDO = "Listo, te atiende *%s*. ¿Qué necesitás? Escribime lo que querés "
            + "pedir con tus palabras, por ejemplo «1 kilo de yerba, aceite y fideos». Si querés otro comercio, "
            + "escribí *OTRO COMERCIO*.";

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

    public static String sinProductos(String comercio) {
        return SIN_PRODUCTOS.formatted(comercio);
    }

    /** La lista de compra con precios y total, y lo que el comercio no tiene con su recomendación. */
    public static String lista(String comercio, ArmadoDeLista armado) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Tu pedido en *%s*:".formatted(comercio));
        IntStream.range(0, armado.lineas().size()).forEach(i -> {
            LineaDeCompra linea = armado.lineas().get(i);
            lineas.add("%d) %s x%s — %s%s".formatted(i + 1, linea.producto().nombre(), cantidad(linea.cantidad()),
                    dinero(linea.subtotal()),
                    linea.limitadaPorStock() ? " (solo tengo " + cantidad(linea.producto().stock()) + ")" : ""));
            if (!linea.alternativas().isEmpty()) {
                lineas.add("   También tengo: " + linea.alternativas().stream()
                        .map(ProductoEnStock::nombre).collect(Collectors.joining(", ")));
            }
        });
        lineas.add("Total: " + dinero(armado.total()));
        armado.faltantes().forEach(faltante -> lineas.add(faltante(faltante)));
        boolean hayAlternativas = armado.lineas().stream().anyMatch(linea -> !linea.alternativas().isEmpty());
        lineas.add(hayAlternativas ? CONFIRMAR_Y_CAMBIAR : CONFIRMAR);
        lineas.add(PIE_DE_LISTA);
        return String.join("\n", lineas);
    }

    /** Las otras marcas del renglón, numeradas, para que el cliente elija una. */
    public static String alternativas(LineaDeCompra linea) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Para *%s* tengo estas otras opciones. Respondé con el número:".formatted(
                linea.producto().nombre()));
        IntStream.range(0, linea.alternativas().size()).forEach(i -> lineas.add("%d) %s — %s".formatted(i + 1,
                linea.alternativas().get(i).nombre(), dinero(linea.alternativas().get(i).precio()))));
        lineas.add("O respondé NO para dejarlo como está.");
        return String.join("\n", lineas);
    }

    public static String sinAlternativas(String producto) {
        return SIN_ALTERNATIVAS.formatted(producto);
    }

    public static String opcionDeMarcaInvalida(int cantidad) {
        return OPCION_DE_MARCA_INVALIDA.formatted(cantidad);
    }

    /** Qué productos cambiaron de precio o se quedaron sin stock entre que se armó la lista y se confirmó. */
    public static String cambioDeStock(List<String> productos) {
        return CAMBIO_DE_STOCK.formatted(String.join(", ", productos));
    }

    public static String pedidoConfirmado(String comercio, BigDecimal total) {
        return PEDIDO_CONFIRMADO.formatted(comercio, dinero(total));
    }

    /** Cuando nada de lo pedido está en el stock: lo que falta y cómo seguir. */
    public static String sinCoincidencias(List<ProductoFaltante> faltantes) {
        List<String> lineas = new ArrayList<>(faltantes.stream().map(TextosPedido::faltante).toList());
        lineas.add(SIN_COINCIDENCIAS_PIE);
        return String.join("\n", lineas);
    }

    private static String faltante(ProductoFaltante faltante) {
        return faltante.similar() == null
                ? "No tengo «%s».".formatted(faltante.pedido())
                : "No tengo «%s», pero tengo %s (%s).".formatted(faltante.pedido(), faltante.similar().nombre(),
                dinero(faltante.similar().precio()));
    }

    /** Pesos argentinos, sin los centavos cuando son cero: «$3.500» o «$3.500,50». */
    static String dinero(BigDecimal importe) {
        return String.format(ARGENTINA, "$%,.2f", importe).replace(",00", "");
    }

    static String cantidad(BigDecimal cantidad) {
        return cantidad.stripTrailingZeros().toPlainString();
    }
}
