package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
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

/** El id lo asigna el dominio al responder. Una respuesta no se modifica. */
@Entity
@Table(name = "respuesta_mini_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaMiniObjetivoEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "objetivo_id", nullable = false, updatable = false)
    private UUID objetivoId;

    @Column(name = "mini_objetivo_id", nullable = false, updatable = false)
    private UUID miniObjetivoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private HizoManual hizo;

    @Enumerated(EnumType.STRING)
    @Column(name = "que_paso", nullable = false, updatable = false, columnDefinition = "text")
    private QuePaso quePaso;

    @Column(updatable = false, columnDefinition = "text")
    private String comentario;

    @Column(name = "semana_inicio", nullable = false, updatable = false)
    private LocalDate semanaInicio;

    @Column(name = "respondida_en", nullable = false, updatable = false)
    private OffsetDateTime respondidaEn;
}
