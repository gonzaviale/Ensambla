package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPreferenciaUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PreferenciaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PreferenciaRepositoryImpl implements PreferenciaRepository {

    private final IPreferenciaUsuarioJpaRepository preferenciaJpaRepository;
    private final PreferenciaRepositoryMapper preferenciaRepositoryMapper;

    @Override
    public Optional<PreferenciaUsuario> findByUsuarioId(UsuarioId usuarioId) {
        return preferenciaJpaRepository.findById(usuarioId.valor()).map(preferenciaRepositoryMapper::toDomain);
    }

    @Override
    public PreferenciaUsuario save(PreferenciaUsuario preferencia) {
        return preferenciaRepositoryMapper.toDomain(
                preferenciaJpaRepository.save(preferenciaRepositoryMapper.toEntity(preferencia)));
    }
}
