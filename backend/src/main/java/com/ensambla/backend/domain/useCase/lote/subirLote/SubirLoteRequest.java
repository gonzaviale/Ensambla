package com.ensambla.backend.domain.useCase.lote.subirLote;

import java.util.List;
import java.util.UUID;

/**
 * @param fotos           bytes de cada archivo, en el orden en que llegaron
 * @param bloqueDestinoId bloque del negocio al que van las filas; {@code null} si la IA debe proponer uno
 */
public record SubirLoteRequest(UUID usuarioId, List<byte[]> fotos, UUID bloqueDestinoId) {
}
