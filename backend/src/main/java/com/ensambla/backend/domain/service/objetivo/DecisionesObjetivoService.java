package com.ensambla.backend.domain.service.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Anota qué decidió la persona con un aviso en cada objetivo activo del negocio. Lo comparten los casos de uso que
 * aplican y los que descartan avisos, que lo llaman dentro de su propia transacción: la decisión y el aviso se guardan
 * juntos o no se guarda ninguno.
 */
@RequiredArgsConstructor
public class DecisionesObjetivoService {

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final DecisionObjetivoRepository decisionObjetivoRepository;

    /**
     * Guarda una decisión por cada objetivo del negocio que todavía no llegó a su meta, vinculada al mini-objetivo en
     * curso del producto del aviso si hay uno. Sin objetivos activos no guarda nada.
     *
     * @param tipo {@link TipoDecision#AVISO_APLICADO} o {@link TipoDecision#AVISO_DESCARTADO}
     */
    public void registrarAviso(Aviso aviso, TipoDecision tipo, OffsetDateTime ahora) {
        objetivoRepository.findAllByNegocioId(aviso.getNegocioId()).stream()
                .filter(objetivo -> !objetivo.estaAlcanzado())
                .forEach(objetivo -> decisionObjetivoRepository.save(DecisionObjetivo.deAviso(
                        objetivo, aviso, tipo, miniDelProducto(objetivo, aviso).orElse(null), ahora)));
    }

    private Optional<MiniObjetivo> miniDelProducto(Objetivo objetivo, Aviso aviso) {
        return aviso.productoId().flatMap(productoId ->
                miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), objetivo.getNegocioId())
                        .stream()
                        .filter(MiniObjetivo::estaEnCurso)
                        .filter(mini -> productoId.equals(mini.getRegistroId()))
                        .findFirst());
    }
}
