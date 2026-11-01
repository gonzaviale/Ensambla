package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Lo que Sami le contesta a la persona por WhatsApp. */
public final class TextosWhatsapp {

    public static final int MAX_FILAS_EN_RESUMEN = 10;

    public static final String BIENVENIDA = "¡Listo! Soy Sami, de Ensambla. Mandame una foto de tu cuaderno o "
            + "escribime algo como «gasté 5000 en harina» y lo cargo en tus tablas. Antes de guardar siempre te voy "
            + "a pedir que confirmes.";
    public static final String VINCULADO = "Listo, tu número quedó vinculado a tu negocio en Ensambla.";
    public static final String NO_VINCULADO = "Este número no está vinculado a ninguna cuenta de Ensambla. "
            + "Verificalo desde la app, en Tu cuenta › WhatsApp, o mandame: VINCULAR <código>";
    public static final String SOLO_TEXTO_Y_FOTOS = "Por ahora sólo entiendo textos y fotos. Escribime lo que "
            + "querías cargar o mandame una foto del cuaderno.";
    public static final String LEYENDO_FOTO = "Recibí tu foto, la estoy leyendo…";
    public static final String DESCARTADO = "Listo, no guardé nada.";
    public static final String DESCARTE_ANTERIOR = "Descarté la carga anterior que no confirmaste.";
    public static final String VENCIDA = "Esa carga venció sin confirmarse. Mandámela de nuevo.";
    public static final String NO_ENTENDI = "No encontré nada para cargar en ese mensaje. Probá con algo como "
            + "«gasté 5000 en harina» o mandame una foto del cuaderno.";
    public static final String SIN_TABLAS = "Todavía no tenés tablas para cargar datos. Armá tu sistema en la app y "
            + "volvé a escribirme.";
    public static final String TABLA_INEXISTENTE = "Esa tabla ya no existe. Mandame los datos de nuevo.";
    public static final String LIMITE_SAMI = "Hoy ya usaste todas las consultas con Sami. Probá mañana o cargalo desde la app.";
    public static final String ERROR_TEMPORAL = "Tuve un problema con tu mensaje. Probá de nuevo en un rato.";
    public static final String FOTO_NO_DESCARGADA = "No pude descargar tu foto. Mandámela de nuevo.";

    private static final String CODIGO_VERIFICACION =
            "Tu código de Ensambla es %s. Vence en 10 minutos. Si no lo pediste, ignorá este mensaje.";
    private static final String PIE_DE_CONFIRMACION = "¿Lo guardo? Respondé *SÍ* para guardar o *NO* para "
            + "descartar. Si algo está mal, respondé NO y mandámelo corregido.";

    private TextosWhatsapp() {
    }

    public static String codigoVerificacion(String codigo) {
        return CODIGO_VERIFICACION.formatted(codigo);
    }

    /** Qué campos no se pudieron guardar y por qué, con el nombre de cada campo en lugar de su id. */
    public static String datosInvalidos(Map<String, String> errores, Map<String, String> nombresPorCampo) {
        String detalle = errores.entrySet().stream()
                .map(error -> "%s (%s)".formatted(nombresPorCampo.getOrDefault(error.getKey(), "dato"),
                        error.getValue().toLowerCase()))
                .collect(Collectors.joining(", "));
        return "No guardé nada porque falta o está mal: " + detalle + ". Mandame los datos de nuevo.";
    }

    public static String guardado(int nuevas, int actualizadas, String tabla) {
        return guardado(nuevas, actualizadas, tabla, List.of(), List.of());
    }

    /**
     * @param sumados lo que se sumó en otras tablas para vincularlo («Marta en Clientes»)
     * @param stock   cómo quedó el stock de cada producto que movió la carga
     */
    public static String guardado(int nuevas, int actualizadas, String tabla, List<String> sumados,
                                  List<MovimientoDeStock> stock) {
        String cambios = actualizadas == 0 ? "guardé %d %s".formatted(nuevas, filas(nuevas))
                : nuevas == 0 ? "actualicé %d %s".formatted(actualizadas, filas(actualizadas))
                : "actualicé %d y agregué %d filas".formatted(actualizadas, nuevas);
        List<String> lineas = new ArrayList<>();
        lineas.add("Listo, %s en *%s*.".formatted(cambios, tabla));
        if (!sumados.isEmpty()) {
            lineas.add("También sumé " + String.join(", ", sumados) + ".");
        }
        stock.forEach(movimiento -> lineas.add(stock(movimiento)));
        lineas.add("Si tenés la app abierta, actualizá la página para verlas.");
        return String.join("\n", lineas);
    }

