package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.numero;
import static com.ensambla.backend.domain.model.objetivo.FormatoObjetivo.pesos;

/**
 * Los textos de reserva del cierre semanal, sin IA y con los números ya calculados: el «qué funcionó» de la semana y el
 * motivo de cada reajuste.
 */
public final class TextoResumen {

    private TextoResumen() {
    }

    /**
     * Cómo terminó la semana: cómo cambió el resultado, qué decidió la persona y cuánto falta para la meta.
     *
     * @param anterior el resultado del cierre de la semana anterior; vacío si no lo hay
     * @param sinVentas si en la semana no hubo ventas, y por eso no se reajustó nada
     */
    public static String queFunciono(
            BigDecimal resultado, Optional<BigDecimal> anterior, ConteoDecisiones decisiones, BigDecimal meta,
            boolean alcanzado, boolean sinVentas) {
        return String.join(" ", cambio(resultado, anterior), decisiones(decisiones),
                cierre(resultado, meta, alcanzado, sinVentas));
    }

    /**
     * Por qué el cierre cambió {@code anterior} por otro mini-objetivo, {@code nuevo}. El que sólo sube su meta usa
     * {@link #metaSubida}.
     */
    public static String motivo(
            MotivoReajuste motivo, MiniObjetivo anterior, BigDecimal valorAlCierre, MiniObjetivo nuevo) {
        return switch (motivo) {
            case LOGRADO -> logrado(anterior, valorAlCierre, nuevo);
            case QUITADO -> "Quitaste «" + anterior.getTitulo() + "». En su lugar te proponemos «"
                    + nuevo.getTitulo() + "».";
            case PRODUCTO_FUERA -> "«" + anterior.getReferencia() + "» ya no está en tu cuaderno o le falta el precio "
                    + "o el costo. Lo cambiamos por «" + nuevo.getTitulo() + "».";
            case DECISION_SIN_EFECTO -> "Aplicaste un aviso para «" + anterior.getTitulo() + "» y las ventas no "
                    + "subieron. Probamos con «" + nuevo.getTitulo() + "».";
            case SIN_AVANCE -> "Hace 2 semanas que «" + anterior.getTitulo() + "» no avanza. Lo cambiamos por «"
                    + nuevo.getTitulo() + "».";
        };
    }

    /** La justificación del mini-objetivo que sigue a uno logrado, con su meta nueva. */
    public static String metaSubida(TipoMiniObjetivo tipo, BigDecimal valorAlCierre, BigDecimal nuevaMeta) {
        return "Lo lograste: llegaste a " + valor(tipo, valorAlCierre) + ". Ahora el objetivo es "
                + valor(tipo, nuevaMeta) + ".";
    }

    /** Uno logrado que ya no puede pedir más (el margen llegó a su tope) deja su lugar a otro. */
    private static String logrado(MiniObjetivo anterior, BigDecimal valorAlCierre, MiniObjetivo nuevo) {
        return "Lo lograste: llegaste a " + valor(anterior.getTipo(), valorAlCierre) + ". Como ya no se puede pedir "
                + "más, ahora probamos con «" + nuevo.getTitulo() + "».";
    }

    private static String cambio(BigDecimal resultado, Optional<BigDecimal> anterior) {
        if (anterior.isEmpty()) {
            return "Al cerrar la semana, lo que te queda de los últimos 30 días es " + pesos(resultado) + ".";
        }
        int comparacion = resultado.compareTo(anterior.get());
        if (comparacion == 0) {
            return "Esta semana tu resultado se mantuvo en " + pesos(resultado) + ".";
        }
        String verbo = comparacion > 0 ? "subió " : "bajó ";
        return "Esta semana tu resultado " + verbo + pesos(resultado.subtract(anterior.get()).abs()) + ": de "
                + pesos(anterior.get()) + " a " + pesos(resultado) + ".";
    }

    private static String decisiones(ConteoDecisiones decisiones) {
        if (decisiones.total() == 0) {
            return "No tomaste decisiones sobre tu objetivo.";
        }
        List<String> partes = new ArrayList<>();
        agregar(partes, decisiones.aplicadas(), "aplicaste", "aviso", "avisos");
        agregar(partes, decisiones.descartadas(), "descartaste", "aviso", "avisos");
        agregar(partes, decisiones.aceptadas(), "aceptaste", "paso", "pasos");
        agregar(partes, decisiones.ignoradas(), "dejaste para después", "paso", "pasos");
        String lista = partes.size() == 1
                ? partes.get(0)
                : String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
        return Character.toUpperCase(lista.charAt(0)) + lista.substring(1) + ".";
    }

    private static void agregar(List<String> partes, int cantidad, String verbo, String singular, String plural) {
        if (cantidad > 0) {
            partes.add(verbo + " " + cantidad + " " + (cantidad == 1 ? singular : plural));
        }
    }

    private static String cierre(BigDecimal resultado, BigDecimal meta, boolean alcanzado, boolean sinVentas) {
        if (alcanzado) {
            return "¡Llegaste a tu meta! Podés subirla para seguir creciendo.";
        }
        if (sinVentas) {
            return "No hubo ventas esta semana, así que no cambiamos tus mini-objetivos.";
        }
        return "Te faltan " + pesos(meta.subtract(resultado)) + " para tu meta.";
    }

    /** El valor de la métrica como lo dice el negocio: pesos, unidades o puntos de margen. */
    private static String valor(TipoMiniObjetivo tipo, BigDecimal valor) {
        return switch (tipo) {
            case SUBIR_PRECIO, VENTAS_SEMANALES -> pesos(valor) + " por semana";
            case RECORTAR_GASTO -> pesos(valor) + " en el mes";
            case MOVER_PARADO, PONER_A_LA_VISTA -> numero(valor) + " por semana";
            case MEJORAR_MARGEN -> numero(valor) + " %";
        };
    }
}
