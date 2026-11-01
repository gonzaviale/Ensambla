package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/**
 * @param fotos           bytes de cada archivo, en el orden en que llegaron
 * @param bloqueDestinoId bloque del negocio al que van las filas; {@code null} si la IA debe proponer uno
 */
public record SubirLoteRequest(UsuarioId usuarioId, List<byte[]> fotos, BloqueId bloqueDestinoId) {
}
