package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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

    @Override
    public Negocio save(Negocio negocio) {
        return negocioRepositoryMapper.toDomain(
                negocioJpaRepository.saveAndFlush(negocioRepositoryMapper.toEntity(negocio)));
    }

    @Override
    public Optional<Negocio> findByDuenioId(UsuarioId usuarioId) {
        return negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor())
                .map(negocioRepositoryMapper::toDomain);
    }

    @Override
    public Optional<Negocio> findByDuenioIdParaActualizar(UsuarioId usuarioId) {
        return negocioJpaRepository.findTopByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor())
                .map(negocioRepositoryMapper::toDomain);
    }

    @Override
    public List<NegocioId> findAllIds() {
        return negocioJpaRepository.findAllIds().stream().map(NegocioId::de).toList();
    }

    @Override
    public Optional<NegocioId> findIdByDuenioId(UsuarioId usuarioId) {
        return negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId.valor())
                .map(negocio -> NegocioId.de(negocio.getId()));
    }
}

