package com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/** La persona quita un mini-objetivo en curso: queda en el historial y su lugar queda libre. */
@RequiredArgsConstructor
public class AbandonarMiniObjetivo {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute(AbandonarMiniObjetivoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        objetivoRepository.obtener(negocioId, request.objetivoId());
        MiniObjetivo miniObjetivo = miniObjetivoRepository.obtener(
                negocioId, request.objetivoId(), request.miniObjetivoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        transaccionPort.ejecutar(() -> abandonar(miniObjetivo, ahora));
    }

    private MiniObjetivo abandonar(MiniObjetivo miniObjetivo, OffsetDateTime ahora) {
        miniObjetivo.abandonar(ahora);
        return miniObjetivoRepository.save(miniObjetivo);
    }
}
