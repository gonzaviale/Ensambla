package com.ensambla.backend.domain.model.objetivo;

/** La respuesta rápida a «qué pasó» con un mini-objetivo manual. */
public enum QuePaso {
    FUNCIONO,
    NO_TUVE_TIEMPO,
    NO_FUNCIONO,
    OTRA;

    /** Si la acción no dio resultado y conviene cambiarla por otra. */
    boolean cambiaLaAccion() {
        return this == NO_FUNCIONO;
    }
}
