package com.ensambla.backend.infrastructure.ia;

/**
 * Punto de conexión con el proveedor de IA para Sami (sólo texto). Para sumar un proveedor alcanza con implementar
 * esta interfaz y registrarlo en {@link SamiConfig} según {@code ensambla.sami.ia.proveedor}.
 */
public interface ClienteModeloTexto {

    /** Puede lanzar cualquier excepción no verificada; {@link SamiIaAdapter} decide qué hacer con ella. */
    RespuestaModeloTexto completar(PeticionModeloTexto peticion);
}
