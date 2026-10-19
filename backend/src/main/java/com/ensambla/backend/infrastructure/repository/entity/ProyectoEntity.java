package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.DistribucionProyecto;
import com.ensambla.backend.domain.model.enums.PlantillaSistema;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "proyecto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProyectoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "plantilla_id")
    private UUID plantillaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plantilla_clave", columnDefinition = "text")
    private PlantillaSistema plantillaClave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private DistribucionProyecto distribucion = DistribucionProyecto.MOSAICO;

    @Column(name = "con_movimientos", nullable = false)
    @Builder.Default
    private Boolean conMovimientos = false;

    @Column(name = "con_gastos", nullable = false)
    @Builder.Default
    private Boolean conGastos = false;

    @Column(name = "es_demo", nullable = false)
    @Builder.Default
    private Boolean esDemo = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
