package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campo {

    private UUID id;

    private UUID negocioId;

    private UUID bloqueId;

    private String nombre;

    private TipoCampo tipo;

    private Boolean obligatorio;

    /** Valor inicial serializado como texto ("12", "true", "2026-10-09"); se interpreta según el tipo. */
    private String valorInicial;

    /** Opciones válidas; sólo aplica a campos de tipo OPCION. */
    private List<String> opciones;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
