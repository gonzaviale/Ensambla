package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;

/** Texto de un cliente, en la cola para que Sami lo atienda. Los clientes sólo escriben texto. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MensajeCliente {

    private MensajeClienteId id;

    private String telefono;

    /** Id del mensaje en WhatsApp; evita guardar dos veces el mismo si el proveedor reintenta. */
    private String idExterno;

    private String texto;

    @Builder.Default
    private EstadoMensajeWhatsapp estado = EstadoMensajeWhatsapp.RECIBIDO;

    private OffsetDateTime recibidoEn;

    private int intentos;

    /** Hasta cuándo es suyo el mensaje para quien lo reclamó; pasado ese momento se da por perdido. */
    private OffsetDateTime reclamadoHasta;

    private OffsetDateTime atendidoEn;

    private String error;

    public static MensajeCliente recibido(MensajeEntrante entrante) {
        return MensajeCliente.builder()
                .telefono(entrante.telefono())
                .idExterno(entrante.idExterno())
                .texto(entrante.texto())
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
