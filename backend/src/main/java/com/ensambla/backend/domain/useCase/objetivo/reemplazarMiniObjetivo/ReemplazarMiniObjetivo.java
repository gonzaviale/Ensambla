package com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * La persona cambia un mini-objetivo en curso por otro candidato. El elegido tiene que seguir siendo una alternativa
 * con los datos de hoy; el nuevo ocupa el mismo lugar y el viejo queda como reemplazado, con su historia.
 */
@RequiredArgsConstructor
public class ReemplazarMiniObjetivo {

    private static final String YA_NO_ESTA = "Ese mini-objetivo ya no está disponible: elegí otro de la lista";

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public MiniObjetivoItem execute(ReemplazarMiniObjetivoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Objetivo objetivo = objetivoRepository.obtener(negocioId, request.objetivoId());
        MiniObjetivo actual = miniObjetivoRepository.obtener(negocioId, request.objetivoId(), request.miniObjetivoId());
        actual.exigirEnCurso();
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LocalDate hoy = ZonaNegocio.dia(ahora);
        CandidatoMiniObjetivo elegido = elegir(negocioId, actual, request, hoy);
        return MiniObjetivoItem.de(transaccionPort.ejecutar(() -> reemplazar(objetivo, actual, elegido, hoy, ahora)));
    }

    private CandidatoMiniObjetivo elegir(
            NegocioId negocioId, MiniObjetivo actual, ReemplazarMiniObjetivoRequest request, LocalDate hoy) {
        List<MiniObjetivo> enCurso = miniObjetivoRepository
                .findAllByObjetivoIdAndNegocioId(request.objetivoId(), negocioId).stream()
                .filter(MiniObjetivo::estaEnCurso)
                .toList();
        return CandidatosMiniObjetivo.alternativas(
                        propuestaMiniObjetivosService.candidatos(negocioId, hoy).candidatos(),
                        actual, enCurso, CandidatosMiniObjetivo.TODOS).stream()
                .filter(candidato -> candidato.esElMismo(request.tipo(), request.registroId(), request.referencia()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(YA_NO_ESTA));
    }

    /** Primero se libera el lugar del viejo y recién después entra el nuevo: un lugar lo ocupa uno solo. */
    private MiniObjetivo reemplazar(
            Objetivo objetivo, MiniObjetivo actual, CandidatoMiniObjetivo elegido, LocalDate hoy,
            OffsetDateTime ahora) {
        MiniObjetivo nuevo = MiniObjetivo.nuevo(
                objetivo, actual.getPosicion(), elegido, SemanaObjetivo.lunesDe(hoy), ahora);
        actual.reemplazarPor(nuevo.getId(), ahora);
        miniObjetivoRepository.save(actual);
        return miniObjetivoRepository.save(nuevo);
    }
}
