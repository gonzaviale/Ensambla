package com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio;

import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.VentaDelDia;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse.UltimoDiaConVentas;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse.Ventas;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse.VentasDelPeriodo;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
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
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerProyecto(negocio, request.proyectoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        DatosDeProyecto datos = cargarDatos(proyecto, negocio);
        return armarResumenDeInicio(negocio, datos, hoy);
    }

    private ObtenerResumenInicioResponse armarResumenDeInicio(Negocio negocio, DatosDeProyecto datos, LocalDate hoy) {
        List<VentaProducto> ventas = datos.ventasHasta(hoy);
        ResumenNegocio mes = resumenDeLosUltimosDias(negocio, datos, DIAS_DEL_MES, hoy);
        return ObtenerResumenInicioResponse.de(hoy, !ventas.isEmpty(), mes,
                resumenDeLosUltimosDias(negocio, datos, DIAS_DE_LA_QUINCENA, hoy),
                ventasDelInicio(negocio, datos, ventas, mes, hoy),
                VentaDelDia.serie(ventas, hoy.minusDays(DIAS_DEL_MES - 1L), hoy),
                ultimoDiaConVentas(negocio, datos, ventas, hoy),
                tieneQuincenaDeVentas(ventas, hoy));
    }

    private Ventas ventasDelInicio(Negocio negocio, DatosDeProyecto datos, List<VentaProducto> ventas,
                                   ResumenNegocio mes, LocalDate hoy) {
        ResumenNegocio semana = resumenDeLosUltimosDias(negocio, datos, DIAS_DE_UNA_SEMANA, hoy);
        ResumenNegocio dia = resumen(negocio, datos, hoy, hoy, hoy);
        return new Ventas(
                VentasDelPeriodo.de(dia, ingresosDelMismoDiaDeLaSemanaPasada(negocio, datos, hoy),
                        sinPrecio(ventas, hoy)),
                VentasDelPeriodo.de(semana, semana.ventas().ingresosAnteriores(),
                        sinPrecio(ventas, hoy.minusDays(DIAS_DE_UNA_SEMANA - 1L))),
                VentasDelPeriodo.de(mes, mes.ventas().ingresosAnteriores(),
                        sinPrecio(ventas, hoy.minusDays(DIAS_DEL_MES - 1L))));
    }

    private static BigDecimal ingresosDelMismoDiaDeLaSemanaPasada(Negocio negocio, DatosDeProyecto datos,
                                                                  LocalDate hoy) {
        LocalDate semanaPasada = hoy.minusDays(DIAS_DE_UNA_SEMANA);
        return resumen(negocio, datos, semanaPasada, semanaPasada, hoy).ventas().ingresos();
    }

    /** Cuántas ventas del período que termina hoy no tienen precio y por eso no suman plata. */
    private static long sinPrecio(List<VentaProducto> ventas, LocalDate desde) {
        return ventas.stream()
                .filter(venta -> !venta.fecha().isBefore(desde))
                .filter(venta -> venta.precio() == null)
                .count();
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

    private static ResumenNegocio resumenDeLosUltimosDias(Negocio negocio, DatosDeProyecto datos, int dias,
                                                          LocalDate hoy) {
        return resumen(negocio, datos, hoy.minusDays(dias - 1L), hoy, hoy);
    }

    private static ResumenNegocio resumen(Negocio negocio, DatosDeProyecto datos, LocalDate desde, LocalDate hasta,
                                          LocalDate hoy) {
        return ResumenNegocio.armar(negocio.getNombre(), datos, AnalisisZona.sinZona(), List.of(),
                new PeriodoResumen(desde, hasta), hoy);
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerProyecto(Negocio negocio, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocio.getId(), proyectoId);
    }

    private DatosDeProyecto cargarDatos(Proyecto proyecto, Negocio negocio) {
        return datosDeProyectoService.cargar(proyecto, negocio.getId());
    }
}
