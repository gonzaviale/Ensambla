package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.CodigoPlan;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private CodigoPlan codigo;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(nullable = false)
    private Boolean configurado;

    @Column(name = "precio_mensual", precision = 14, scale = 2)
    private BigDecimal precioMensual;

    @Column(columnDefinition = "text")
    private String moneda;

    @Column(name = "limite_proyectos")
    private Integer limiteProyectos;

    @Column(name = "limite_registros")
    private Long limiteRegistros;

    @Column(name = "limite_paginas_ia_mensuales")
    private Integer limitePaginasIaMensuales;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> funciones;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
