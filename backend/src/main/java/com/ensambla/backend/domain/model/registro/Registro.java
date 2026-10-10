package com.ensambla.backend.domain.model.registro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registro {

    private UUID id;

    private UUID negocioId;

    private UUID bloqueId;

    /** Valores cargados, indexados por el id del campo (así renombrar un campo no rompe los datos). */
    private Map<String, Object> valores;

    /** Foto del cuaderno de la que salió el registro; {@code null} si se cargó a mano. */
    private UUID fotoOrigenId;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
