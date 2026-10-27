package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;

/** Texto, foto o nota de voz recibido de un número vinculado, en la cola para que Sami lo atienda. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MensajeWhatsapp {

    private MensajeWhatsappId id;

    private NegocioId negocioId;

    private String telefono;

    /** Id del mensaje en WhatsApp; evita guardar dos veces el mismo si el proveedor reintenta. */
    private String idExterno;

    private TipoMensajeWhatsapp tipo;

    /** El texto, o el pie si es una foto. */
    private String texto;

    /** Tipo del audio o de la imagen. */
    private String mimeMedia;

    private Integer duracionSeg;

    @Builder.Default
    private EstadoMensajeWhatsapp estado = EstadoMensajeWhatsapp.RECIBIDO;

    private OffsetDateTime recibidoEn;

    private int intentos;

    /** Hasta cuándo es suyo el mensaje para quien lo reclamó; pasado ese momento se da por perdido. */
    private OffsetDateTime reclamadoHasta;

    private OffsetDateTime atendidoEn;

    private String error;

    public static MensajeWhatsapp recibido(NegocioId negocioId, MensajeEntrante entrante) {
        return MensajeWhatsapp.builder()
                .negocioId(negocioId)
                .telefono(entrante.telefono())
                .idExterno(entrante.idExterno())
                .tipo(entrante.tipo())
                .texto(entrante.texto())
                .mimeMedia(entrante.mimeMedia())
                .duracionSeg(entrante.duracionSeg())
                .recibidoEn(entrante.recibidoEn())
                .build();
    }

    public void reclamar(OffsetDateTime ahora, Duration vigencia) {
        estado = EstadoMensajeWhatsapp.ATENDIENDO;
        intentos++;
        reclamadoHasta = ahora.plus(vigencia);
    }

    public void atender(OffsetDateTime ahora) {
        estado = EstadoMensajeWhatsapp.ATENDIDO;
        atendidoEn = ahora;
        error = null;
    }

    public void fallar(String motivo, OffsetDateTime ahora) {
        estado = EstadoMensajeWhatsapp.FALLIDO;
        atendidoEn = ahora;
        error = motivo;
    }
}
