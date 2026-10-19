package com.ensambla.backend.infrastructure.presentation.mapper.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.dto.aviso.ListarAvisosRequest;
import com.ensambla.backend.domain.dto.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AvisoPresentationMapper {

    public ListarAvisosRequest toListarRequest(UUID usuarioId, EstadoAviso estado) {
        return new ListarAvisosRequest(usuarioId, estado);
    }

    public ResolverAvisoRequest toResolverRequest(UUID usuarioId, UUID avisoId) {
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
}
