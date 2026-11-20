package com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mide en silencio, cada hora, cómo viene cada objetivo de todos los negocios ({@link MonitoreoObjetivosService}).
 * <p>
 * No recibe ningún puerto que avise o mande mensajes: lo que deja sólo se ve cuando la persona entra a la app. Cada
 * negocio va en su propia transacción y uno con error no frena a los demás.
 */
@Slf4j
@RequiredArgsConstructor
public class MonitorearObjetivos {

    private final ObjetivoRepository objetivoRepository;

    private final MonitoreoObjetivosService monitoreoObjetivosService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Cuántos negocios se piden por vez, para no cargar a todos juntos. */
    private final int tamanoTanda;

    public void execute() {
        int pagina = 0;
        List<NegocioId> tanda;
        do {
            tanda = obtenerTanda(pagina++);
            tanda.forEach(this::monitorearNegocio);
        } while (tanda.size() == tamanoTanda);
    }

    private List<NegocioId> obtenerTanda(int pagina) {
        return objetivoRepository.findNegocioIdsConObjetivos(pagina, tamanoTanda);
    }

    private void monitorearNegocio(NegocioId negocioId) {
        try {
            monitorearEnTransaccion(negocioId, OffsetDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("No se pudo monitorear los objetivos del negocio {}", negocioId, e);
        }
    }

    private void monitorearEnTransaccion(NegocioId negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> monitorear(negocioId, ahora));
    }

    private void monitorear(NegocioId negocioId, OffsetDateTime ahora) {
        monitoreoObjetivosService.monitorear(negocioId, ahora);
    }
}
