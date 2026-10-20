package com.ensambla.backend.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Número de WhatsApp vinculado a un usuario y a su negocio. */
@Entity
@Table(name = "whatsapp_vinculo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VinculoWhatsappEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, columnDefinition = "text")
    private String telefono;

    @Column(name = "vinculado_en", nullable = false, updatable = false)
    private OffsetDateTime vinculadoEn;
}
