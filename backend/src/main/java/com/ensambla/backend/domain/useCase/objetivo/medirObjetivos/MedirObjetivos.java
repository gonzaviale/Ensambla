package com.ensambla.backend.domain.useCase.objetivo.medirObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Mide ahora los objetivos del negocio de la sesión, sin esperar al monitoreo de cada hora: se pide al abrir los
 * objetivos, para que una venta recién cargada ya se vea en los mini-objetivos y en el gráfico.
 */
@RequiredArgsConstructor
public class MedirObjetivos {

    private final NegocioRepository negocioRepository;

    private final MonitoreoObjetivosService monitoreoObjetivosService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute(MedirObjetivosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        medirEnTransaccion(negocioId, OffsetDateTime.now(clock));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void medirEnTransaccion(NegocioId negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> medir(negocioId, ahora));
    }

    private void medir(NegocioId negocioId, OffsetDateTime ahora) {
        monitoreoObjetivosService.monitorear(negocioId, ahora);
    }
}
