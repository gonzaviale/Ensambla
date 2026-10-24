package com.ensambla.backend.domain.useCase.procesamiento.procesarLote;

import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ProcesarLote {

    private final ProcesamientoService procesamientoService;
    private final Clock clock;

    public void execute() {
        procesamientoService.recuperar(OffsetDateTime.now(clock));
        procesamientoService.reclamar(OffsetDateTime.now(clock)).ifPresent(lote ->
                procesamientoService.finalizar(lote,
                        procesamientoService.leer(lote, OffsetDateTime.now(clock)), OffsetDateTime.now(clock)));
    }
}
