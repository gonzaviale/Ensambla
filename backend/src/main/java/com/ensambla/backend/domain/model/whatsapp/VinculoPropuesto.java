package com.ensambla.backend.domain.model.whatsapp;

/**
 * La fila de otra tabla que nombra un campo que se vincula (el Producto o el Cliente de una venta).
 *
 * @param nombre     cómo se llama: el de la fila encontrada o, si es nueva, lo que escribió la persona
 * @param registroId id de la fila encontrada; null si no existe y se crea al confirmar
 * @param bloqueId   id de la tabla a la que apunta el campo
 * @param tabla      nombre de esa tabla, para mostrarlo en el resumen
 */
public record VinculoPropuesto(String nombre, String registroId, String bloqueId, String tabla) {

    public boolean esNuevo() {
        return registroId == null;
    }
}
