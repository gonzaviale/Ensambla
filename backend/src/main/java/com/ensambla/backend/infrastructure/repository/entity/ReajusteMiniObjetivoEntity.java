package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** El id lo asigna el dominio. Se guarda junto con su resumen y no se modifica. */
@Entity
@Table(name = "reajuste_mini_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReajusteMiniObjetivoEntity {

    @Id
    private UUID id;

    @Column(name = "resumen_id", nullable = false, updatable = false)
    private UUID resumenId;

    @Column(name = "mini_anterior_id", nullable = false, updatable = false)
    private UUID miniAnteriorId;

    @Column(name = "mini_nuevo_id")
    private UUID miniNuevoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private TipoReajuste tipo;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String motivo;

    @Column(name = "titulo_anterior", nullable = false, updatable = false, columnDefinition = "text")
    private String tituloAnterior;

    @Column(name = "meta_anterior", nullable = false, updatable = false)
    private BigDecimal metaAnterior;

    @Column(name = "titulo_nuevo", updatable = false, columnDefinition = "text")
    private String tituloNuevo;

    @Column(name = "meta_nueva", updatable = false)
    private BigDecimal metaNueva;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
