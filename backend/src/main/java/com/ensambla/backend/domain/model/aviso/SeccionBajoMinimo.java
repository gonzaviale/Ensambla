package com.ensambla.backend.domain.model.aviso;

/** Cuántos productos quedaron en su mínimo o por debajo, sin haberse agotado. */
public class SeccionBajoMinimo extends SeccionDeStock {

    @Override
    protected boolean cuentaAgotados() {
        return false;
    }

    @Override
    protected String descripcion(long cantidad) {
        return cantidadConUnidad(cantidad, "producto por debajo del mínimo", "productos por debajo del mínimo");
    }
}
