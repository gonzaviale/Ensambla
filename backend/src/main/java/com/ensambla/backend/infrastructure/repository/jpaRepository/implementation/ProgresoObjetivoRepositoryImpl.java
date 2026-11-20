package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProgresoObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ProgresoObjetivoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProgresoObjetivoRepositoryImpl implements ProgresoObjetivoRepository {

    private final IProgresoObjetivoJpaRepository progresoObjetivoJpaRepository;

    private final ProgresoObjetivoRepositoryMapper progresoObjetivoRepositoryMapper;

    @Override
    public void registrar(ProgresoObjetivo progreso) {
        progresoObjetivoJpaRepository.registrar(
                progreso.id().valor(),
                progreso.objetivoId().valor(),
                Identificador.valorDe(progreso.miniObjetivoId()),
                progreso.dia(),
                progreso.valor(),
                progreso.medidoEn());
    }

    @Override
    public List<ProgresoObjetivo> findAllByObjetivoId(ObjetivoId objetivoId) {
        return progresoObjetivoJpaRepository.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivoId.valor()).stream()
                .map(progresoObjetivoRepositoryMapper::toDomain)
                .toList();
    }
}
