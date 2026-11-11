package com.ensambla.backend.domain.useCase.objetivo.crearObjetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearObjetivo {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ObjetivoItem execute(CrearObjetivoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Objetivo objetivo = transaccionPort.ejecutar(() -> crear(negocioId, request, ahora));
        return ObjetivoItem.de(objetivo);
    }

    private Objetivo crear(NegocioId negocioId, CrearObjetivoRequest request, OffsetDateTime ahora) {
        return objetivoRepository.save(Objetivo.nuevo(negocioId, request.texto(), request.meta(), ahora));
    }
}
