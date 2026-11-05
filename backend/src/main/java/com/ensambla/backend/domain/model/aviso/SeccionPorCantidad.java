package com.ensambla.backend.domain.model.aviso;

import java.util.List;
import java.util.Optional;

/**
 * Las partes del resumen que cuentan cuántos avisos pendientes tienen algo en común. La base fija el flujo (contar y,
 * si hay alguno, escribir la línea) y cada parte dice cuáles cuenta y cómo lo cuenta.
 */
public abstract class SeccionPorCantidad implements SeccionResumen {

    @Override
    public final Optional<String> texto(List<Aviso> pendientes) {
        long cantidad = pendientes.stream().filter(this::corresponde).count();
        return cantidad == 0 ? Optional.empty() : Optional.of(descripcion(cantidad));
    }

    /** Si el aviso es de los que esta parte cuenta. */
    protected abstract boolean corresponde(Aviso aviso);

    /** La línea para esa cantidad, que siempre es mayor a cero. */
    protected abstract String descripcion(long cantidad);

    protected static String cantidadConUnidad(long cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }
}
