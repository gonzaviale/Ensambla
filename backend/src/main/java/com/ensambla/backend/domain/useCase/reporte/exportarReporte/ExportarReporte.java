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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.EXPORTAR_EXCEL, OffsetDateTime.now(clock));
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        FiltroReporte filtro = FiltroReporte.armar(
                negocioId,
                bloque.getId(),
                campos,
                request.texto(),
                request.desde(),
                request.hasta(),
                request.campoId(),
                request.valor());
        ReporteRegistros reporte = reporteService.generar(filtro, campos, MAXIMO_EXPORTACION);
        if (reporte.truncado()) {
            throw new BusinessRuleException("El reporte tiene más de " + MAXIMO_EXPORTACION
                    + " filas: acotá los filtros para poder descargarlo");
        }
        Map<RegistroId, String> etiquetas = etiquetasDeConexiones(negocioId, campos, reporte.filas());
        TablaReporte tabla = TablaReporte.de(bloque.getNombre(), campos, reporte, etiquetas);
        return ExportarReporteResponse.de(
                exportadorReportePort.exportar(tabla), bloque.getNombre(), LocalDate.now(clock));
    }

    /**
     * El nombre de cada fila a la que apuntan las columnas de conexión de las filas del reporte (una consulta por
     * tabla conectada), indexado por el id de la fila.
     */
    private Map<RegistroId, String> etiquetasDeConexiones(NegocioId negocioId, List<Campo> campos,
            List<Registro> filas) {
        Map<BloqueId, Set<RegistroId>> idsPorTabla = new HashMap<>();
        for (Campo campo : campos.stream().filter(Campo::esRelacion).toList()) {
            for (Registro fila : filas) {
                if (fila.getValores().get(campo.getId().toString()) instanceof String id) {
                    idsPorTabla.computeIfAbsent(campo.getBloqueRelacionadoId(), tabla -> new HashSet<>())
                            .add(RegistroId.de(id));
                }
            }
        }
        Map<RegistroId, String> etiquetas = new HashMap<>();
        idsPorTabla.forEach((tablaId, ids) -> {
            Function<Registro, Referencia> etiquetador = Referencia.etiquetador(
                    campoRepository.findByBloqueIdAndNegocioId(tablaId, negocioId));
            registroRepository.findAllByIdsAndBloque(ids, tablaId, negocioId).stream()
                    .map(etiquetador)
                    .forEach(referencia -> etiquetas.put(referencia.id(), referencia.etiqueta()));
        });
        return etiquetas;
    }
}
