package com.ensambla.backend.infrastructure.presentation.mapper.metrica;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem.DeDondeSaleItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem.FilaItem;
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.InterpretarPedidoRequest;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricasRequest;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridasRequest;
import com.ensambla.backend.domain.useCase.metrica.moverMetrica.MoverMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetricaRequest;
import com.ensambla.backend.domain.useCase.metrica.renombrarMetrica.RenombrarMetricaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.CrearMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.DefinicionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.InterpretarPedidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MoverMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.RenombrarMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse.DeDondeSaleResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse.FilaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.SugeridaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetricaPresentationMapper {

    public ListarMetricasRequest toListarRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new ListarMetricasRequest(usuarioId, proyectoId);
    }

    public ListarSugeridasRequest toListarSugeridasRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new ListarSugeridasRequest(usuarioId, proyectoId);
    }

    public PrevisualizarMetricaRequest toPrevisualizarRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, MetricaWebRequest request) {
        return new PrevisualizarMetricaRequest(
                usuarioId, proyectoId, request.nombre(), request.fuente(), request.calculo(), request.agrupar(),
                request.periodo(), request.soloFiado());
    }

    public CrearMetricaRequest toCrearRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, CrearMetricaWebRequest request) {
        return new CrearMetricaRequest(
                usuarioId, proyectoId, request.nombre(), request.fuente(), request.calculo(), request.agrupar(),
                request.periodo(), request.soloFiado(), request.autoria());
    }

    public EliminarMetricaRequest toEliminarRequest(UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId) {
        return new EliminarMetricaRequest(usuarioId, proyectoId, metricaId);
    }

    public InterpretarPedidoRequest toInterpretarRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, InterpretarPedidoWebRequest request) {
        return new InterpretarPedidoRequest(usuarioId, proyectoId, request.pedido());
    }

    public RenombrarMetricaRequest toRenombrarRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId, RenombrarMetricaWebRequest request) {
        return new RenombrarMetricaRequest(usuarioId, proyectoId, metricaId, request.nombre());
    }

    public MoverMetricaRequest toMoverRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId, MoverMetricaWebRequest request) {
        return new MoverMetricaRequest(usuarioId, proyectoId, metricaId, request.lugar());
    }

    public List<MetricaResponse> toResponse(List<MetricaItem> metricas) {
        return metricas.stream().map(this::toResponse).toList();
    }

    public MetricaResponse toResponse(MetricaItem metrica) {
        return new MetricaResponse(
                metrica.id(), toResponse(metrica.definicion()), metrica.orden(), metrica.autoria(),
                toResponse(metrica.resultado()));
    }

    public List<SugeridaResponse> toSugeridasResponse(List<SugeridaItem> sugeridas) {
        return sugeridas.stream().map(this::toSugeridaResponse).toList();
    }

    public ResultadoResponse toResponse(ResultadoItem resultado) {
        return new ResultadoResponse(resultado.formato(), resultado.valor(), toFilasResponse(resultado.filas()),
                resultado.motivo(), toResponse(resultado.deDondeSale()));
    }

    public SugeridaResponse toSugeridaResponse(SugeridaItem sugerida) {
        return new SugeridaResponse(toResponse(sugerida.definicion()), toResponse(sugerida.resultado()));
    }

    private List<FilaResponse> toFilasResponse(List<FilaItem> filas) {
        return filas.stream().map(fila -> new FilaResponse(fila.etiqueta(), fila.valor())).toList();
    }

    private DeDondeSaleResponse toResponse(DeDondeSaleItem deDondeSale) {
        return new DeDondeSaleResponse(deDondeSale.fuente(), deDondeSale.filasUsadas(), deDondeSale.filasSinDato(),
                deDondeSale.desde(), deDondeSale.hasta());
    }

    private DefinicionResponse toResponse(DefinicionItem definicion) {
        return new DefinicionResponse(
                definicion.nombre(), definicion.fuente(), definicion.calculo(), definicion.agrupar(),
                definicion.periodo(), definicion.soloFiado());
    }
}
