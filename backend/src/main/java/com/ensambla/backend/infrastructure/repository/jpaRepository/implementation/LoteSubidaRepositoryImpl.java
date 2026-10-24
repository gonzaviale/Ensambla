package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
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
    public boolean confirmar(LoteId loteId, NegocioId negocioId, BloqueId bloqueDestinoId, OffsetDateTime ahora) {
        return loteSubidaJpaRepository.cambiarEstado(
                loteId.valor(), negocioId.valor(), EstadoLote.LISTO, EstadoLote.CONFIRMADO, Identificador
                        .valorDe(bloqueDestinoId), ahora) == 1;
    }

    @Override
    public Optional<FotoCuaderno> findFotoByIdAndNegocioId(FotoId fotoId, NegocioId negocioId) {
        return fotoCuadernoJpaRepository.findByIdAndNegocioId(fotoId.valor(), negocioId.valor())
                .map(loteSubidaRepositoryMapper::toDomain);
    }

    @Override
    public Optional<LoteSubida> findByIdAndNegocioId(LoteId loteId, NegocioId negocioId) {
        return loteSubidaJpaRepository.findByIdAndNegocioId(loteId.valor(), negocioId.valor())
                .map(loteSubidaRepositoryMapper::toDomain)
                .map(this::cargarFotos);
    }

    private FotoCuadernoEntity aEntity(FotoCuaderno foto, LoteId loteId) {
        FotoCuadernoEntity entity = loteSubidaRepositoryMapper.toEntity(foto);
        entity.setLoteId(loteId.valor());
        return entity;
    }

    private LoteSubida cargarFotos(LoteSubida lote) {
        lote.setFotos(fotoCuadernoJpaRepository
                .findAllByLoteIdAndNegocioIdOrderByPosicionAsc(lote.getId().valor(), lote.getNegocioId().valor())
                .stream()
                .map(loteSubidaRepositoryMapper::toDomain)
                .toList());
        return lote;
    }
}
