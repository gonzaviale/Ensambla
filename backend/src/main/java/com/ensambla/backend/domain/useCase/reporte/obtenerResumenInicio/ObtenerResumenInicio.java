package com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio;

import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse.UltimoDiaConVentas;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Las métricas del inicio de un sistema: lo que vendió hoy frente al mismo día de la semana pasada, lo que más salió,
 * lo que falta y lo que está parado. Usa el mismo cálculo que el resumen en PDF, pero devuelve los números en lugar de
 * un documento y no exige el plan de reportes. Sólo mira el sistema del propio negocio.
 */
@RequiredArgsConstructor
public class ObtenerResumenInicio {

    private static final int DIAS_DEL_MES = 30;

    private static final int DIAS_DE_LA_QUINCENA = 14;

    private static final int DIAS_DE_UNA_SEMANA = 7;

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final Clock clock;

    public ObtenerResumenInicioResponse execute(ObtenerResumenInicioRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocio.getId());
        List<VentaProducto> ventas = ventasHastaHoy(datos, hoy);
        UltimoDiaConVentas ultimoDia = ultimoDiaConVentas(negocio, datos, ventas, hoy);
        return ObtenerResumenInicioResponse.de(
                hoy,
                !ventas.isEmpty(),
                resumen(negocio, datos, hoy.minusDays(DIAS_DEL_MES - 1L), hoy, hoy),
                resumen(negocio, datos, hoy.minusDays(DIAS_DE_LA_QUINCENA - 1L), hoy, hoy),
                resumen(negocio, datos, hoy, hoy, hoy),
                resumen(negocio, datos, hoy.minusDays(DIAS_DE_UNA_SEMANA), hoy.minusDays(DIAS_DE_UNA_SEMANA), hoy)
                        .ventas().ingresos(),
                ventas.stream().filter(venta -> venta.fecha().equals(hoy) && venta.precio() == null).count(),
                ultimoDia,
                tieneQuincenaDeVentas(ventas, hoy));
    }

    private static List<VentaProducto> ventasHastaHoy(DatosDeProyecto datos, LocalDate hoy) {
        return datos.ventas().stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()))
                .filter(venta -> !venta.fecha().isAfter(hoy))
                .toList();
    }

    private UltimoDiaConVentas ultimoDiaConVentas(Negocio negocio, DatosDeProyecto datos, List<VentaProducto> ventas,
                                                  LocalDate hoy) {
        return ventas.stream()
                .map(VentaProducto::fecha)
                .max(Comparator.naturalOrder())
                .map(dia -> new UltimoDiaConVentas(dia, resumen(negocio, datos, dia, dia, hoy).ventas().ingresos()))
                .orElse(null);
    }

    /** Un producto "no se vendió en dos semanas" sólo si el sistema ya tiene ventas anotadas de hace dos semanas. */
    private static boolean tieneQuincenaDeVentas(List<VentaProducto> ventas, LocalDate hoy) {
        return ventas.stream().anyMatch(venta -> !venta.fecha().isAfter(hoy.minusDays(DIAS_DE_LA_QUINCENA - 1L)));
    }

    private static ResumenNegocio resumen(Negocio negocio, DatosDeProyecto datos, LocalDate desde, LocalDate hasta,
                                          LocalDate hoy) {
        return ResumenNegocio.armar(negocio.getNombre(), datos, AnalisisZona.sinZona(), List.of(),
                new PeriodoResumen(desde, hasta), hoy);
    }
}
