package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cómo viene tu negocio: lo vendido, la mercadería, los gastos y lo que queda de los últimos 30 días, más el
 * resultado de las últimas seis semanas. Suma todos los cuadernos (sistemas) del propio negocio y usa el mismo
 * cálculo que el resumen en PDF.
 */
@RequiredArgsConstructor
public class ObtenerComoVieneElNegocio {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final Clock clock;

    public ObtenerComoVieneElNegocioResponse execute(ObtenerComoVieneElNegocioRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        List<DatosDeProyecto> datos = cargarDatosDeLosCuadernos(negocioId);
        return ObtenerComoVieneElNegocioResponse.de(
                ComoVieneElNegocio.armar(datos, ZonaNegocio.dia(OffsetDateTime.now(clock))));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<DatosDeProyecto> cargarDatosDeLosCuadernos(NegocioId negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId).stream()
                .map(datosDeProyectoService::cargar)
                .toList();
    }
}
