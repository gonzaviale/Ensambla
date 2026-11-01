package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRenovacionPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RenovacionPremiumRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RenovacionPremiumRepositoryImpl implements RenovacionPremiumRepository {

    private final IRenovacionPremiumJpaRepository renovacionPremiumJpaRepository;

    private final RenovacionPremiumRepositoryMapper renovacionPremiumRepositoryMapper;

    @Override
    public RenovacionPremium save(RenovacionPremium renovacion) {
        return renovacionPremiumRepositoryMapper.toDomain(renovacionPremiumJpaRepository.saveAndFlush(
                renovacionPremiumRepositoryMapper.toEntity(renovacion)));
    }

    @Override
    public Optional<RenovacionPremium> findById(RenovacionId id) {
        return renovacionPremiumJpaRepository.findById(id.valor()).map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public Optional<NegocioId> findNegocioIdById(RenovacionId id) {
        return renovacionPremiumJpaRepository.findNegocioIdById(id.valor()).map(NegocioId::de);
    }

    @Override
    public Optional<RenovacionPremium> findByProveedorId(String proveedorId) {
        return renovacionPremiumJpaRepository.findByProveedorId(proveedorId)
                .map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public Optional<RenovacionPremium> findUltimaByNegocioId(NegocioId negocioId) {
        return renovacionPremiumJpaRepository.findActualByNegocioId(negocioId.valor())
                .map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public List<RenovacionId> listarParaSincronizar() {
        return renovacionPremiumJpaRepository.listarParaSincronizar().stream().map(RenovacionId::de).toList();
    }

    @Override
    public boolean existeAbierta(NegocioId negocioId) {
        return renovacionPremiumJpaRepository.existsByNegocioIdAndEstadoNot(negocioId.valor(), "CANCELADA");
    }
}
