package com.ensambla.backend.domain.model.objetivo;

/**
 * Lo que la IA lee para redactar la acción que renueva un mini-objetivo manual: la acción anterior, qué hacer con ella y
 * lo que la persona contó (sin datos personales).
 *
 * @param comentario lo que escribió la persona, sin correos ni números largos; vacío si no escribió nada
 */
public record PedidoRenovacion(
        AccionRenovacion accion, String titulo, String justificacion, HizoManual hizo, QuePaso quePaso,
        String comentario) {

    public static PedidoRenovacion de(MiniObjetivo mini, RespuestaMiniObjetivo respuesta) {
        return new PedidoRenovacion(
                AccionRenovacion.de(respuesta.hizo(), respuesta.quePaso()),
                mini.getTitulo(),
                mini.getJustificacion(),
                respuesta.hizo(),
                respuesta.quePaso(),
                respuesta.comentario() == null ? null : TextoSeguro.de(respuesta.comentario()));
    }
}
