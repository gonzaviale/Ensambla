package com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;

import java.util.Optional;

/** El PDF del pedido al proveedor y el nombre con el que se descarga. */
public record ArmarPedidoProveedorResponse(byte[] contenido, String nombreArchivo) {

    private static final String PREFIJO = "pedido";

    /** El nombre es {@code pedido-<proveedor o negocio>-<aaaammdd>.pdf}, sin tildes ni espacios. */
    public static ArmarPedidoProveedorResponse de(byte[] contenido, PedidoReposicion pedido) {
        String destinatario = Optional.ofNullable(pedido.proveedor()).orElse(pedido.negocio());
        String nombre = TextoNormalizado.slug(destinatario);
        String conPrefijo = nombre.isEmpty() ? PREFIJO : PREFIJO + "-" + nombre;
        String fecha = pedido.fecha().toString().replace("-", "");
        return new ArmarPedidoProveedorResponse(contenido, conPrefijo + "-" + fecha + ".pdf");
    }
}
