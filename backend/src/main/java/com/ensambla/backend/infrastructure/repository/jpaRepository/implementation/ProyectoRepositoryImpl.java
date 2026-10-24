package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ProyectoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProyectoRepositoryImpl implements ProyectoRepository {

    private final IProyectoJpaRepository proyectoJpaRepository;

    private final IBloqueJpaRepository bloqueJpaRepository;

    private final ICampoJpaRepository campoJpaRepository;

    private final ProyectoRepositoryMapper proyectoRepositoryMapper;

    @Override
    public List<Proyecto> findAllByNegocioId(UUID negocioId) {
        return proyectoJpaRepository.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Proyecto> findByIdAndNegocioId(UUID proyectoId, UUID negocioId) {
        return proyectoJpaRepository.findByIdAndNegocioId(proyectoId, negocioId)
                .map(proyectoRepositoryMapper::toDomain)
                .map(this::cargarEstructura);
    }

    @Override
    public Proyecto save(Proyecto proyecto) {
        Proyecto guardado = proyectoRepositoryMapper.toDomain(
                proyectoJpaRepository.save(proyectoRepositoryMapper.toEntity(proyecto)));
        guardado.setBloques(proyecto.getBloques());
        return guardado;
    }

    @Override
    public Proyecto saveConEstructura(Proyecto proyecto) {
        Proyecto guardado = save(proyecto);
        guardado.setBloques(proyecto.getBloques().stream()
                .map(bloque -> guardarBloque(bloque, guardado.getId()))
                .toList());
        return guardado;
    }

    @Override
    public void deleteByIdAndNegocioId(UUID proyectoId, UUID negocioId) {
        proyectoJpaRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }

    private Proyecto cargarEstructura(Proyecto proyecto) {
        List<Bloque> bloques = bloqueJpaRepository
                .findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyecto.getId(), proyecto.getNegocioId())
                .stream()
                .map(proyectoRepositoryMapper::toDomain)
                .toList();
        List<UUID> bloqueIds = bloques.stream().map(Bloque::getId).toList();
        Map<UUID, List<Campo>> campos = campoJpaRepository
                .findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(bloqueIds, proyecto.getNegocioId())
                .stream()
                .map(proyectoRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Campo::getBloqueId));
        bloques.forEach(bloque -> bloque.setCampos(campos.getOrDefault(bloque.getId(), List.of())));
        proyecto.setBloques(bloques);
        return proyecto;
    }

    private Bloque guardarBloque(Bloque bloque, UUID proyectoId) {
        BloqueEntity entity = proyectoRepositoryMapper.toEntity(bloque);
        entity.setProyectoId(proyectoId);
        Bloque guardado = proyectoRepositoryMapper.toDomain(bloqueJpaRepository.save(entity));
        guardado.setCampos(bloque.getCampos().stream()
                .map(campo -> guardarCampo(campo, guardado.getId()))
                .toList());
        return guardado;
    }

    private Campo guardarCampo(Campo campo, UUID bloqueId) {
        CampoEntity entity = proyectoRepositoryMapper.toEntity(campo);
        entity.setBloqueId(bloqueId);
        return proyectoRepositoryMapper.toDomain(campoJpaRepository.save(entity));
    }
}

