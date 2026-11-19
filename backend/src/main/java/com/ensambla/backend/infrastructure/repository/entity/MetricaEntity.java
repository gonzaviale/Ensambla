package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "metrica")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proyecto_id", nullable = false, updatable = false)
    private UUID proyectoId;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private FuenteMetrica fuente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private CalculoMetrica calculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private AgrupacionMetrica agrupar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private PeriodoMetrica periodo;

    @Column(name = "solo_fiado", nullable = false)
    private boolean soloFiado;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private AutoriaMetrica autoria;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    /** Vacío mientras la persona no eligió: se usa el de siempre para su reparto. */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private GraficoMetrica grafico;

    /** El id se genera al insertar. */
    @PrePersist
    void asignarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
