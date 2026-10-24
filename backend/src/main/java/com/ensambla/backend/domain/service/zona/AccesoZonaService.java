package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AccesoZonaService {

    private final AccesoContenidoService accesoContenidoService;

    public ZonaItem filtrar(UUID negocioId, ZonaItem zona, OffsetDateTime ahora) {
        List<String> claves = zona.productos().stream().map(producto -> clave(zona, producto)).toList();
        AccesoContenidoItem acceso = accesoContenidoService.seleccionar(negocioId, TipoContenidoPlan.ZONA, claves, ahora);
        return new ZonaItem(zona.nombre(), zona.comercios(), zona.productos().stream()
                .filter(producto -> acceso.autorizadas().contains(clave(zona, producto))).toList(), acceso);
    }

    private String clave(ZonaItem zona, ZonaItem.ProductoItem producto) {
        return "zona:" + UUID.nameUUIDFromBytes(
                (zona.nombre() + "|" + producto.nombre()).getBytes(StandardCharsets.UTF_8));
    }
}
