package com.ensambla.backend.domain.useCase.objetivo.crearObjetivo;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Guarda el objetivo y le propone hasta 3 mini-objetivos con los números reales del negocio: elige entre las palancas
 * con datos, sin repetir tipo ni producto. Si el resultado de hoy ya llega a la meta, el objetivo nace alcanzado y sin
 * mini-objetivos; si no hay datos para proponer, guarda los que haya (quizás ninguno) y no inventa.
 * <p>
 * La IA sólo redacta el texto, nunca el número, y se consulta antes de abrir la transacción. Sin IA (apagada, sin
 * plan, sin cupo o si falla) cada mini-objetivo lleva su texto de reserva.
 */
@Slf4j
@RequiredArgsConstructor
public class CrearObjetivo {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final ControlPlanService controlPlanService;

    private final ConsumoSamiRepository consumoSamiRepository;

    private final RedaccionObjetivosIaPort redaccionObjetivosIaPort;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    private final boolean iaHabilitada;

    private final int limiteDiarioIa;

    public ObjetivoItem execute(CrearObjetivoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LocalDate hoy = ZonaNegocio.dia(ahora);
        CandidatosDelNegocio negocio = propuestaMiniObjetivosService.candidatos(negocioId, hoy);
        boolean alcanzado = negocio.alcanza(request.meta());
        List<CandidatoMiniObjetivo> elegidos = alcanzado
                ? List.of()
                : redactar(negocioId, CandidatosMiniObjetivo.elegir(
                        negocio.candidatos(), List.of(), MiniObjetivo.MAXIMO_EN_CURSO), ahora);
        Objetivo objetivo = transaccionPort.ejecutar(() -> crear(negocioId, request, alcanzado, elegidos, hoy, ahora));
        return ObjetivoItem.de(objetivo);
    }

    private Objetivo crear(
            NegocioId negocioId, CrearObjetivoRequest request, boolean alcanzado, List<CandidatoMiniObjetivo> elegidos,
            LocalDate hoy, OffsetDateTime ahora) {
        Objetivo nuevo = Objetivo.nuevo(negocioId, request.texto(), request.meta(), ahora);
        if (alcanzado) {
            nuevo.marcarAlcanzado(ahora);
        }
        Objetivo guardado = objetivoRepository.save(nuevo);
        MiniObjetivo.propuestos(guardado, elegidos, SemanaObjetivo.lunesDe(hoy), ahora)
                .forEach(miniObjetivoRepository::save);
        return guardado;
    }

    /** Los mismos candidatos con el texto de la IA; con cualquier problema, con su texto de reserva. */
    private List<CandidatoMiniObjetivo> redactar(
            NegocioId negocioId, List<CandidatoMiniObjetivo> elegidos, OffsetDateTime ahora) {
        if (!iaHabilitada || elegidos.isEmpty()) {
            return elegidos;
        }
        try {
            controlPlanService.exigirIa(negocioId);
            LocalDate fecha = ZonaNegocio.dia(ahora);
            if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiarioIa, ahora)) {
                return elegidos;
            }
            RedaccionObjetivosIa redaccion = redaccionObjetivosIaPort.redactar(elegidos);
            consumoSamiRepository.registrar(negocioId, fecha, redaccion.consumo(), ahora);
            return IntStream.range(0, elegidos.size())
                    .mapToObj(indice -> elegidos.get(indice).conTextoDeIa(redaccion.textos().get(indice)))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("No se pudieron redactar los mini-objetivos con IA; se usa el texto de reserva", e);
            return elegidos;
        }
    }
}
