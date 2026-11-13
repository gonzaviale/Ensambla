package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPagoPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PagoPremiumRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PagoPremiumRepositoryImpl implements PagoPremiumRepository {

    private final IPagoPremiumJpaRepository pagoPremiumJpaRepository;

    private final PagoPremiumRepositoryMapper pagoPremiumRepositoryMapper;

    @Override
    public PagoPremium save(PagoPremium pago) {
        return pagoPremiumRepositoryMapper.toDomain(pagoPremiumJpaRepository.saveAndFlush(
                pagoPremiumRepositoryMapper.toEntity(pago)));
    }

    @Override
    public Optional<PagoPremium> findByFacturaId(String facturaId) {
        return pagoPremiumJpaRepository.findById(facturaId).map(pagoPremiumRepositoryMapper::toDomain);
    }

    @Override
    public Optional<PagoPremium> findVigente(UsuarioId usuarioId, OffsetDateTime ahora) {
        return pagoPremiumJpaRepository
                .findFirstByUsuarioIdAndEstadoAndInicioEnLessThanEqualAndFinEnGreaterThanOrderByFinEnDesc(
                        usuarioId.valor(), "approved", ahora, ahora).map(pagoPremiumRepositoryMapper::toDomain);
    }

    @Override
    public boolean existeByUsuarioId(UsuarioId usuarioId) {
        return pagoPremiumJpaRepository.existsByUsuarioId(usuarioId.valor());
    }
}
