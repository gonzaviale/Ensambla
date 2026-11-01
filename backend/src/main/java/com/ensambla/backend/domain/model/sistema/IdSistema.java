package com.ensambla.backend.domain.model.sistema;

import com.ensambla.backend.domain.model.common.Identificador;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Los ids de un sistema los pone la pantalla (por ejemplo {@code e<uuid>}) y la base guarda UUID. Esta regla es
 * determinística: el mismo id del front da siempre el mismo UUID, así guardar dos veces el mismo sistema reemplaza
 * en vez de duplicar.
 */
public final class IdSistema {

    private static final Pattern UUID_ESTRICTO =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private IdSistema() {
    }

    /**
     * El id de un id del front: el propio UUID, el UUID con prefijo {@code e}, o uno derivado del {@code ambito} (el
     * negocio, o el bloque para sus campos) y el id.
     */
    public static <T extends Identificador> T aId(Identificador ambito, String idDelFront, Function<UUID, T> tipo) {
        String id = idDelFront.strip();
        if (UUID_ESTRICTO.matcher(id).matches()) {
            return tipo.apply(UUID.fromString(id));
        }
        if (id.startsWith("e") && UUID_ESTRICTO.matcher(id.substring(1)).matches()) {
            return tipo.apply(UUID.fromString(id.substring(1)));
        }
        return tipo.apply(UUID.nameUUIDFromBytes((ambito + ":" + id).getBytes(StandardCharsets.UTF_8)));
    }

    /** Lo que ve la pantalla: el UUID tal cual. */
    public static String aFront(Identificador id) {
        return id.toString();
    }
}
