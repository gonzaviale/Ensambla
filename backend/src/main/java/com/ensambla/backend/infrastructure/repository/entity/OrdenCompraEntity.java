package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
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
import java.util.UUID;

/** Orden de compra que un cliente confirmó por WhatsApp. Los ítems se guardan como texto JSON. */
@Entity
@Table(name = "orden_compra")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EstadoOrdenCompra estado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String items;

    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "motivo_rechazo", columnDefinition = "text")
    private String motivoRechazo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
