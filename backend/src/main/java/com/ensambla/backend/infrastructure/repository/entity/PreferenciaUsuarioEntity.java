package com.ensambla.backend.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lo que la persona decidió sobre los avisos y la bienvenida; viaja con la cuenta. */
@Entity
@Table(name = "preferencia_usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaUsuarioEntity {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /** Huellas de los avisos que descartó. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avisos_descartados", nullable = false)
    @Builder.Default
    private List<String> avisosDescartados = List.of();

    /** Grupos de avisos que apagó (por defecto todos están prendidos). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grupos_avisos", nullable = false)
    @Builder.Default
    private Map<String, Boolean> gruposAvisos = Map.of();

    @Column(name = "bienvenida_vista", nullable = false)
    @Builder.Default
    private Boolean bienvenidaVista = false;

    /** Cómo quiere que Sami lo llame; si falta, se usa el nombre de la cuenta. */
    @Column(name = "nombre_preferido", columnDefinition = "text")
    private String nombrePreferido;

    /** Si pidió que Sami le mande por WhatsApp el resumen de la mañana. */
    @Column(name = "avisos_por_whatsapp", nullable = false)
    @Builder.Default
    private Boolean avisosPorWhatsapp = false;

    /** El último día (argentino) en que se le mandó el resumen; sin valor si nunca. */
    @Column(name = "resumen_whatsapp_enviado_el")
    private LocalDate resumenWhatsappEnviadoEl;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
