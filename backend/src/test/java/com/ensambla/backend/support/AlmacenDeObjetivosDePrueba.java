package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda en la base un almacén con ventas de hoy para probar de punta a punta lo que se calcula con los datos del
 * negocio: la yerba (precio $3.600, costo $2.700, 60 vendidas), la Coca-Cola (5 vendidas) y el azúcar, parado.
 */
public final class AlmacenDeObjetivosDePrueba {

    private AlmacenDeObjetivosDePrueba() {
    }

    public static void guardar(SistemaRepository sistemaRepository, UUID negocioId) {
        String sistemaId = UUID.randomUUID().toString();
        String seccionId = frontId();
        String inventarioId = frontId();
        String ventasId = frontId();
        String yerbaId = frontId();
        String azucarId = frontId();
        String cocaColaId = frontId();
        BloqueItem inventario = new BloqueItem(
                inventarioId, "inventory", "Inventario", "", "orange", "full",
                List.of(
                        campo("name", "Producto", "text"),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("cost", "Costo", "currency")),
                List.of(
                        fila(yerbaId, "name", "Yerba Playadito 1 kg", "quantity", 4, "price", 3600, "cost", 2700),
                        fila(azucarId, "name", "Azúcar Ledesma 1 kg", "quantity", 25, "price", 1500, "cost", 900),
                        fila(cocaColaId, "name", "Coca-Cola 2,25 L", "quantity", 18, "price", 2700, "cost", 1950)),
                "", false, null);
        BloqueItem ventas = new BloqueItem(
                ventasId, "sales", "Ventas", "", "yellow", "full",
                List.of(
                        new CampoItem("product", "Producto", "relation", true, null, null, inventarioId),
                        campo("quantity", "Cantidad", "number"),
                        campo("price", "Precio", "currency"),
                        campo("date", "Fecha", "date")),
                List.of(
                        fila(frontId(), "product", yerbaId, "quantity", 60, "price", 3600, "date", hoyEnElNegocio()),
                        fila(frontId(), "product", cocaColaId, "quantity", 5, "price", 2700, "date",
                                hoyEnElNegocio())),
                "", false, null);
        SistemaItem item = new SistemaItem(
                sistemaId, "Almacén de objetivos", OffsetDateTime.now().toString(), null,
                List.of(new SeccionItem(seccionId, "Vista general", List.of(inventario, ventas))),
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

    /** El día del negocio (Argentina), no el del servidor: pasadas las 21:00 locales el UTC ya es el día siguiente. */
    private static String hoyEnElNegocio() {
        return ZonaNegocio.dia(OffsetDateTime.now()).toString();
    }

    private static String frontId() {
        return "e" + UUID.randomUUID();
    }
}
