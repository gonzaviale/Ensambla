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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Precio y ventas de un producto en una zona. Es un dato agregado y anónimo: la base exige al menos 3 comercios, así
 * que nunca identifica a uno solo.
 */
@Entity
@Table(name = "agregado_zona_producto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgregadoZonaProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "zona_id", nullable = false, updatable = false)
    private UUID zonaId;

    @Column(name = "producto_normalizado", nullable = false, columnDefinition = "text")
    private String productoNormalizado;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    /** Cuántos comercios aportan el precio de este producto. */
    @Column(nullable = false)
    private Integer comercios;

    /** Cuántos comercios lo venden. */
    @Column(name = "comercios_venden", nullable = false)
    private Integer comerciosVenden;

    @Column(name = "precio_minimo", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioMinimo;

    @Column(name = "precio_promedio", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioPromedio;

    @Column(name = "precio_maximo", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioMaximo;

    /** Unidades por semana que vende un comercio promedio. */
    @Column(name = "unidades_semana", nullable = false, precision = 10, scale = 2)
    private BigDecimal unidadesSemana;

    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @Column(name = "calculado_en", nullable = false)
    private OffsetDateTime calculadoEn;
}
