package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plan.AccesoIa;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quién puede usar la IA ({@code ensambla.ia.acceso.*}).
 *
 * @param restringido {@code ENSAMBLA_IA_ACCESO_RESTRINGIDO}: con {@code true} sólo los emails de la lista; por
 *                    defecto la IA es para todos
 * @param emails      {@code ENSAMBLA_IA_ACCESO_EMAILS}: emails habilitados, separados por coma
 */
@ConfigurationProperties(prefix = "ensambla.ia.acceso")
public record AccesoIaProperties(boolean restringido, List<String> emails) {

    public AccesoIa acceso() {
        return new AccesoIa(restringido, emails == null ? Set.of() : new HashSet<>(emails));
    }
}
