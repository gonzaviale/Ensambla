package com.ensambla.backend.domain.model.objetivo;

/**
 * Cómo se renueva un mini-objetivo manual el domingo según lo que la persona respondió: si lo hizo y le funcionó sube un
 * paso; si lo hizo en parte se divide (queda lo que faltó, más chico); si no lo hizo por falta de tiempo o por otra
 * razón se simplifica; y si no funcionó, cambia por otra acción. Sin respuesta no se renueva.
 */
public enum AccionRenovacion {
    SUBE_UN_PASO(TipoReajuste.SUBIR_META),
    SE_DIVIDE(TipoReajuste.REEMPLAZAR),
    SE_SIMPLIFICA(TipoReajuste.REEMPLAZAR),
    CAMBIA(TipoReajuste.PROBAR_OTRO_CAMINO);

    private final TipoReajuste tipoReajuste;

    AccionRenovacion(TipoReajuste tipoReajuste) {
        this.tipoReajuste = tipoReajuste;
    }

    /** La clase de cambio que se anota en el resumen de la semana. */
    public TipoReajuste tipoReajuste() {
        return tipoReajuste;
    }

    public static AccionRenovacion de(HizoManual hizo, QuePaso quePaso) {
        return switch (hizo) {
            case SI -> quePaso == QuePaso.NO_FUNCIONO ? CAMBIA : SUBE_UN_PASO;
            case EN_PARTE -> SE_DIVIDE;
            case NO -> quePaso == QuePaso.NO_FUNCIONO ? CAMBIA : SE_SIMPLIFICA;
        };
    }

    /**
     * La acción que sigue cuando no hay IA o su texto no sirve: sale del título anterior, sin ningún número nuevo.
     * Respeta los largos de {@link AccionManual}.
     */
    public AccionManual textoDeReserva(String tituloAnterior) {
        return switch (this) {
            case SUBE_UN_PASO -> new AccionManual(
                    recortar("Un paso más: " + tituloAnterior),
                    "Te salió bien «" + tituloAnterior + "». Repetilo esta semana y sumale algo más ambicioso.");
            case SE_DIVIDE -> new AccionManual(
                    recortar("Lo que faltó: " + tituloAnterior),
                    "Hiciste una parte de «" + tituloAnterior
                            + "». Esta semana terminá lo que faltó, en un paso más chico.");
            case SE_SIMPLIFICA -> new AccionManual(
                    recortar("Versión corta: " + tituloAnterior),
                    "No llegaste con «" + tituloAnterior + "». Probá una versión de 10 minutos de lo mismo.");
            case CAMBIA -> new AccionManual(
                    "Probá otra acción para tu objetivo",
                    "«" + tituloAnterior + "» no dio resultado. Elegí una acción distinta hacia el mismo objetivo y "
                            + "contale a Sami cuál fue.");
        };
    }

    /** Por qué el cierre cambió una acción por otra, para el resumen de la semana. */
    public String motivo(String tituloAnterior, String tituloNuevo) {
        return switch (this) {
            case SUBE_UN_PASO -> "Hiciste «" + tituloAnterior + "» y te funcionó: seguimos con un paso más, «"
                    + tituloNuevo + "».";
            case SE_DIVIDE -> "Hiciste «" + tituloAnterior + "» en parte: nos quedamos con lo que faltó, «"
                    + tituloNuevo + "».";
            case SE_SIMPLIFICA -> "No llegaste con «" + tituloAnterior + "»: lo hacemos más simple, «"
                    + tituloNuevo + "».";
            case CAMBIA -> "«" + tituloAnterior + "» no dio resultado: probamos otra acción, «" + tituloNuevo + "».";
        };
    }

    private static String recortar(String titulo) {
        if (titulo.length() <= AccionManual.LARGO_MAXIMO_TITULO) {
            return titulo;
        }
        return titulo.substring(0, AccionManual.LARGO_MAXIMO_TITULO - 1).strip() + "…";
    }
}
