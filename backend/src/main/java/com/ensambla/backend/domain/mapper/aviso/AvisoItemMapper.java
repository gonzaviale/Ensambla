package com.ensambla.backend.domain.mapper.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.model.aviso.Aviso;

import java.util.List;

public class AvisoItemMapper {

    public List<AvisoItem> toResponse(List<Aviso> avisos) {
        return avisos.stream().map(this::toResponse).toList();
    }

    public AvisoItem toResponse(Aviso aviso) {
        return new AvisoItem(
                aviso.getId(),
                aviso.getProyectoId(),
                aviso.getTipo(),
                aviso.getGrupo(),
                aviso.getPrioridad(),
                aviso.getEstado(),
                aviso.getEntidad(),
                aviso.getTitulo(),
                aviso.getDetalle(),
                aviso.getMotivos(),
                aviso.getAcciones(),
                aviso.getDatos(),
                aviso.getCreadoEn(),
                aviso.getResueltoEn());
    }
}
