package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPreferenciaUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PreferenciaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PreferenciaRepositoryImpl implements PreferenciaRepository {

    private final IPreferenciaUsuarioJpaRepository preferenciaJpaRepository;
    private final PreferenciaRepositoryMapper preferenciaRepositoryMapper;

    @Override
    public Optional<PreferenciaUsuario> findByUsuarioId(UUID usuarioId) {
        return preferenciaJpaRepository.findById(usuarioId).map(preferenciaRepositoryMapper::toDomain);
    }

    @Override
    public PreferenciaUsuario save(PreferenciaUsuario preferencia) {
        return preferenciaRepositoryMapper.toDomain(
                preferenciaJpaRepository.save(preferenciaRepositoryMapper.toEntity(preferencia)));
    }
}
