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
@Table(name = "pago_premium")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoPremiumEntity {

    @Id
    @Column(name = "factura_id", columnDefinition = "text")
    private String facturaId;

    @Column(name = "negocio_id")
    private UUID negocioId;

    @Column(name = "renovacion_id")
    private UUID renovacionId;

    @Column(name = "pago_id", columnDefinition = "text")
    private String pagoId;

    @Column(name = "estado", columnDefinition = "text")
    private String estado;

    @Column(name = "inicio_en")
    private OffsetDateTime inicioEn;

    @Column(name = "fin_en")
    private OffsetDateTime finEn;

    @Column(name = "modificado_proveedor_en")
    private OffsetDateTime modificadoProveedorEn;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;
}
