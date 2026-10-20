package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Código de un solo uso que el usuario manda por WhatsApp para demostrar que el número es suyo. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVinculacion {

    public static final Duration VIGENCIA = Duration.ofMinutes(10);

    private UUID id;

    private UUID usuarioId;

    private String codigo;

    private OffsetDateTime expiraEn;

    private OffsetDateTime usadoEn;

    public static CodigoVinculacion nuevo(UUID usuarioId, String codigo, OffsetDateTime ahora) {
        return CodigoVinculacion.builder()
                .usuarioId(usuarioId)
                .codigo(codigo)
                .expiraEn(ahora.plus(VIGENCIA))
                .build();
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn);
    }

    public void usar(OffsetDateTime ahora) {
        if (!estaVigente(ahora)) {
            throw new BusinessRuleException("El código de vinculación venció o ya se usó");
        }
        usadoEn = ahora;
    }
}
