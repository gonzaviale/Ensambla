package com.ensambla.backend.domain.dto.lote;

import java.util.List;
import java.util.UUID;

/** @param fotos bytes de cada archivo, en el orden en que llegaron */
public record SubirLoteRequest(UUID usuarioId, List<byte[]> fotos) {
}
