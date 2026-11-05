package com.ensambla.backend.domain.model.aviso;

/** Cuántos productos se agotaron. */
public class SeccionAgotados extends SeccionDeStock {

    @Override
    protected boolean cuentaAgotados() {
        return true;
    }

    @Override
    protected String descripcion(long cantidad) {
        return cantidadConUnidad(cantidad, "producto agotado", "productos agotados");
    }
}
