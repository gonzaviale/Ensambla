package com.ensambla.backend.domain.useCase.metrica.listarSugeridas;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Las métricas que el catálogo le sugiere al rubro del negocio y el cuaderno todavía no guardó, cada una con lo que
 * daría hoy para que la persona vea el número antes de agregarla.
 */
@RequiredArgsConstructor
public class ListarSugeridas {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final MetricaRepository metricaRepository;

    private final CatalogoMetricas catalogoMetricas;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MotorMetricas motorMetricas;

    private final Clock clock;

    public List<SugeridaItem> execute(ListarSugeridasRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        List<DefinicionMetrica> guardadas =
                metricaRepository.findAllByProyectoIdAndNegocioId(request.proyectoId(), negocio.getId()).stream()
                        .map(Metrica::getDefinicion)
                        .toList();
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocio.getId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        return catalogoMetricas.paraRubro(negocio.getRubro()).stream()
                .filter(sugerida -> guardadas.stream().noneMatch(sugerida::esIgualA))
                .map(sugerida -> SugeridaItem.de(sugerida, motorMetricas.calcular(sugerida, datos, hoy)))
                .toList();
    }
}
