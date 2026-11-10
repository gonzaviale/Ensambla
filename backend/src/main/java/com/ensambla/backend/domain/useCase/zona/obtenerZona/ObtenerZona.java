package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.VistaZona;
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
import java.util.List;

/**
 * La zona del negocio para una pantalla de Mi zona. Cada pantalla ordena los productos a su manera y gasta su propio
 * cupo gratuito (ver {@link VistaZona}), así que una no puede dejar sin lugar a otra.
 */
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
        VistaZona vista = request.vista();
        ZonaItem zonaItem = ZonaItem.de(vista.ordenar(analisis, nombresPropios(vista, negocio, analisis)));
        AccesoContenidoItem acceso = controlPlanService.seleccionar(
                negocio.getId(), vista.tipoDeContenido(), zonaItem.clavesDeAcceso(), OffsetDateTime.now(clock));
        return zonaItem.filtrada(acceso);
    }

    /** El inventario sólo se consulta si la pantalla lo usa para ordenar y hay productos que ordenar. */
    private List<String> nombresPropios(VistaZona vista, Negocio negocio, AnalisisZona analisis) {
        return vista.usaInventario() && !analisis.productos().isEmpty()
                ? productosPropiosService.nombresDelInventario(negocio.getId())
                : List.of();
    }
}
