package com.ensambla.backend.domain.model.aviso;

/** Cuántos productos se venden en la zona y el negocio todavía no ofrece. */
public class SeccionLoQuePideLaZona extends SeccionPorCantidad {

    @Override
    protected boolean corresponde(Aviso aviso) {
        return aviso.getTipo() == TipoAviso.CONSUMO_ZONA;
    }

    @Override
    protected String descripcion(long cantidad) {
        return cantidadConUnidad(cantidad, "producto que se vende cerca tuyo y todavía no tenés",
                "productos que se venden cerca tuyo y todavía no tenés");
    }
}
