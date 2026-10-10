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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lectura_ia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaIaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "lote_id", nullable = false, updatable = false)
    private UUID loteId;

    @Column(nullable = false, columnDefinition = "text")
    private String proveedor;

    @Column(nullable = false, columnDefinition = "text")
    private String modelo;

    @Column(name = "version_prompt", nullable = false, columnDefinition = "text")
    private String versionPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> resultado;

    @Column(name = "motivo_error", columnDefinition = "text")
    private String motivoError;

    @Column(nullable = false)
    private Short paginas;

    @Column(name = "tokens_entrada", nullable = false)
    private Integer tokensEntrada;

    @Column(name = "tokens_salida", nullable = false)
    private Integer tokensSalida;

    @Column(name = "costo_estimado_usd", precision = 12, scale = 6)
    private BigDecimal costoEstimadoUsd;

    @Column(name = "duracion_ms", nullable = false)
    private Integer duracionMs;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
