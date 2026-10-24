package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.common.Huella;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class AccesoZonaService {

    private final AccesoContenidoService accesoContenidoService;

    public ZonaItem filtrar(NegocioId negocioId, ZonaItem zona, OffsetDateTime ahora) {
        List<String> claves = zona.productos().stream().map(producto -> clave(zona, producto)).toList();
        AccesoContenidoItem acceso = accesoContenidoService.seleccionar(negocioId, TipoContenidoPlan.ZONA, claves, ahora);
        return new ZonaItem(zona.nombre(), zona.comercios(), zona.productos().stream()
                .filter(producto -> acceso.autorizadas().contains(clave(zona, producto))).toList(), acceso);
    }

    private String clave(ZonaItem zona, ZonaItem.ProductoItem producto) {
        return "zona:" + Huella.de(zona.nombre() + "|" + producto.nombre());
    }
}
