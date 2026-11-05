package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;

/** Escribe el pedido de reposición como un documento PDF para mandárselo al proveedor. */
public interface GeneradorPedidoPdfPort {

    byte[] generar(PedidoReposicion pedido);
}
