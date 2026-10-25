package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.time.OffsetDateTime;
import java.util.Map;

public record RegistroItem(
        RegistroId id,
        BloqueId bloqueId,
        Map<String, Object> valores,
        FotoId fotoOrigenId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {

    public static RegistroItem de(Registro registro) {
        return new RegistroItem(
                registro.getId(),
                registro.getBloqueId(),
                registro.getValores(),
                registro.getFotoOrigenId(),
                registro.getCreadoEn(),
                registro.getActualizadoEn());
    }
}
