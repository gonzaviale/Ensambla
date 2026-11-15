package com.ensambla.backend.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Sólo se lee con JPA: las fotos se escriben con el upsert de {@code IProgresoObjetivoJpaRepository}. */
@Entity
@Table(name = "progreso_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoObjetivoEntity {

    @Id
    private UUID id;

    @Column(name = "objetivo_id", nullable = false, updatable = false)
    private UUID objetivoId;

    @Column(name = "mini_objetivo_id", updatable = false)
    private UUID miniObjetivoId;

    @Column(nullable = false, updatable = false)
    private LocalDate dia;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "medido_en", nullable = false)
    private OffsetDateTime medidoEn;
}
