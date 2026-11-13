package com.ensambla.backend.domain.useCase.objetivo.medirObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        transaccionPort.ejecutar(() -> monitoreoObjetivosService.monitorear(negocioId, ahora));
    }
}
