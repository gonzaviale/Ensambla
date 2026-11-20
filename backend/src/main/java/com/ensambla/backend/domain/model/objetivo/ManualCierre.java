package com.ensambla.backend.domain.model.objetivo;

/**
 * Cómo terminó la semana un mini-objetivo manual: lo que la persona respondió o que no respondió.
 *
 * @param hizo    vacío si no respondió
 * @param quePaso vacío si no respondió
 */
public record ManualCierre(MiniObjetivoId miniObjetivoId, String titulo, HizoManual hizo, QuePaso quePaso) {

    public static ManualCierre respondido(MiniObjetivo mini, RespuestaMiniObjetivo respuesta) {
        return new ManualCierre(mini.getId(), mini.getTitulo(), respuesta.hizo(), respuesta.quePaso());
    }

    public static ManualCierre sinRespuesta(MiniObjetivo mini) {
        return new ManualCierre(mini.getId(), mini.getTitulo(), null, null);
    }

    public boolean respondido() {
        return hizo != null;
    }
}
