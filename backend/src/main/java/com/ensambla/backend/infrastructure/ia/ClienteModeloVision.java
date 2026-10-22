package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA para leer fotos. Cada proveedor la arma en {@link ProveedorIa#vision}
 * (llamada, autenticación, el esquema de {@link PeticionModeloVision#esquemaJson()}). Ver {@link IaConfig}.
 */
public interface ClienteModeloVision {

    /** Puede lanzar cualquier excepción no verificada; el adapter la traduce a un error de lectura. */
    RespuestaModeloVision completar(PeticionModeloVision peticion);
}
