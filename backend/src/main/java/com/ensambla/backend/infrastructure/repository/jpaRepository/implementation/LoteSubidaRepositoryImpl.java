package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.LoteSubidaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoteSubidaRepositoryImpl implements LoteSubidaRepository {

    private final ILoteSubidaJpaRepository loteSubidaJpaRepository;

    private final IFotoCuadernoJpaRepository fotoCuadernoJpaRepository;

    private final LoteSubidaRepositoryMapper loteSubidaRepositoryMapper;

    @Override
    public LoteSubida saveConFotos(LoteSubida lote) {
        LoteSubida guardado = loteSubidaRepositoryMapper.toDomain(
                loteSubidaJpaRepository.saveAndFlush(loteSubidaRepositoryMapper.toEntity(lote)));
        List<FotoCuadernoEntity> fotos = lote.getFotos().stream()
                .map(foto -> aEntity(foto, guardado.getId()))
                .toList();
        guardado.setFotos(fotoCuadernoJpaRepository.saveAllAndFlush(fotos).stream()
                .map(loteSubidaRepositoryMapper::toDomain)
                .toList());
        return guardado;
    }

    @Override
    public boolean confirmar(UUID loteId, UUID negocioId, UUID bloqueDestinoId, OffsetDateTime ahora) {
        return loteSubidaJpaRepository.cambiarEstado(
                loteId, negocioId, EstadoLote.LISTO, EstadoLote.CONFIRMADO, bloqueDestinoId, ahora) == 1;
    }

    @Override
    public Optional<FotoCuaderno> findFotoByIdAndNegocioId(UUID fotoId, UUID negocioId) {
        return fotoCuadernoJpaRepository.findByIdAndNegocioId(fotoId, negocioId)
                .map(loteSubidaRepositoryMapper::toDomain);
    }

    @Override
    public Optional<LoteSubida> findByIdAndNegocioId(UUID loteId, UUID negocioId) {
        return loteSubidaJpaRepository.findByIdAndNegocioId(loteId, negocioId)
                .map(loteSubidaRepositoryMapper::toDomain)
                .map(this::cargarFotos);
    }

    private FotoCuadernoEntity aEntity(FotoCuaderno foto, UUID loteId) {
        FotoCuadernoEntity entity = loteSubidaRepositoryMapper.toEntity(foto);
        entity.setLoteId(loteId);
        return entity;
    }

    private LoteSubida cargarFotos(LoteSubida lote) {
        lote.setFotos(fotoCuadernoJpaRepository
                .findAllByLoteIdAndNegocioIdOrderByPosicionAsc(lote.getId(), lote.getNegocioId())
                .stream()
                .map(loteSubidaRepositoryMapper::toDomain)
                .toList());
        return lote;
    }
}
