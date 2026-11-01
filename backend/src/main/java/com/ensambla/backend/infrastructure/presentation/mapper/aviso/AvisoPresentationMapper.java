package com.ensambla.backend.infrastructure.presentation.mapper.aviso;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoAvisos;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ImpactoDeAviso;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ImpactoAvisosResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvisoPresentationMapper {

    public ListarAvisosRequest toListarRequest(UsuarioId usuarioId, EstadoAviso estado) {
        return new ListarAvisosRequest(usuarioId, estado);
    }

    public ResolverAvisoRequest toResolverRequest(UsuarioId usuarioId, AvisoId avisoId) {
        return new ResolverAvisoRequest(usuarioId, avisoId);
    }

    public List<AvisoResponse> toResponse(List<AvisoItem> avisos) {
        return avisos.stream().map(this::toResponse).toList();
    }

    public AvisoResponse toResponse(AvisoItem aviso) {
        return new AvisoResponse(
                aviso.id(),
                aviso.proyectoId(),
                aviso.tipo(),
                aviso.grupo(),
                aviso.prioridad(),
                aviso.estado(),
                aviso.entidad(),
                aviso.titulo(),
                aviso.detalle(),
                aviso.motivos(),
                aviso.acciones(),
                aviso.datos(),
                aviso.creadoEn(),
                aviso.resueltoEn());
    }

    public ContadorAvisosResponse toResponse(ContadorAvisos contador) {
        return new ContadorAvisosResponse(contador.pendientes(), contador.urgentes());
    }

    public ImpactoAvisosResponse toResponse(ImpactoAvisos impacto) {
        return new ImpactoAvisosResponse(impacto.aplicados(), impacto.descartados(), impacto.ingresoSemanalDelta(),
                impacto.medidos().stream().map(this::toResponse).toList());
    }

    private ImpactoAvisosResponse.Medido toResponse(ImpactoDeAviso medido) {
        return new ImpactoAvisosResponse.Medido(
                medido.avisoId(),
                medido.tipo(),
                medido.titulo(),
                medido.aplicadoEn(),
                medido.impacto().ventanaDias(),
                medido.impacto().unidadesPorDiaAntes(),
                medido.impacto().unidadesPorDiaDespues(),
                medido.impacto().ingresoPorDiaAntes(),
                medido.impacto().ingresoPorDiaDespues(),
                medido.impacto().deltaIngresoSemanal());
    }
}
