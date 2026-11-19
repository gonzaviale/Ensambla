package com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirObjetivoDelNegocio(negocioId, request.objetivoId());
        MiniObjetivo miniObjetivo = obtenerMiniObjetivo(negocioId, request);
        abandonarEnTransaccion(miniObjetivo, OffsetDateTime.now(clock));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirObjetivoDelNegocio(NegocioId negocioId, ObjetivoId objetivoId) {
        objetivoRepository.obtener(negocioId, objetivoId);
    }

    private MiniObjetivo obtenerMiniObjetivo(NegocioId negocioId, AbandonarMiniObjetivoRequest request) {
        return miniObjetivoRepository.obtener(negocioId, request.objetivoId(), request.miniObjetivoId());
    }

    private void abandonarEnTransaccion(MiniObjetivo miniObjetivo, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> abandonar(miniObjetivo, ahora));
    }

    private MiniObjetivo abandonar(MiniObjetivo miniObjetivo, OffsetDateTime ahora) {
        miniObjetivo.abandonar(ahora);
        return miniObjetivoRepository.save(miniObjetivo);
    }
}
