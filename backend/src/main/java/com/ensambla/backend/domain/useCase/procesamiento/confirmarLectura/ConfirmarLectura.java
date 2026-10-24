package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ConfirmacionLecturaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmarLectura {

    private final NegocioService negocioService;
    private final LoteSubidaService loteSubidaService;
    private final ConfirmacionLecturaService confirmacionLecturaService;
    private final Clock clock;

    public ConfirmarLecturaResponse execute(ConfirmarLecturaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        LoteSubida lote = loteSubidaService.obtener(negocioId, request.loteId());
        ConfirmacionGuardada guardada = confirmacionLecturaService.confirmar(
                negocioId, lote, request.aConfirmacionLectura(), OffsetDateTime.now(clock));
        return ConfirmarLecturaResponse.de(guardada);
    }
}
