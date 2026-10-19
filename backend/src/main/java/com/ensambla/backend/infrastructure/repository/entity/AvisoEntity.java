package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Recomendación del motor de monitoreo, con su motivo y las acciones que se pueden aplicar o descartar. */
@Entity
@Table(name = "aviso")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proyecto_id")
    private UUID proyectoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private TipoAviso tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private GrupoAviso grupo;

    /** 3 urgente, 2 para hoy, 1 para mirar. */
    @Column(nullable = false)
    private Short prioridad;

    /** Identificador estable: depende del estado de los datos que dispararon el aviso. */
    @Column(nullable = false, columnDefinition = "text")
    private String huella;

    /** Entidad que afecta el aviso, con la forma {@code registro:<uuid>}. */
    @Column(nullable = false, columnDefinition = "text")
    private String entidad;

    /** Datos de origen con los que el motor armó el aviso. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> datos = Map.of();

    @Column(nullable = false, columnDefinition = "text")
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String detalle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private List<Object> motivos = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private List<Object> acciones = List.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private EstadoAviso estado = EstadoAviso.PENDIENTE;

    /** Cuándo se aplicó o descartó; null mientras siga pendiente o haya vencido. */
    @Column(name = "resuelto_en")
    private OffsetDateTime resueltoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
