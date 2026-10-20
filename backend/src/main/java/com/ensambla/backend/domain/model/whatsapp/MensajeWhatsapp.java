package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Texto o nota de voz recibido de un número vinculado, a la espera de transcribirse e interpretarse. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MensajeWhatsapp {

    private UUID id;

    private UUID negocioId;

    private String telefono;

    /** Id del mensaje en WhatsApp; evita guardar dos veces el mismo si el proveedor reintenta. */
    private String idExterno;

    private TipoMensajeWhatsapp tipo;

    private String texto;

    private String mimeAudio;

    private Integer duracionSeg;

    @Builder.Default
    private EstadoMensajeWhatsapp estado = EstadoMensajeWhatsapp.RECIBIDO;

    private OffsetDateTime recibidoEn;

    public static MensajeWhatsapp recibido(UUID negocioId, MensajeEntrante entrante) {
        return MensajeWhatsapp.builder()
                .negocioId(negocioId)
                .telefono(entrante.telefono())
                .idExterno(entrante.idExterno())
                .tipo(entrante.tipo())
                .texto(entrante.texto())
                .mimeAudio(entrante.mimeAudio())
                .duracionSeg(entrante.duracionSeg())
                .recibidoEn(entrante.recibidoEn())
                .build();
    }
}
