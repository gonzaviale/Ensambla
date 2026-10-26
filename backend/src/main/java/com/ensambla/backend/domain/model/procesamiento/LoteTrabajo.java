package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

public record LoteTrabajo(LoteId id, NegocioId negocioId, BloqueId bloqueDestinoId, ReclamoId reclamoId, int intentos) {
}
