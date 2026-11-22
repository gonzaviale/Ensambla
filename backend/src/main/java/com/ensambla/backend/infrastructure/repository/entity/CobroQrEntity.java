package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.caja.ConfirmacionDeCobro;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
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

@Entity
@Table(name = "cobro_qr")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CobroQrEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(updatable = false, columnDefinition = "text")
    private String alias;

    @Column(updatable = false, columnDefinition = "text")
    private String titular;

    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EstadoCobroQr estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "vence_en", nullable = false, updatable = false)
    private OffsetDateTime venceEn;

    @Column(name = "confirmado_en")
    private OffsetDateTime confirmadoEn;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private ConfirmacionDeCobro confirmacion;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "proveedor_orden_id", updatable = false, unique = true, columnDefinition = "text")
    private String proveedorOrdenId;

    @Column(name = "proveedor_pago_id", columnDefinition = "text")
    private String proveedorPagoId;

    @Column(name = "qr_dinamico", columnDefinition = "text")
    private String qrDinamico;

    @Column(name = "qr_estatico", updatable = false, columnDefinition = "text")
    private String qrEstatico;

    @Column(name = "estado_detalle", columnDefinition = "text")
    private String estadoDetalle;
}
