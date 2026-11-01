package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.lote.EstadoLote;
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

@Entity
@Table(name = "lote_subida")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteSubidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "bloque_destino_id")
    private UUID bloqueDestinoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EstadoLote estado;

    @Column(name = "motivo_error", columnDefinition = "text")
    private String motivoError;

    @Column(nullable = false)
    private Short intentos;

    @Column(name = "procesado_en")
    private OffsetDateTime procesadoEn;

    @Column(name = "confirmado_en")
    private OffsetDateTime confirmadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
