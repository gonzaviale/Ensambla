package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.whatsapp.CoincidenciaDeFilas;
import com.ensambla.backend.domain.model.zona.NombreDeProducto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca en el stock del comercio lo que pidió el cliente, aunque lo haya escrito como pueda. Si es una cosa clara
 * («yerba playadito») agrega ese producto; si es vaga («aceite») elige la primera opción razonable y deja las demás
 * como alternativas para cambiar de marca; si el comercio no lo tiene, lo avisa y recomienda un producto parecido.
 */
public final class BuscadorDeProductos {

    /** Cuántas alternativas se le ofrecen como máximo por producto. */
    public static final int MAX_ALTERNATIVAS = 4;

    private BuscadorDeProductos() {
    }

    public static ArmadoDeLista armar(List<LineaPedida> pedidas, List<ProductoEnStock> catalogo) {
        List<String> nombres = catalogo.stream().map(ProductoEnStock::nombre).toList();
        Map<RegistroId, LineaDeCompra> lineas = new LinkedHashMap<>();
        List<ProductoFaltante> faltantes = new ArrayList<>();
        for (LineaPedida pedida : pedidas) {
            List<ProductoEnStock> parecidos = CoincidenciaDeFilas.coincidencias(pedida.producto(), nombres).stream()
                    .map(catalogo::get)
                    .toList();
            if (parecidos.isEmpty()) {
                faltantes.add(new ProductoFaltante(pedida.producto(), similar(pedida.producto(), catalogo)));
                continue;
            }
            ProductoEnStock elegido = parecidos.getFirst();
            List<ProductoEnStock> alternativas = parecidos.stream().skip(1).limit(MAX_ALTERNATIVAS).toList();
            lineas.merge(elegido.id(), LineaDeCompra.de(elegido, pedida.cantidad(), alternativas),
                    LineaDeCompra::sumar);
        }
        return new ArmadoDeLista(List.copyOf(lineas.values()), faltantes);
    }

    /** El primer producto del comercio que es del mismo tipo que lo pedido (otra marca, otro tamaño). */
    private static ProductoEnStock similar(String pedido, List<ProductoEnStock> catalogo) {
        return catalogo.stream()
                .filter(producto -> NombreDeProducto.esElMismo(pedido, producto.nombre()))
                .findFirst()
                .orElse(null);
    }
}
