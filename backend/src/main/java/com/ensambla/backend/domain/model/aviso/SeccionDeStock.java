package com.ensambla.backend.domain.model.aviso;

/** Las partes del resumen sobre el stock: los avisos de stock se reparten entre agotados y bajo el mínimo. */
public abstract class SeccionDeStock extends SeccionPorCantidad {

    private static final String CLAVE_CANTIDAD = "cantidad";

    @Override
    protected final boolean corresponde(Aviso aviso) {
        return aviso.getTipo() == TipoAviso.STOCK && estaAgotado(aviso) == cuentaAgotados();
    }

    /** {@code true} si esta parte cuenta los productos agotados; {@code false} si cuenta los que solo están bajos. */
    protected abstract boolean cuentaAgotados();

    private static boolean estaAgotado(Aviso aviso) {
        return aviso.getDatos().get(CLAVE_CANTIDAD) instanceof Number cantidad && cantidad.doubleValue() <= 0;
    }
}
