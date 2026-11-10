package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cómo viene tu negocio: lo vendido, la mercadería, los gastos y lo que queda de los últimos 30 días, más el
 * resultado de las últimas seis semanas. Suma todos los sistemas del propio negocio y usa el mismo cálculo que el
 * resumen en PDF.
 */
@RequiredArgsConstructor
public class ObtenerComoVieneElNegocio {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final Clock clock;

    public ObtenerComoVieneElNegocioResponse execute(ObtenerComoVieneElNegocioRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        List<DatosDeProyecto> datos = proyectoRepository.findAllByNegocioId(negocio.getId()).stream()
                .map(proyecto -> datosDeProyectoService.cargar(proyecto, negocio.getId()))
                .toList();
        return ObtenerComoVieneElNegocioResponse.de(
                ComoVieneElNegocio.armar(datos, ZonaNegocio.dia(OffsetDateTime.now(clock))));
    }
}
