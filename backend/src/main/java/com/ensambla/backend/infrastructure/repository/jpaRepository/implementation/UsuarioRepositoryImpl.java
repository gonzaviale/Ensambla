package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.UsuarioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final IUsuarioJpaRepository jpa;
    private final UsuarioRepositoryMapper mapper;

    @Override
    public Usuario save(Usuario usuario) {
        try {
            return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(usuario)));
        } catch (DataIntegrityViolationException exception) {
            if (String.valueOf(exception.getMostSpecificCause().getMessage()).contains("usuario_email_key")) {
                throw new DuplicateResourceException("Ya existe una cuenta con ese email");
            }
            throw exception;
        }
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
