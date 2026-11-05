package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.promocion.ContextoPromocion;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.model.promocion.ReglaPromocion;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Qué conviene promocionar: cada regla mira lo que vende el negocio y propone una idea, o ninguna. Se devuelven las que
 * aplican junto con los canales donde se pueden compartir.
 */
@RequiredArgsConstructor
public class SugerirPromociones {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MetricasVentas metricasVentas;

    private final List<ReglaPromocion> reglas;

    private final CatalogoDeCanales catalogoDeCanales;

    private final Clock clock;

    public SugerirPromocionesResponse execute(SugerirPromocionesRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        ContextoPromocion contexto = contextoDe(negocioId, request);
        return SugerirPromocionesResponse.de(promocionesQueAplican(contexto), catalogoDeCanales.todos());
    }

    private ContextoPromocion contextoDe(NegocioId negocioId, SugerirPromocionesRequest request) {
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocioId);
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        return ContextoPromocion.de(datos, metricasVentas.calcular(datos.ventas(), hoy));
    }

    private List<Promocion> promocionesQueAplican(ContextoPromocion contexto) {
        return reglas.stream()
                .map(regla -> regla.evaluar(contexto))
                .flatMap(Optional::stream)
                .toList();
    }
}
