package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hasta {@link CandidatosMiniObjetivo#ALTERNATIVAS} mini-objetivos con los que la persona puede cambiar uno en curso,
 * calculados con los datos de hoy. Un objetivo o un mini-objetivo ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ListarCandidatos {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final Clock clock;

    public ListarCandidatosResponse execute(ListarCandidatosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        objetivoRepository.obtener(negocioId, request.objetivoId());
        MiniObjetivo miniObjetivo = miniObjetivoRepository.obtener(
                negocioId, request.objetivoId(), request.miniObjetivoId());
        miniObjetivo.exigirEnCurso();
        List<MiniObjetivo> enCurso = miniObjetivoRepository
                .findAllByObjetivoIdAndNegocioId(request.objetivoId(), negocioId).stream()
                .filter(MiniObjetivo::estaEnCurso)
                .toList();
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        CandidatosDelNegocio negocio = propuestaMiniObjetivosService.candidatos(negocioId, hoy);
        return ListarCandidatosResponse.de(CandidatosMiniObjetivo.alternativas(
                negocio.candidatos(), miniObjetivo, enCurso, CandidatosMiniObjetivo.ALTERNATIVAS));
    }
}
