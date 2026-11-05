package com.ensambla.backend.domain.model.aviso;

import java.util.List;
import java.util.Optional;

/**
 * Una parte del resumen de la mañana. Cada una mira los avisos pendientes y dice lo suyo en una línea, o nada si no
 * tiene qué contar. Para sumar una parte alcanza con una clase nueva registrada como bean: el resumen recibe todas.
 */
public interface SeccionResumen {

    Optional<String> texto(List<Aviso> pendientes);
}
