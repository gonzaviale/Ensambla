package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.model.campo.Campo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CampoItem(
        UUID id,
        UUID bloqueId,
        String nombre,
        String tipo,
        boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        UUID bloqueRelacionadoId,
        String formula,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {

    public static CampoItem de(Campo campo) {
        return new CampoItem(
                campo.getId(),
                campo.getBloqueId(),
                campo.getNombre(),
                campo.getTipo().name(),
                campo.esObligatorio(),
                campo.getValorInicial(),
                campo.getOpciones(),
                campo.getBloqueRelacionadoId(),
                campo.getFormula(),
                campo.getPosicion(),
                campo.getCreadoEn(),
                campo.getActualizadoEn());
    }
}