    private static String stock(MovimientoDeStock movimiento) {
        String linea = "Stock de %s: %s → %s.".formatted(movimiento.producto(), movimiento.antes().toPlainString(),
                movimiento.despues().toPlainString());
        return movimiento.noAlcanzo()
                ? linea + " No alcanzaba para %s: lo dejé en 0.".formatted(movimiento.pedido().toPlainString())
                : linea;
    }

    private static String filas(int cantidad) {
        return cantidad == 1 ? "fila" : "filas";
    }

    /** La carga tal como quedaría, con hasta {@value #MAX_FILAS_EN_RESUMEN} filas, para que la persona confirme. */
    public static String resumen(PropuestaWhatsapp propuesta, TablaDisponible tabla) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Para *%s* (%s) entendí:".formatted(tabla.bloque().nombre(), tabla.proyecto()));
        List<FilaPropuesta> filas = propuesta.getFilas();
        IntStream.range(0, Math.min(filas.size(), MAX_FILAS_EN_RESUMEN))
                .forEach(i -> lineas.add("%d) %s".formatted(i + 1, accion(filas.get(i)) + fila(filas.get(i), tabla))));
        if (filas.size() > MAX_FILAS_EN_RESUMEN) {
            lineas.add("…y %d filas más.".formatted(filas.size() - MAX_FILAS_EN_RESUMEN));
        }
        if (filas.stream().anyMatch(fila -> !fila.dudosos().isEmpty())) {
            lineas.add("(?) = dato que no pude leer bien.");
        }
        lineas.add(PIE_DE_CONFIRMACION);
        return String.join("\n", lineas);
    }

    /** La lista numerada de tablas entre las que la persona tiene que elegir. */
    public static String elegirTabla(List<TablaDisponible> tablas) {
        List<String> lineas = new ArrayList<>();
        lineas.add("No estoy seguro de en qué tabla va. Respondé con el número:");
        IntStream.range(0, tablas.size())
                .forEach(i -> lineas.add("%d) %s".formatted(i + 1, tablas.get(i).etiqueta())));
        lineas.add("O respondé NO para descartar.");
        return String.join("\n", lineas);
    }

    /** Si la fila actualiza una que ya existe lo dice con su nombre; si no, avisa que se agrega. */
    private static String accion(FilaPropuesta fila) {
        return fila.actualizaUnaFila() ? "Actualizo *%s*: ".formatted(fila.nombreExistente()) : "Agrego: ";
    }

    private static String fila(FilaPropuesta fila, TablaDisponible tabla) {
        Map<String, String> valores = fila.valores();
        return tabla.bloque().campos().stream()
                .filter(campo -> valores.containsKey(campo.id().toString()))
                .filter(campo -> !(fila.actualizaUnaFila() && campo.id().toString().equals(fila.campoNombre())))
                .map(campo -> celda(campo, valores, fila))
                .collect(Collectors.joining(" · "));
    }

    private static String celda(CampoConocido campo, Map<String, String> valores, FilaPropuesta fila) {
        String clave = campo.id().toString();
        return "%s: %s%s".formatted(campo.nombre(), valor(clave, valores.get(clave), fila),
                fila.dudosos().contains(clave) ? " (?)" : "");
    }

    /** Lo que se suma muestra cómo queda («5 → 15 (+10)»); lo que se vincula, la fila que encontró o que suma. */
    private static String valor(String clave, String valor, FilaPropuesta fila) {
        VinculoPropuesto vinculo = fila.vinculos().get(clave);
        if (vinculo != null) {
            return vinculo.esNuevo()
                    ? "%s (nuevo, lo sumo a %s)".formatted(vinculo.nombre(), vinculo.tabla())
                    : vinculo.nombre();
        }
        String habia = fila.antes().get(clave);
        if (habia == null) {
            return valor;
        }
        return "%s → %s (%s)".formatted(habia, FilaPropuesta.sumado(habia, FilaPropuesta.cambio(valor))
                .toPlainString(), valor.strip());
    }
}
