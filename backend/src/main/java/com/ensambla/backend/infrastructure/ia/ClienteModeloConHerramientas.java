package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA para el taller de Sami: una llamada con herramientas. Para sumar un
 * proveedor alcanza con implementar esta interfaz y registrarlo en {@link SamiConfig}.
 */
public interface ClienteModeloConHerramientas {

    /** Puede lanzar cualquier excepción no verificada; {@link TallerIaAdapter} la deja pasar al servicio. */
    RespuestaConHerramientas completar(PeticionConHerramientas peticion);
}
