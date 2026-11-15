package com.ensambla.backend.domain.model.aviso;

/** Quien sabe cambiar datos del negocio: cada acción que los modifica le pide que la aplique. */
public interface AplicadorDeAcciones {

    void editar(AccionEditarRegistro accion);

    void crear(AccionCrearRegistro accion);
}
