package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.UsuarioRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.entity;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryImplTest {

    @Mock
    private IUsuarioJpaRepository jpa;

    private UsuarioRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UsuarioRepositoryImpl(jpa, new UsuarioRepositoryMapper());
    }

    @Test
    @DisplayName("Guarda el usuario y devuelve el modelo persistido")
    void saveShouldPersistAndReturnDomain() {
        when(jpa.saveAndFlush(any(UsuarioEntity.class))).thenReturn(entity());

        Usuario result = repository.save(usuario());

        assertThat(result.getId()).isEqualTo(USUARIO_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Busca por email normalizado y mapea el resultado")
    void findByEmailShouldMapResult() {
        when(jpa.findByEmail(EMAIL)).thenReturn(Optional.of(entity()));

        Optional<Usuario> result = repository.findByEmail(EMAIL);

        assertThat(result).get().extracting(Usuario::getId).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Busca por id y mapea el resultado")
    void findByIdShouldMapResult() {
        when(jpa.findById(USUARIO_ID)).thenReturn(Optional.of(entity()));

        Optional<Usuario> result = repository.findById(USUARIO_ID);

        assertThat(result).get().extracting(Usuario::getEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Devuelve vacío cuando el usuario no existe")
    void findByIdShouldReturnEmptyWhenMissing() {
        when(jpa.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThat(repository.findById(USUARIO_ID)).isEmpty();
    }
}
