package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    /** El email se guarda normalizado (minúsculas, sin espacios), lo garantiza un CHECK en la base. */
    Optional<UsuarioEntity> findByEmail(String email);
}
