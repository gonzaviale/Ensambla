package com.ensambla.backend.domain.useCase.metrica.listarSugeridas;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.MetricasDelCuaderno;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerCuaderno(negocio.getId(), request.proyectoId());
        MetricasDelCuaderno guardadas = metricasDelCuaderno(request.proyectoId(), negocio.getId());
        DatosDeProyecto datos = leerElCuaderno(proyecto, negocio.getId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        return sugeridasDelRubro(negocio.getRubro()).stream()
                .filter(sugerida -> !guardadas.incluye(sugerida))
                .map(sugerida -> conLoQueDariaHoy(sugerida, datos, hoy))
                .toList();
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerCuaderno(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private MetricasDelCuaderno metricasDelCuaderno(ProyectoId proyectoId, NegocioId negocioId) {
        return new MetricasDelCuaderno(metricaRepository.findAllByProyectoIdAndNegocioId(proyectoId, negocioId));
    }

    private DatosDeProyecto leerElCuaderno(Proyecto proyecto, NegocioId negocioId) {
        return datosDeProyectoService.cargar(proyecto, negocioId);
    }

    private List<DefinicionMetrica> sugeridasDelRubro(RubroNegocio rubro) {
        return catalogoMetricas.paraRubro(rubro);
    }

    private SugeridaItem conLoQueDariaHoy(DefinicionMetrica sugerida, DatosDeProyecto datos, LocalDate hoy) {
        return SugeridaItem.de(sugerida, motorMetricas.calcular(sugerida, datos, hoy));
    }
}
