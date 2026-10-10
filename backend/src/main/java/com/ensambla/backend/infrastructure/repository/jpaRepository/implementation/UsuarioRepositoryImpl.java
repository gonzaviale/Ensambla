package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.UsuarioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final IUsuarioJpaRepository jpa;
    private final UsuarioRepositoryMapper mapper;

    @Override
    public Usuario save(Usuario usuario) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(usuario)));
    }

    @Override
    public Optional<Usuario> findByEmail(String emailNormalizado) {
        return jpa.findByEmail(emailNormalizado).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }
}
