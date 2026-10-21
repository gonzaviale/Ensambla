package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA para el taller de Sami: una llamada con herramientas. Cada proveedor la
 * arma en {@link ProveedorIa#conHerramientas}; las fotos que adjunta la persona llegan en la petición.
 */
public interface ClienteModeloConHerramientas {

    /** Puede lanzar cualquier excepción no verificada; {@link TallerIaAdapter} la deja pasar al servicio. */
    RespuestaConHerramientas completar(PeticionConHerramientas peticion);
}
