package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA. Para sumar un proveedor real alcanza con implementar esta interfaz
 * (llamada HTTP, autenticación, salida estructurada con {@link PeticionModeloVision#esquemaJson()}) y registrarla
 * como bean cuando {@code ensambla.ia.proveedor} tenga su nombre. Ver {@link IaConfig}.
 */
public interface ClienteModeloVision {

    /** Puede lanzar cualquier excepción no verificada; el adapter la traduce a un error de lectura. */
    RespuestaModeloVision completar(PeticionModeloVision peticion);
}
