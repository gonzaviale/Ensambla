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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "registro")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "bloque_id", nullable = false, updatable = false)
    private UUID bloqueId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> valores;

    @Column(name = "foto_origen_id")
    private UUID fotoOrigenId;

    @Column(name = "es_ejemplo", nullable = false)
    @Builder.Default
    private Boolean esEjemplo = false;

    @Column(columnDefinition = "text")
    private String origen;

    /** Posición de la fila dentro de su bloque, como la dejó la persona. */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

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
