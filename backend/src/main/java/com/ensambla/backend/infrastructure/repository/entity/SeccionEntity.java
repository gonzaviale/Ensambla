package com.ensambla.backend.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "seccion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeccionEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proyecto_id", nullable = false, updatable = false)
    private UUID proyectoId;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(nullable = false)
    private Short posicion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    /** El id puede venir del frontend (el sistema se guarda entero); si no viene, se genera al insertar. */
    @PrePersist
    void asignarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
