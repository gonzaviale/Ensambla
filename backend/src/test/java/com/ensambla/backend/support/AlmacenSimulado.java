package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Un almacén con ventas de todos los días entre dos fechas, para simular varias semanas de un objetivo. Cada día vende
 * las unidades de base de cada producto multiplicadas por el factor de ese día (redondeado); con factor 1 deja
 * $22.200 de ganancia por día, $666.000 en 30 días.
 */
public final class AlmacenSimulado {

    private static final List<Producto> PRODUCTOS = List.of(
            new Producto("Yerba Playadito 1 kg", 3600, 2700, 10),
            new Producto("Coca-Cola 2,25 L", 2700, 1950, 6),
            new Producto("Galletitas Express", 1200, 800, 8),
            new Producto("Aceite Natura 900 ml", 4000, 3000, 3),
            new Producto("Fernet Branca 750 ml", 9000, 6500, 1));

    private AlmacenSimulado() {
    }

    /** Guarda el almacén con una venta por producto y día, de {@code desde} a {@code hasta} inclusive. */
    public static void guardar(
            SistemaRepository sistemaRepository, UUID negocioId, LocalDate desde, LocalDate hasta,
            ToDoubleFunction<LocalDate> factorDelDia) {
        String sistemaId = UUID.randomUUID().toString();
        String inventarioId = frontId();
        List<String> productoIds = PRODUCTOS.stream().map(producto -> frontId()).toList();
        List<Map<String, Object>> inventario = new ArrayList<>();
        for (int indice = 0; indice < PRODUCTOS.size(); indice++) {
            Producto producto = PRODUCTOS.get(indice);
            inventario.add(fila(productoIds.get(indice), "name", producto.nombre(), "quantity", 500,
                    "price", producto.precio(), "cost", producto.costo()));
        }
        List<Map<String, Object>> ventas = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            double factor = factorDelDia.applyAsDouble(dia);
            for (int indice = 0; indice < PRODUCTOS.size(); indice++) {
                Producto producto = PRODUCTOS.get(indice);
                long unidades = Math.round(producto.unidadesPorDia() * factor);
                if (unidades > 0) {
                    ventas.add(fila(frontId(), "product", productoIds.get(indice), "quantity", unidades,
                            "price", producto.precio(), "date", dia.toString()));
                }
            }
        }
        BloqueItem bloqueInventario = new BloqueItem(
                inventarioId, "inventory", "Inventario", "", "orange", "full",
                List.of(
                        campo("name", "Producto", "text"),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("cost", "Costo", "currency")),
                inventario, "", false, null);
        BloqueItem bloqueVentas = new BloqueItem(
                frontId(), "sales", "Ventas", "", "yellow", "full",
                List.of(
                        new CampoItem("product", "Producto", "relation", true, null, null, inventarioId),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("date", "Fecha", "date")),
                ventas, "", false, null);
        SistemaItem item = new SistemaItem(
                sistemaId, "Almacén simulado", OffsetDateTime.now().toString(), null,
                List.of(new SeccionItem(frontId(), "Vista general", List.of(bloqueInventario, bloqueVentas))),
                "shop", new OpcionesItem(true, false), "mosaic", false);

        sistemaRepository.guardar(item.aSistema(NegocioId.de(negocioId), sistemaId, OffsetDateTime.now()));
    }

    private static CampoItem campo(String clave, String nombre, String tipo) {
        return new CampoItem(clave, nombre, tipo, true, null, null, null);
    }

    private static Map<String, Object> fila(String id, Object... clavesYValores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", id);
        for (int indice = 0; indice < clavesYValores.length; indice += 2) {
            fila.put((String) clavesYValores[indice], clavesYValores[indice + 1]);
        }
        return fila;
    }

    private static String frontId() {
        return "e" + UUID.randomUUID();
    }

    private record Producto(String nombre, int precio, int costo, int unidadesPorDia) {
    }
}
