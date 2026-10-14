package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.SubirLoteRequest;
import com.ensambla.backend.domain.mapper.lote.SubirLoteMapper;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class SubirLoteUseCase {

    private final NegocioService negocioService;

    private final LoteSubidaService loteSubidaService;

    private final BloqueService bloqueService;

    private final SubirLoteMapper subirLoteMapper;

    private final Clock clock;

    public LoteItem execute(SubirLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        validarDestino(negocioId, request.bloqueDestinoId());
        LoteSubida lote = loteSubidaService.subir(
                negocioId, request.fotos(), request.bloqueDestinoId(), OffsetDateTime.now(clock));
        return subirLoteMapper.toResponse(lote);
    }

    /** Un bloque de otro negocio responde como inexistente. */
    private void validarDestino(UUID negocioId, UUID bloqueDestinoId) {
        if (bloqueDestinoId != null) {
            bloqueService.obtener(bloqueDestinoId, negocioId);
        }
    }
}
