package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProyectoRepositoryImpl implements ProyectoRepository {

    private final IProyectoJpaRepository proyectoJpaRepository;

    private final IBloqueJpaRepository bloqueJpaRepository;

    private final ICampoJpaRepository campoJpaRepository;

    private final ProyectoRepositoryMapper proyectoRepositoryMapper;

    @Override
    public List<Proyecto> findAllByNegocioId(NegocioId negocioId) {
        return proyectoJpaRepository.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Proyecto> findByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId) {
        return proyectoJpaRepository.findByIdAndNegocioId(proyectoId.valor(), negocioId.valor())
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
    public void deleteByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId) {
        proyectoJpaRepository.deleteByIdAndNegocioId(proyectoId.valor(), negocioId.valor());
    }

    private Proyecto cargarEstructura(Proyecto proyecto) {
        List<Bloque> bloques = bloqueJpaRepository
                .findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyecto.getId().valor(), proyecto
                .getNegocioId().valor())
                .stream()
                .map(proyectoRepositoryMapper::toDomain)
                .toList();
        List<BloqueId> bloqueIds = bloques.stream().map(Bloque::getId).toList();
        Map<BloqueId, List<Campo>> campos = campoJpaRepository
                .findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(Identificador.valores(bloqueIds), proyecto
                .getNegocioId().valor())
                .stream()
                .map(proyectoRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Campo::getBloqueId));
        bloques.forEach(bloque -> bloque.setCampos(campos.getOrDefault(bloque.getId(), List.of())));
        proyecto.setBloques(bloques);
        return proyecto;
    }

    private Bloque guardarBloque(Bloque bloque, ProyectoId proyectoId) {
        BloqueEntity entity = proyectoRepositoryMapper.toEntity(bloque);
        entity.setProyectoId(proyectoId.valor());
        Bloque guardado = proyectoRepositoryMapper.toDomain(bloqueJpaRepository.save(entity));
        guardado.setCampos(bloque.getCampos().stream()
                .map(campo -> guardarCampo(campo, guardado.getId()))
                .toList());
        return guardado;
    }

    private Campo guardarCampo(Campo campo, BloqueId bloqueId) {
        CampoEntity entity = proyectoRepositoryMapper.toEntity(campo);
        entity.setBloqueId(bloqueId.valor());
        return proyectoRepositoryMapper.toDomain(campoJpaRepository.save(entity));
    }
}

