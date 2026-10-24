package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class SubirLote {

    private final NegocioService negocioService;

    private final LoteSubidaService loteSubidaService;

    private final BloqueService bloqueService;

    private final Clock clock;

    public LoteItem execute(SubirLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        validarDestino(negocioId, request.bloqueDestinoId());
        LoteSubida lote = loteSubidaService.subir(
                negocioId, request.fotos(), request.bloqueDestinoId(), OffsetDateTime.now(clock));
        return LoteItem.de(lote);
    }

    /** Un bloque de otro negocio responde como inexistente. */
    private void validarDestino(UUID negocioId, UUID bloqueDestinoId) {
        if (bloqueDestinoId != null) {
            bloqueService.obtener(bloqueDestinoId, negocioId);
        }
    }
}
