package com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El pedido que la persona armó: a qué proveedor (opcional) y cuánto pidió de cada producto, después de ajustar las
 * cantidades que se le sugirieron.
 */
public record ArmarPedidoProveedorRequest(
        UsuarioId usuarioId,
        ProyectoId proyectoId,
        String proveedor,
        List<LineaDePedido> productos) {

    /** Cuánto pide de un producto del inventario. */
    public record LineaDePedido(RegistroId productoId, BigDecimal cantidad) {
    }

    /** Cantidad pedida por producto, en el orden en que llegaron; si un producto se repite vale la última. */
    public Map<RegistroId, BigDecimal> cantidadesPorProducto() {
        Map<RegistroId, BigDecimal> cantidades = new LinkedHashMap<>();
        productos.forEach(linea -> cantidades.put(linea.productoId(), linea.cantidad()));
        return cantidades;
    }
}
