package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IReajusteMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IResumenSemanalJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ResumenSemanalRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResumenSemanalRepositoryImpl implements ResumenSemanalRepository {

    private static final String SEMANA_YA_CERRADA = "ux_resumen_semana";

    private final IResumenSemanalJpaRepository resumenSemanalJpaRepository;

    private final IReajusteMiniObjetivoJpaRepository reajusteMiniObjetivoJpaRepository;

    private final ResumenSemanalRepositoryMapper resumenSemanalRepositoryMapper;

    @Override
    public ResumenSemanal save(ResumenSemanal resumen) {
        guardar(resumenSemanalRepositoryMapper.toEntity(resumen));
        reajusteMiniObjetivoJpaRepository.saveAllAndFlush(resumen.reajustes().stream()
                .map(reajuste -> resumenSemanalRepositoryMapper.toEntity(resumen.id(), reajuste))
                .toList());
        return resumen;
    }

    @Override
    public List<ResumenSemanal> findAllByObjetivoId(ObjetivoId objetivoId) {
        List<ResumenSemanalEntity> resumenes =
                resumenSemanalJpaRepository.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivoId.valor());
        if (resumenes.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ReajusteMiniObjetivoEntity>> reajustes = reajusteMiniObjetivoJpaRepository
                .findAllByResumenIdInOrderByCreadoEnAscIdAsc(resumenes.stream().map(ResumenSemanalEntity::getId)
                        .toList()).stream()
                .collect(Collectors.groupingBy(ReajusteMiniObjetivoEntity::getResumenId));
        return resumenes.stream()
                .map(resumen -> resumenSemanalRepositoryMapper.toDomain(
                        resumen, reajustes.getOrDefault(resumen.getId(), List.of())))
                .toList();
    }

    /** Otra corrida que cerró la misma semana antes choca con el índice único: se informa como duplicado. */
    private void guardar(ResumenSemanalEntity entity) {
        try {
            resumenSemanalJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (String.valueOf(exception.getMostSpecificCause().getMessage()).contains(SEMANA_YA_CERRADA)) {
                throw new DuplicateResourceException("Esa semana del objetivo ya está cerrada");
            }
            throw exception;
        }
    }
}
