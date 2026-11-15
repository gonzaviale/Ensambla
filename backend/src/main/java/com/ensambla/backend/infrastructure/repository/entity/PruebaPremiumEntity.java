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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "prueba_premium")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaPremiumEntity {

    @Id
    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "inicio_en", nullable = false, updatable = false)
    private OffsetDateTime inicioEn;

    @Column(name = "fin_en", nullable = false, updatable = false)
    private OffsetDateTime finEn;

    @Column(name = "paginas_ia", nullable = false)
    private int paginasIa;
}
