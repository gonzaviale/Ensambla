package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * Lo que la persona respondió de sus mini-objetivos manuales y lo que quedó sin responder; vacío si no había
     * ninguno. Lo que no respondió no se renueva hasta que responda.
     */
    public static String manuales(List<ManualCierre> manuales) {
        if (manuales.isEmpty()) {
            return "";
        }
        List<String> frases = new ArrayList<>();
        long hechas = contar(manuales, HizoManual.SI);
        long enParte = contar(manuales, HizoManual.EN_PARTE);
        long noHechas = contar(manuales, HizoManual.NO);
        if (hechas + enParte + noHechas > 0) {
            frases.add("De tus acciones respondiste: " + hechas + " hechas, " + enParte + " en parte y " + noHechas
                    + " sin hacer.");
        }
        manuales.stream()
                .filter(manual -> !manual.respondido())
                .forEach(manual -> frases.add("No me contaste cómo te fue con «" + manual.titulo()
                        + "»: queda igual hasta que respondas."));
        return String.join(" ", frases);
    }

    private static long contar(List<ManualCierre> manuales, HizoManual hizo) {
        return manuales.stream().filter(manual -> manual.hizo() == hizo).count();
    }

    /**
     * Por qué el cierre cambió {@code anterior} por otro mini-objetivo, {@code nuevo}. El que sólo sube su meta usa
     * {@link #metaSubida}.
     */
    public static String motivo(
            MotivoReajuste motivo, MiniObjetivo anterior, BigDecimal valorAlCierre, MiniObjetivo nuevo) {
        return motivo.explicar(anterior, valorAlCierre, nuevo);
    }

    /** La justificación del mini-objetivo que sigue a uno logrado, con su meta nueva. */
    public static String metaSubida(TipoMiniObjetivo tipo, BigDecimal valorAlCierre, BigDecimal nuevaMeta) {
        return "Lo lograste: llegaste a " + tipo.decir(valorAlCierre) + ". Ahora el objetivo es "
                + tipo.decir(nuevaMeta) + ".";
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

    /** Sin meta en pesos ({@code meta} vacía) no hay cuánto falta: sólo se sigue con los mini-objetivos. */
    private static String cierre(BigDecimal resultado, BigDecimal meta, boolean alcanzado, boolean sinVentas) {
        if (alcanzado) {
            return "¡Llegaste a tu meta! Podés subirla para seguir creciendo.";
        }
        if (sinVentas) {
            return "No hubo ventas esta semana, así que no cambiamos tus mini-objetivos.";
        }
        if (meta == null) {
            return "Seguí con tus mini-objetivos de la semana que viene.";
        }
        return "Te faltan " + pesos(meta.subtract(resultado)) + " para tu meta.";
    }
}
