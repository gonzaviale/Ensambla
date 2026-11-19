package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.entity;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryImplTest {

    @Mock
    private IUsuarioJpaRepository jpa;

    private UsuarioRepositoryImpl repository;

    @BeforeEach
    void dadoUnRepositorioDeUsuarios() {
        repository = new UsuarioRepositoryImpl(jpa, new UsuarioRepositoryMapper());
    }

    private Usuario guardado;
    private Optional<Usuario> encontrado;
    private Throwable rechazo;
    private DataIntegrityViolationException violacion;

    @Test
    @DisplayName("Guarda el usuario y devuelve el modelo persistido")
    void guardaElUsuarioYDevuelveElModeloPersistido() {
        dadoQueGuardarDevuelveElUsuario();

        cuandoSeGuardaElUsuario();

        entoncesSeDevuelveElUsuarioPersistido();
    }

    @Test
    @DisplayName("Traduce un email repetido concurrente al mensaje de dominio")
    void traduceUnEmailRepetidoConcurrenteAlMensajeDeDominio() {
        dadoQueElEmailYaExisteEnUnGuardadoConcurrente();

        cuandoSeIntentaGuardarElUsuario();

        entoncesSeRechazaPorEmailRepetido();
    }

    @Test
    @DisplayName("Conserva otras violaciones de integridad")
    void conservaOtrasViolacionesDeIntegridad() {
        dadoQueGuardarFallaPorOtraRestriccion();

        cuandoSeIntentaGuardarElUsuario();

        entoncesSePropagaLaMismaViolacion();
    }

    @Test
    @DisplayName("Busca por email normalizado y mapea el resultado")
    void buscaPorEmailNormalizadoYMapeaElResultado() {
        when(jpa.findByEmail(EMAIL)).thenReturn(Optional.of(unaEntidad()));

        cuandoSeBuscaElUsuarioPorEmail();

        entoncesSeEncuentraElUsuarioConSuId();
    }

    @Test
    @DisplayName("Busca por id y mapea el resultado")
    void buscaPorIdYMapeaElResultado() {
        when(jpa.findById(USUARIO_ID.valor())).thenReturn(Optional.of(unaEntidad()));

        cuandoSeBuscaElUsuarioPorId();

        entoncesSeEncuentraElUsuarioConSuEmail();
    }

    @Test
    @DisplayName("Devuelve vacío cuando el usuario no existe")
    void devuelveVacioCuandoElUsuarioNoExiste() {
        when(jpa.findById(USUARIO_ID.valor())).thenReturn(Optional.empty());

        cuandoSeBuscaElUsuarioPorId();

        entoncesNoSeEncuentraNingunUsuario();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElUsuario() {
        when(jpa.saveAndFlush(any(UsuarioEntity.class))).thenReturn(unaEntidad());
    }

    private void dadoQueElEmailYaExisteEnUnGuardadoConcurrente() {
        when(jpa.saveAndFlush(any(UsuarioEntity.class))).thenThrow(
                new DataIntegrityViolationException("duplicate key violates usuario_email_key"));
    }

    private void dadoQueGuardarFallaPorOtraRestriccion() {
        violacion = new DataIntegrityViolationException("otra restricción");
        when(jpa.saveAndFlush(any(UsuarioEntity.class))).thenThrow(violacion);
    }

    private UsuarioEntity unaEntidad() {
        return entity();
    }

    // --- cuando ---
    private void cuandoSeGuardaElUsuario() {
        guardado = repository.save(usuario());
    }

    private void cuandoSeIntentaGuardarElUsuario() {
        rechazo = catchThrowable(this::cuandoSeGuardaElUsuario);
    }

    private void cuandoSeBuscaElUsuarioPorEmail() {
        encontrado = repository.findByEmail(EMAIL);
    }

    private void cuandoSeBuscaElUsuarioPorId() {
        encontrado = repository.findById(USUARIO_ID);
    }

    // --- entonces ---
    private void entoncesSeDevuelveElUsuarioPersistido() {
        assertThat(guardado.getId()).isEqualTo(USUARIO_ID);
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
    }

    private void entoncesSeRechazaPorEmailRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya existe una cuenta con ese email");
    }

    private void entoncesSePropagaLaMismaViolacion() {
        assertThat(rechazo).isSameAs(violacion);
    }

    private void entoncesSeEncuentraElUsuarioConSuId() {
        assertThat(encontrado).get().extracting(Usuario::getId).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeEncuentraElUsuarioConSuEmail() {
        assertThat(encontrado).get().extracting(Usuario::getEmail).isEqualTo(EMAIL);
    }

    private void entoncesNoSeEncuentraNingunUsuario() {
        assertThat(encontrado).isEmpty();
    }
}
