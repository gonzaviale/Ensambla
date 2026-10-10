package com.ensambla.backend.domain.dto.registro;

import com.ensambla.backend.domain.model.registro.Registro;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RegistroItem(
        UUID id,
        UUID bloqueId,
        Map<String, Object> valores,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {

    public static RegistroItem de(Registro registro) {
        return new RegistroItem(
                registro.getId(),
                registro.getBloqueId(),
                registro.getValores(),
                registro.getCreadoEn(),
                registro.getActualizadoEn());
    }
}
