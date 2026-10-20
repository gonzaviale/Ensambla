package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Texto o nota de voz recibido por WhatsApp de un número vinculado. */
@Entity
@Table(name = "whatsapp_mensaje")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeWhatsappEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String telefono;

    @Column(name = "id_externo", nullable = false, updatable = false, columnDefinition = "text")
    private String idExterno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private TipoMensajeWhatsapp tipo;

    @Column(columnDefinition = "text")
    private String texto;

    @Column(name = "mime_audio", columnDefinition = "text")
    private String mimeAudio;

    @Column(name = "duracion_seg")
    private Integer duracionSeg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private EstadoMensajeWhatsapp estado = EstadoMensajeWhatsapp.RECIBIDO;

    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;
}
