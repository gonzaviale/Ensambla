package com.ensambla.backend.domain.model.promocion;

import java.util.Optional;

/**
 * Una idea para promocionar. Cada regla mira lo que vende el negocio y propone una promoción, o nada si no aplica.
 * Para sumar una idea alcanza con una clase nueva registrada como bean: el caso de uso recibe todas las que haya.
 */
public interface ReglaPromocion {

    Optional<Promocion> evaluar(ContextoPromocion contexto);
}
