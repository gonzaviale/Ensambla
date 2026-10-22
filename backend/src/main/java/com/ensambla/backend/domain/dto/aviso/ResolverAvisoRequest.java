package com.ensambla.backend.domain.dto.aviso;

import java.util.UUID;

/** Pedido de aplicar o descartar un aviso del negocio del usuario. */
public record ResolverAvisoRequest(UUID usuarioId, UUID avisoId) {
}
