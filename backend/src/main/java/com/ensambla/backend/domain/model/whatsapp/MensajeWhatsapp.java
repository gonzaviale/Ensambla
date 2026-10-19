package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un texto o una nota de voz de un número vinculado, guardado en su negocio para interpretarlo. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeWhatsapp {

    private UUID id;

    private UUID negocioId;

    private String telefono;

    private String waMensajeId;

    private TipoMensajeWhatsapp tipo;

    private String texto;

    private String mediaId;

    private EstadoMensajeWhatsapp estado;

    private OffsetDateTime recibidoEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public static MensajeWhatsapp nuevo(UUID negocioId, MensajeEntranteWhatsapp entrante, OffsetDateTime ahora) {
        return MensajeWhatsapp.builder()
                .negocioId(negocioId)
                .telefono(entrante.telefono())
                .waMensajeId(entrante.waMensajeId())
                .tipo(entrante.tipo())
                .texto(entrante.texto())
                .mediaId(entrante.mediaId())
                .estado(EstadoMensajeWhatsapp.PENDIENTE)
                .recibidoEn(entrante.recibidoEn())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }
}
