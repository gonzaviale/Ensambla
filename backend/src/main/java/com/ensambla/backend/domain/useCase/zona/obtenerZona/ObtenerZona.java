package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.zona.ProductosPropiosService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerZona {

    private final NegocioRepository negocioRepository;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final ControlPlanService controlPlanService;

    private final ProductosPropiosService productosPropiosService;

    private final Clock clock;

    public ZonaItem execute(ObtenerZonaRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        AnalisisZona analisis = zonaRepository.findByIdSiTiene(negocio.getZonaId())
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
        ZonaItem zonaItem = ZonaItem.de(productosPropiosService.conLosPropiosPrimero(negocio.getId(), analisis));
        AccesoContenidoItem acceso = controlPlanService.seleccionar(
                negocio.getId(), TipoContenidoPlan.ZONA, zonaItem.clavesDeAcceso(), OffsetDateTime.now(clock));
        return zonaItem.filtrada(acceso);
    }
}
