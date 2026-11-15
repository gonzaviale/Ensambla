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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "renovacion_premium")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenovacionPremiumEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "negocio_id")
    private UUID negocioId;

    @Column(name = "proveedor_id", columnDefinition = "text")
    private String proveedorId;

    @Column(name = "enlace", columnDefinition = "text")
    private String enlace;

    @Column(name = "estado", columnDefinition = "text")
    private String estado;

    @Column(name = "importe")
    private BigDecimal importe;

    @Column(name = "moneda", columnDefinition = "text")
    private String moneda;

    @Column(name = "primer_cobro_en")
    private OffsetDateTime primerCobroEn;

    @Column(name = "cancelacion_solicitada")
    private boolean cancelacionSolicitada;

    @Column(name = "proximo_cobro_en")
    private OffsetDateTime proximoCobroEn;

    @Column(name = "modificado_proveedor_en")
    private OffsetDateTime modificadoProveedorEn;

    @Column(name = "sincronizado_en")
    private OffsetDateTime sincronizadoEn;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;
}
