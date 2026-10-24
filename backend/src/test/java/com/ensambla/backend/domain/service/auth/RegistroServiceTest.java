package com.ensambla.backend.domain.service.auth;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.HASH;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NegocioRepository negocioRepository;
    @Mock
    private ContraseniaPort contraseniaPort;
    @Mock
    private SuscripcionService suscripcionService;
    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private RegistroService registroService;
    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;
    @Captor
    private ArgumentCaptor<Negocio> negocioCaptor;

    @Test
    @DisplayName("Normaliza los datos y guarda usuario y negocio con el mismo instante")
    void registrarShouldSaveUsuarioAndNegocioWithHash() {
        givenEmailAvailable();

        Usuario result = registrar();

        thenUsuarioAndNegocioSaved(result);
    }

    @Test
    @DisplayName("Rechaza el email repetido sin hashear ni crear negocio")
    void registrarShouldRejectDuplicateEmailBeforeWriting() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(this::registrar).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya existe una cuenta con ese email");

        verifyNoInteractions(negocioRepository, contraseniaPort, suscripcionService);
    }

    // --- arrange ---
    private void givenEmailAvailable() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(contraseniaPort.hashear(CONTRASENIA)).thenReturn(HASH);
        when(usuarioRepository.save(any())).thenReturn(usuario());
        when(negocioRepository.save(any())).thenReturn(Negocio.builder().id(NEGOCIO_ID).build());
    }

    // --- act ---
    private Usuario registrar() {
        return registroService.registrar(
                new RegistrarUsuarioRequest(" Persona ", " Mi negocio ", " PERSONA@EXAMPLE.COM ", CONTRASENIA),
                FECHA);
    }

    // --- assert ---
    private void thenUsuarioAndNegocioSaved(Usuario result) {
        assertThat(result.getId()).isEqualTo(usuario().getId());
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Persona");
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
        assertThat(guardado.getHashContrasenia()).isEqualTo(HASH);
        assertThat(guardado.estaActivo()).isTrue();
        assertThat(guardado.getCreadoEn()).isEqualTo(FECHA);
        assertThat(guardado.getActualizadoEn()).isEqualTo(FECHA);
        verify(negocioRepository).save(negocioCaptor.capture());
        Negocio negocio = negocioCaptor.getValue();
        assertThat(negocio.getNombre()).isEqualTo("Mi negocio");
        assertThat(negocio.getDuenioId()).isEqualTo(result.getId());
        assertThat(negocio.getCreadoEn()).isEqualTo(FECHA);
        assertThat(negocio.getActualizadoEn()).isEqualTo(FECHA);
        verify(suscripcionService).asignarGratuito(NEGOCIO_ID, FECHA);
    }
}

