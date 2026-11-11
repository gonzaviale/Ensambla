package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** El id lo asigna el dominio al crear el mini-objetivo (el reemplazo apunta al nuevo antes de guardarlo). */
@Entity
@Table(name = "mini_objetivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniObjetivoEntity {

    @Id
    private UUID id;

    @Column(name = "objetivo_id", nullable = false, updatable = false)
    private UUID objetivoId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private short posicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private TipoMiniObjetivo tipo;

    @Column(name = "registro_id", updatable = false)
    private UUID registroId;

    @Column(columnDefinition = "text", updatable = false)
    private String referencia;

    @Column(nullable = false)
    private BigDecimal meta;

    @Column(name = "valor_inicial", nullable = false, updatable = false)
    private BigDecimal valorInicial;

    @Column(name = "valor_actual", nullable = false)
    private BigDecimal valorActual;

    @Column(name = "aporte_estimado", nullable = false, updatable = false)
    private BigDecimal aporteEstimado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EstadoMiniObjetivo estado;

    @Column(name = "semana_inicio", nullable = false, updatable = false)
    private LocalDate semanaInicio;

    @Column(nullable = false, columnDefinition = "text")
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String justificacion;

    @Column(name = "texto_con_ia", nullable = false)
    private boolean textoConIa;

    @Column(name = "reemplazado_por")
    private UUID reemplazadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
