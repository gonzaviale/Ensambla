package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.TipoCampo;
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
@Table(name = "campo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "bloque_id", nullable = false, updatable = false)
    private UUID bloqueId;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private TipoCampo tipo;

    @Column(nullable = false)
    private Boolean obligatorio;

    @Column(name = "valor_inicial", columnDefinition = "text")
    private String valorInicial;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> opciones;

    @Column(name = "bloque_relacionado_id")
    private UUID bloqueRelacionadoId;

    @Column(columnDefinition = "text")
    private String formula;

    @Column(columnDefinition = "text")
    private String clave;

    @Column(precision = 14, scale = 3)
    private BigDecimal minimo;

    @Column(nullable = false)
    private Short posicion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
