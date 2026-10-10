package com.ensambla.backend.domain.port;

import java.util.UUID;

/** Lee una clave privada del negocio; los bytes deben estar comprimidos y rotados por la subida. */
public interface ArchivoCuadernoPort {

    byte[] leer(UUID negocioId, String clave);
}
