package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** El id lo asigna el dominio al registrar la decisión. */
@Entity
@Table(name = "decision_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionObjetivoEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "objetivo_id", nullable = false, updatable = false)
    private UUID objetivoId;

    @Column(name = "mini_objetivo_id")
    private UUID miniObjetivoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private TipoDecision tipo;

    @Column(name = "aviso_id")
    private UUID avisoId;

    @Column(name = "registro_id", updatable = false)
    private UUID registroId;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "tomada_en", nullable = false, updatable = false)
    private OffsetDateTime tomadaEn;

    @Column(name = "semana_inicio", nullable = false, updatable = false)
    private LocalDate semanaInicio;
}
