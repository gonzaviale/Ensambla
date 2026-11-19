package com.ensambla.backend.domain.model.objetivo;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Cómo se renueva un mini-objetivo manual el domingo según lo que la persona respondió: si lo hizo y le funcionó sube un
 * paso; si lo hizo en parte se divide (queda lo que faltó, más chico); si no lo hizo por falta de tiempo o por otra
 * razón se simplifica; y si no funcionó, cambia por otra acción. Sin respuesta no se renueva.
 */
public enum AccionRenovacion {
    SUBE_UN_PASO(TipoReajuste.SUBIR_META,
            titulo -> new AccionManual(
                    recortar("Un paso más: " + titulo),
                    "Te salió bien «" + titulo + "». Repetilo esta semana y sumale algo más ambicioso."),
            (anterior, nuevo) -> "Hiciste «" + anterior + "» y te funcionó: seguimos con un paso más, «" + nuevo
                    + "»."),
    SE_DIVIDE(TipoReajuste.REEMPLAZAR,
            titulo -> new AccionManual(
                    recortar("Lo que faltó: " + titulo),
                    "Hiciste una parte de «" + titulo + "». Esta semana terminá lo que faltó, en un paso más chico."),
            (anterior, nuevo) -> "Hiciste «" + anterior + "» en parte: nos quedamos con lo que faltó, «" + nuevo
                    + "»."),
    SE_SIMPLIFICA(TipoReajuste.REEMPLAZAR,
            titulo -> new AccionManual(
                    recortar("Versión corta: " + titulo),
                    "No llegaste con «" + titulo + "». Probá una versión de 10 minutos de lo mismo."),
            (anterior, nuevo) -> "No llegaste con «" + anterior + "»: lo hacemos más simple, «" + nuevo + "»."),
    CAMBIA(TipoReajuste.PROBAR_OTRO_CAMINO,
            titulo -> new AccionManual(
                    "Probá otra acción para tu objetivo",
                    "«" + titulo + "» no dio resultado. Elegí una acción distinta hacia el mismo objetivo y "
                            + "contale a Sami cuál fue."),
            (anterior, nuevo) -> "«" + anterior + "» no dio resultado: probamos otra acción, «" + nuevo + "».");

    private final TipoReajuste tipoReajuste;

    private final Function<String, AccionManual> textoSinIa;

    private final BiFunction<String, String, String> porQueCambio;

    AccionRenovacion(TipoReajuste tipoReajuste, Function<String, AccionManual> textoSinIa,
                     BiFunction<String, String, String> porQueCambio) {
        this.tipoReajuste = tipoReajuste;
        this.textoSinIa = textoSinIa;
        this.porQueCambio = porQueCambio;
    }

    /** La clase de cambio que se anota en el resumen de la semana. */
    public TipoReajuste tipoReajuste() {
        return tipoReajuste;
    }

    public static AccionRenovacion de(HizoManual hizo, QuePaso quePaso) {
        return hizo.renovacionSegun(quePaso);
    }

    /**
     * La acción que sigue cuando no hay IA o su texto no sirve: sale del título anterior, sin ningún número nuevo.
     * Respeta los largos de {@link AccionManual}.
     */
    public AccionManual textoDeReserva(String tituloAnterior) {
        return textoSinIa.apply(tituloAnterior);
    }

    /** Por qué el cierre cambió una acción por otra, para el resumen de la semana. */
    public String motivo(String tituloAnterior, String tituloNuevo) {
        return porQueCambio.apply(tituloAnterior, tituloNuevo);
    }

    private static String recortar(String titulo) {
        if (titulo.length() <= AccionManual.LARGO_MAXIMO_TITULO) {
            return titulo;
        }
        return titulo.substring(0, AccionManual.LARGO_MAXIMO_TITULO - 1).strip() + "…";
    }
}
