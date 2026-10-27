package com.ensambla.backend.domain.model.common;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * El id de una entidad, con su tipo: un {@code ProyectoId} no se puede pasar donde va un {@code BloqueId}. Cada
 * implementación es un record que envuelve el UUID y ofrece {@code de(UUID)}, {@code de(String)} y {@code nuevo()};
 * su {@code toString()} es el UUID, así el id se lee igual en textos, claves y JSON.
 */
public interface Identificador {

    UUID valor();

    /** El UUID de un id que puede faltar. */
    static UUID valorDe(Identificador id) {
        return id == null ? null : id.valor();
    }

    /** Los UUID de varios ids, para consultar por todos juntos. */
    static List<UUID> valores(Collection<? extends Identificador> ids) {
        return ids.stream().map(Identificador::valor).toList();
    }

    /** El id tipado de un UUID que puede faltar. */
    static <T extends Identificador> T opcional(UUID valor, Function<UUID, T> de) {
        return valor == null ? null : de.apply(valor);
    }
}
