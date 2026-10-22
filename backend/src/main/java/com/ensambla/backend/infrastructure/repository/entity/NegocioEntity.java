package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.RubroNegocio;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "negocio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegocioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(name = "duenio_id", nullable = false)
    private UUID duenioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private RubroNegocio rubro = RubroNegocio.OTRO;

    @Column(columnDefinition = "text")
    private String direccion;

    @Column(columnDefinition = "text")
    private String localidad;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "zona_id")
    private UUID zonaId;

    @Column(name = "comparte_datos_zona", nullable = false)
    @Builder.Default
    private Boolean comparteDatosZona = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
