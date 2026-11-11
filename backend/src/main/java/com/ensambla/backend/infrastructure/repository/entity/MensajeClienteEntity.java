package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
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

/** Texto recibido por WhatsApp de un cliente, en la cola de atención. */
@Entity
@Table(name = "mensaje_cliente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String telefono;

    @Column(name = "id_externo", nullable = false, updatable = false, columnDefinition = "text")
    private String idExterno;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private EstadoMensajeWhatsapp estado = EstadoMensajeWhatsapp.RECIBIDO;

    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "reclamado_hasta")
    private OffsetDateTime reclamadoHasta;

    @Column(name = "atendido_en")
    private OffsetDateTime atendidoEn;

    @Column(columnDefinition = "text")
    private String error;
}
