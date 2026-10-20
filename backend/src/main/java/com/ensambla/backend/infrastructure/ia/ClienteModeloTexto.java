package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA para Sami (sólo texto). Cada proveedor la arma en
 * {@link ProveedorIa#texto}.
 */
public interface ClienteModeloTexto {

    /** Puede lanzar cualquier excepción no verificada; {@link SamiIaAdapter} decide qué hacer con ella. */
    RespuestaModeloTexto completar(PeticionModeloTexto peticion);
}
