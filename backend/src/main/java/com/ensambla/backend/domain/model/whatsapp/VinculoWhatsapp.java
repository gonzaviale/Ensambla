package com.ensambla.backend.domain.model.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Número de WhatsApp de un usuario, vinculado a su negocio: sólo desde él se puede cargar datos por chat. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VinculoWhatsapp {

    private UUID id;

    private UUID usuarioId;

    private UUID negocioId;

    private String telefono;

    private OffsetDateTime vinculadoEn;

    public static VinculoWhatsapp nuevo(UUID usuarioId, UUID negocioId, TelefonoWhatsapp telefono,
                                        OffsetDateTime ahora) {
        return VinculoWhatsapp.builder()
                .usuarioId(usuarioId)
                .negocioId(negocioId)
                .telefono(telefono.valor())
                .vinculadoEn(ahora)
                .build();
    }
}
