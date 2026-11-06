package com.ensambla.backend.domain.useCase.reporte.obtenerMetricas;

import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.VentaDelDia;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Las métricas detalladas de un sistema para la pantalla «Mis métricas»: hoy, la semana o el mes. Usa las mismas
 * cuentas que el resumen en PDF, pero devuelve los números para dibujarlos y no exige el plan de reportes. Sólo mira
 * el sistema del propio negocio.
 */
@RequiredArgsConstructor
public class ObtenerMetricas {

    private static final int DIAS_DE_LA_QUINCENA = 14;

    private static final int DIAS_DE_UNA_SEMANA = 7;

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final AvisoRepository avisoRepository;

    private final Clock clock;

    public ObtenerMetricasResponse execute(ObtenerMetricasRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocio.getId());
        List<VentaProducto> ventas = ventasHastaHoy(datos, hoy);
        PeriodoResumen periodo = request.periodo().hasta(hoy);
        ResumenNegocio resumen = ResumenNegocio.armar(negocio.getNombre(), datos, zonaDe(negocio.getId()),
                avisosDe(negocio.getId(), proyecto), periodo, hoy);
        return ObtenerMetricasResponse.de(
                request.periodo(),
                !ventas.isEmpty(),
                resumen,
                resumen(negocio, datos, new PeriodoResumen(hoy.minusDays(DIAS_DE_LA_QUINCENA - 1L), hoy), hoy),
                ingresosAnteriores(negocio, datos, request.periodo(), resumen, hoy),
                ventas.stream()
                        .filter(venta -> periodo.incluye(venta.fecha()) && venta.precio() == null)
                        .count(),
                VentaDelDia.serie(ventas, periodo.desde(), periodo.hasta()),
                ventas.stream().anyMatch(venta -> !venta.fecha().isAfter(hoy.minusDays(DIAS_DE_LA_QUINCENA - 1L))));
    }

    /** Los agregados de la zona del negocio, o una zona vacía si todavía no tiene una asignada. */
    private AnalisisZona zonaDe(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaRepository::findById)
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }

    private List<Aviso> avisosDe(NegocioId negocioId, Proyecto proyecto) {
        return avisoRepository.findByNegocioId(negocioId).stream()
                .filter(aviso -> proyecto.getId().equals(aviso.getProyectoId()))
                .toList();
    }

    /** Hoy se compara con el mismo día de la semana pasada; la semana y el mes, con los días anteriores. */
    private static BigDecimal ingresosAnteriores(Negocio negocio, DatosDeProyecto datos, PeriodoMetricas periodo,
                                                 ResumenNegocio resumen, LocalDate hoy) {
        if (periodo != PeriodoMetricas.HOY) {
            return resumen.ventas().ingresosAnteriores();
        }
        LocalDate semanaPasada = hoy.minusDays(DIAS_DE_UNA_SEMANA);
        return resumen(negocio, datos, new PeriodoResumen(semanaPasada, semanaPasada), hoy).ventas().ingresos();
    }

    private static List<VentaProducto> ventasHastaHoy(DatosDeProyecto datos, LocalDate hoy) {
        return datos.ventas().stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()))
                .filter(venta -> !venta.fecha().isAfter(hoy))
                .toList();
    }

    private static ResumenNegocio resumen(Negocio negocio, DatosDeProyecto datos, PeriodoResumen periodo,
                                          LocalDate hoy) {
        return ResumenNegocio.armar(negocio.getNombre(), datos, AnalisisZona.sinZona(), List.of(), periodo, hoy);
    }
}
