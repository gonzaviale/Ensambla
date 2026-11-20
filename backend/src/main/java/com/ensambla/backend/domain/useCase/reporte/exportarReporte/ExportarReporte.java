package com.ensambla.backend.domain.useCase.reporte.exportarReporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.reporte.TablaReporte;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExportarReporte {

    /** Filas que entran en un archivo descargado. */
    static final int MAXIMO_EXPORTACION = 10_000;

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final BloqueRepository bloqueRepository;

    private final ReporteService reporteService;

    private final RegistroRepository registroRepository;

    private final CampoRepository campoRepository;

    private final ExportadorReportePort exportadorReportePort;

    private final Clock clock;

    /**
     * El archivo con todas las filas que cumplen el filtro y sus totales (los mismos que se ven en pantalla).
     *
     * @throws BusinessRuleException si son más de {@value MAXIMO_EXPORTACION} filas: no se descarga un
     *                               archivo incompleto, hay que acotar los filtros
     */
    public ExportarReporteResponse execute(GenerarReporteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request, negocioId);
        exigirLaFuncionDeExportar(negocioId);
        List<Campo> campos = obtenerCampos(bloque.getId(), negocioId);
        ReporteRegistros reporte = generarElReporteCompleto(request.comoFiltro(negocioId, bloque, campos), campos);
        TablaReporte tabla = TablaReporte.de(bloque.getNombre(), campos, reporte,
                etiquetasDeConexiones(negocioId, campos, reporte.filas()));
        return ExportarReporteResponse.de(exportar(tabla), bloque.getNombre(), LocalDate.now(clock));
    }

    private ReporteRegistros generarElReporteCompleto(FiltroReporte filtro, List<Campo> campos) {
        ReporteRegistros reporte = reporteService.generar(filtro, campos, MAXIMO_EXPORTACION);
        if (reporte.truncado()) {
            throw new BusinessRuleException("El reporte tiene más de " + MAXIMO_EXPORTACION
                    + " filas: acotá los filtros para poder descargarlo");
        }
        return reporte;
    }

    /**
     * El nombre de cada fila a la que apuntan las columnas de conexión de las filas del reporte (una consulta por
     * tabla conectada), indexado por el id de la fila.
     */
    private Map<RegistroId, String> etiquetasDeConexiones(NegocioId negocioId, List<Campo> campos,
                                                          List<Registro> filas) {
        Map<RegistroId, String> etiquetas = new HashMap<>();
        idsConectadosPorTabla(campos, filas).forEach((tablaId, ids) ->
                etiquetasDeLaTabla(tablaId, ids, negocioId).forEach(
                        referencia -> etiquetas.put(referencia.id(), referencia.etiqueta())));
        return etiquetas;
    }

    private static Map<BloqueId, Set<RegistroId>> idsConectadosPorTabla(List<Campo> campos, List<Registro> filas) {
        Map<BloqueId, Set<RegistroId>> idsPorTabla = new HashMap<>();
        campos.stream().filter(Campo::esRelacion).forEach(campo -> filas.forEach(
                fila -> idConectado(fila, campo).ifPresent(id -> idsPorTabla
                        .computeIfAbsent(campo.getBloqueRelacionadoId(), tabla -> new HashSet<>()).add(id))));
        return idsPorTabla;
    }

    private static Optional<RegistroId> idConectado(Registro fila, Campo campo) {
        if (fila.getValores().get(campo.getId().toString()) instanceof String id) {
            return Optional.of(RegistroId.de(id));
        }
        return Optional.empty();
    }

    private List<Referencia> etiquetasDeLaTabla(BloqueId tablaId, Set<RegistroId> ids, NegocioId negocioId) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(obtenerCampos(tablaId, negocioId));
        return buscarFilas(ids, tablaId, negocioId).stream().map(etiquetador).toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(GenerarReporteRequest request, NegocioId negocioId) {
        return bloqueRepository.obtener(request.bloqueId(), negocioId);
    }

    private void exigirLaFuncionDeExportar(NegocioId negocioId) {
        controlPlanService.validarFuncion(negocioId, FuncionPlan.EXPORTAR_EXCEL, OffsetDateTime.now(clock));
    }

    private List<Campo> obtenerCampos(BloqueId bloqueId, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }

    private List<Registro> buscarFilas(Set<RegistroId> ids, BloqueId tablaId, NegocioId negocioId) {
        return registroRepository.findAllByIdsAndBloque(ids, tablaId, negocioId);
    }

    private byte[] exportar(TablaReporte tabla) {
        return exportadorReportePort.exportar(tabla);
    }
}
