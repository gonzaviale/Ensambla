package com.ensambla.backend.domain.model.bloque;

import java.util.List;

/** Las columnas fijas de cada tipo de sección. La tabla completa y su porqué están en docs/estructura-fija.md. */
final class ColumnasFijas {

    private static final List<String> CATEGORIAS_DE_GASTO = List.of("Mercadería", "Servicios", "Alquiler", "Otros");

    private ColumnasFijas() {
    }

    static List<ColumnaFija> ninguna() {
        return List.of();
    }

    static List<ColumnaFija> deStock() {
        return List.of(
                ColumnaFija.texto("name", "Producto", "identificar cada producto").siempreObligatoria(),
                ColumnaFija.numero("quantity", "Cantidad", "controlar el stock y descontarlo en Caja").siempreObligatoria(),
                ColumnaFija.moneda("price", "Precio", "cobrar en Caja y calcular la ganancia"),
                ColumnaFija.moneda("cost", "Costo", "calcular la ganancia de Objetivos y Mi negocio"),
                ColumnaFija.numero("minimum", "Stock mínimo", "avisarte cuando queda poco stock"),
                ColumnaFija.texto("barcode", "Código de barras", "escanear productos en Caja y en el stock"),
                ColumnaFija.lista("saleMode", "Venta por", "elegir si Caja vende por unidad o por peso",
                        List.of("unidad", "peso"), List.of("unidad", "peso")),
                ColumnaFija.siNo("favorite", "Favorito en caja", "mostrar tus favoritos en Caja"),
                ColumnaFija.siNo("stockControl", "Controlar stock", "decidir si Caja descuenta el stock"));
    }

    static List<ColumnaFija> deVentas() {
        return List.of(
                ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "saber qué producto se vendió")
                        .siempreObligatoria(),
                ColumnaFija.numero("quantity", "Cantidad", "descontar el stock y medir las ventas").siempreObligatoria(),
                ColumnaFija.moneda("price", "Precio", "calcular lo vendido en Caja y Objetivos"),
                ColumnaFija.fecha("date", "Fecha", "ubicar cada venta en el día y en el período"),
                ColumnaFija.textoOVinculo("customer", "Cliente", TipoBloque.CLIENTES, "registrar a quién se fió"),
                ColumnaFija.siNo("credit", "Fiado", "calcular lo que te deben"));
    }

    static List<ColumnaFija> deClientes() {
        return List.of(ColumnaFija.texto("name", "Nombre", "identificar a cada cliente en Caja").siempreObligatoria());
    }

    static List<ColumnaFija> deGastos() {
        return List.of(
                ColumnaFija.texto("name", "Concepto", "identificar cada gasto").siempreObligatoria(),
                ColumnaFija.fecha("date", "Fecha", "ubicar cada gasto en el período"),
                ColumnaFija.moneda("total", "Importe", "calcular tus costos y tu ganancia").siempreObligatoria(),
                ColumnaFija.lista("category", "Categoría", "separar la mercadería de los otros gastos",
                        CATEGORIAS_DE_GASTO, List.of("Mercadería")));
    }

    static List<ColumnaFija> deMovimientos() {
        return List.of(
                ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "mover el stock del producto")
                        .siempreObligatoria(),
                ColumnaFija.numero("quantity", "Cantidad", "sumar o restar unidades de stock").siempreObligatoria());
    }
}
