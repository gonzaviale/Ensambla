package com.ensambla.backend.domain.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.model.campo.Campo;

public class CampoItemMapper {

    public CampoItem toResponse(Campo campo) {
        return new CampoItem(
                campo.getId(),
                campo.getBloqueId(),
                campo.getNombre(),
                campo.getTipo().name(),
                campo.esObligatorio(),
                campo.getValorInicial(),
                campo.getOpciones(),
                campo.getPosicion(),
                campo.getCreadoEn(),
                campo.getActualizadoEn());
    }
}
