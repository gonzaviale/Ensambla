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
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un cambio del precio de un producto del inventario. */
@Entity
@Table(name = "historial_precio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPrecioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "registro_id", nullable = false, updatable = false)
    private UUID registroId;

    @Column(name = "precio_anterior")
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", nullable = false)
    private BigDecimal precioNuevo;

    @Column(name = "cambiado_en", nullable = false)
    private OffsetDateTime cambiadoEn;
}
