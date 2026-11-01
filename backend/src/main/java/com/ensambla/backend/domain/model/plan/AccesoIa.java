package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.usuario.Usuario;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quién puede usar la IA con costo (lectura de cuadernos, Sami y el taller). Con {@code restringido} sólo los dueños
 * cuyo email está en la lista; sin restringir, todos. Restringido con la lista vacía no deja pasar a nadie.
 */
public record AccesoIa(boolean restringido, Set<String> emails) {

    public static final AccesoIa LIBRE = new AccesoIa(false, Set.of());

    public static final String MENSAJE = "La IA no está habilitada para tu cuenta.";

    public AccesoIa {
        emails = emails == null ? Set.of() : emails.stream()
                .filter(Objects::nonNull)
                .map(Usuario::normalizarEmail)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean permite(String email) {
        return !restringido || (email != null && emails.contains(Usuario.normalizarEmail(email)));
    }
}
