package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NegocioRepositoryImpl implements NegocioRepository {

    private final INegocioJpaRepository negocioJpaRepository;
    private final NegocioRepositoryMapper negocioRepositoryMapper;

    private final NegocioActivoPort negocioActivoPort;

    @Override
    public Negocio save(Negocio negocio) {
        return negocioRepositoryMapper.toDomain(
                negocioJpaRepository.saveAndFlush(negocioRepositoryMapper.toEntity(negocio)));
    }

    @Override
    public Optional<Negocio> findByDuenioId(UsuarioId usuarioId) {
        return findActivo(usuarioId)
                .map(negocioRepositoryMapper::toDomain);
    }

    @Override
    public Optional<Negocio> findByDuenioIdParaActualizar(UsuarioId usuarioId) {
        return findActivoParaActualizar(usuarioId)
                .map(negocioRepositoryMapper::toDomain);
    }

    @Override
    public List<Negocio> findAllByDuenioId(UsuarioId usuarioId) {
        return negocioJpaRepository.findAllByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor()).stream()
                .map(negocioRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public List<Negocio> findAllByDuenioIdParaActualizar(UsuarioId usuarioId) {
        return negocioJpaRepository.findAllByDuenioIdParaActualizar(usuarioId.valor()).stream()
                .map(negocioRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Negocio> findByIdAndDuenioId(NegocioId negocioId, UsuarioId usuarioId) {
        return negocioJpaRepository.findByIdAndDuenioId(negocioId.valor(), usuarioId.valor())
                .map(negocioRepositoryMapper::toDomain);
    }

    @Override
    public Optional<ZonaId> findZonaIdById(NegocioId negocioId) {
        return negocioJpaRepository.findById(negocioId.valor())
                .map(NegocioEntity::getZonaId)
                .map(ZonaId::de);
    }

    @Override
    public Optional<UsuarioId> findDuenioIdById(NegocioId negocioId) {
        return negocioJpaRepository.findDuenioIdById(negocioId.valor()).map(UsuarioId::de);
    }

    @Override
    public Optional<String> findEmailDuenioById(NegocioId negocioId) {
        return negocioJpaRepository.findEmailDuenioById(negocioId.valor());
    }

    @Override
    public List<NegocioId> findAllIds() {
        return negocioJpaRepository.findAllIds().stream().map(NegocioId::de).toList();
    }

    @Override
    public Optional<NegocioId> findIdByDuenioId(UsuarioId usuarioId) {
        return findActivo(usuarioId)
                .map(negocio -> NegocioId.de(negocio.getId()));
    }

    @Override
    public void deleteById(NegocioId negocioId) {
        negocioJpaRepository.deleteById(negocioId.valor());
        negocioJpaRepository.flush();
    }

    private Optional<NegocioEntity> findActivo(UsuarioId usuarioId) {
        Optional<NegocioEntity> activo = negocioActivoPort.obtener(usuarioId)
                .flatMap(id -> negocioJpaRepository.findByIdAndDuenioId(id.valor(), usuarioId.valor()));
        return activo.isPresent() ? activo : negocioJpaRepository
                .findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor());
    }

    private Optional<NegocioEntity> findActivoParaActualizar(UsuarioId usuarioId) {
        Optional<NegocioEntity> activo = negocioActivoPort.obtener(usuarioId)
                .flatMap(id -> negocioJpaRepository.findByIdAndDuenioIdParaActualizar(
                        id.valor(), usuarioId.valor()));
        if (activo.isPresent()) {
            return activo;
        }
        return negocioJpaRepository.findTopByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor());
    }
}

