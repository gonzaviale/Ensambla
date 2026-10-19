package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.enums.AnchoBloque;
import com.ensambla.backend.domain.model.enums.ColorBloque;
import com.ensambla.backend.domain.model.enums.TipoBloque;
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
@Table(name = "bloque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proyecto_id", nullable = false, updatable = false)
    private UUID proyectoId;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(nullable = false)
    private Short posicion;

    @Column(name = "seccion_id")
    private UUID seccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private TipoBloque tipo = TipoBloque.FORMULARIO;

    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private String descripcion = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private ColorBloque color = ColorBloque.CELESTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private AnchoBloque ancho = AnchoBloque.MEDIO;

    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private String contenido = "";

    @Column(name = "empieza_fila", nullable = false)
    @Builder.Default
    private Boolean empiezaFila = false;

    /** +1 suma stock (entradas), -1 lo descuenta (salidas y ventas); null si el bloque no mueve stock. */
    @Column(name = "direccion_stock")
    private Short direccionStock;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
