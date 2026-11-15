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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** El id lo asigna el dominio al cerrar la semana. Un resumen no se modifica. */
@Entity
@Table(name = "resumen_semanal_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenSemanalEntity {

    @Id
    private UUID id;

    @Column(name = "objetivo_id", nullable = false, updatable = false)
    private UUID objetivoId;

    @Column(name = "semana_inicio", nullable = false, updatable = false)
    private LocalDate semanaInicio;

    @Column(nullable = false, updatable = false)
    private BigDecimal resultado;

    @Column(nullable = false, updatable = false)
    private BigDecimal avance;

    @Column(name = "decisiones_aplicadas", nullable = false, updatable = false)
    private int decisionesAplicadas;

    @Column(name = "decisiones_descartadas", nullable = false, updatable = false)
    private int decisionesDescartadas;

    @Column(name = "decisiones_aceptadas", nullable = false, updatable = false)
    private int decisionesAceptadas;

    @Column(name = "decisiones_ignoradas", nullable = false, updatable = false)
    private int decisionesIgnoradas;

    /** El valor de cada mini-objetivo al cierre: {@code [{"miniObjetivoId": "…", "valor": "123.45"}]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private List<Object> minis = List.of();

    @Column(name = "que_funciono", nullable = false, updatable = false, columnDefinition = "text")
    private String queFunciono;

    @Column(name = "texto_con_ia", nullable = false, updatable = false)
    private boolean textoConIa;

    @Column(name = "cerrado_en", nullable = false, updatable = false)
    private OffsetDateTime cerradoEn;
}
