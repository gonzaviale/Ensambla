package com.ensambla.backend.infrastructure.repository.entity;

import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/** La marca del negocio: logo, dos colores, tipografía y tono. Un kit por negocio. */
@Entity
@Table(name = "kit_marca")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitDeMarcaEntity {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "color_principal", nullable = false, columnDefinition = "text")
    private String colorPrincipal;

    @Column(name = "color_secundario", nullable = false, columnDefinition = "text")
    private String colorSecundario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private Tipografia tipografia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private TonoDeMarca tono;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(columnDefinition = "bytea")
    private byte[] logo;

    @Column(name = "logo_tipo_mime", columnDefinition = "text")
    private String logoTipoMime;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
