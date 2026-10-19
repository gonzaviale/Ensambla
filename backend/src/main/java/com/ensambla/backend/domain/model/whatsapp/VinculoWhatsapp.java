package com.ensambla.backend.domain.model.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El número de WhatsApp de un negocio. Para vincularlo, la app da un código que la persona manda por WhatsApp desde
 * su número: el número del que llega el código es el que queda vinculado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VinculoWhatsapp {

    public static final Duration VIGENCIA_CODIGO = Duration.ofMinutes(10);

    private UUID id;

    private UUID negocioId;

    /** Número como lo manda Meta (país y número, sólo dígitos); {@code null} si no está vinculado. */
    private String telefono;

    private OffsetDateTime vinculadoEn;

    private String codigo;

    private OffsetDateTime codigoVenceEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public static VinculoWhatsapp nuevo(UUID negocioId, OffsetDateTime ahora) {
        return VinculoWhatsapp.builder()
                .negocioId(negocioId)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** Reemplaza el código anterior, si había. El número vinculado no cambia hasta que llegue el código nuevo. */
    public void asignarCodigo(String codigo, OffsetDateTime ahora) {
        this.codigo = codigo;
        this.codigoVenceEn = ahora.plus(VIGENCIA_CODIGO);
        this.actualizadoEn = ahora;
    }

    public boolean codigoVigente(OffsetDateTime ahora) {
        return codigoVenceEn != null && ahora.isBefore(codigoVenceEn);
    }

    /** Guarda el número y borra el código, que ya no sirve. */
    public void vincular(String telefono, OffsetDateTime ahora) {
        this.telefono = telefono;
        this.vinculadoEn = ahora;
        this.codigo = null;
        this.codigoVenceEn = null;
        this.actualizadoEn = ahora;
    }

    public void desvincular(OffsetDateTime ahora) {
        this.telefono = null;
        this.vinculadoEn = null;
        this.actualizadoEn = ahora;
    }
}
