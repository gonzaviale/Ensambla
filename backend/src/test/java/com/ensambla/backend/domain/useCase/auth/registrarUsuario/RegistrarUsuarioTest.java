package com.ensambla.backend.domain.useCase.auth.registrarUsuario;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.PLAN_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.HASH;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrarUsuarioTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final ContraseniaPort contraseniaPort = mock(ContraseniaPort.class);
    private final RegistrarUsuario registrarUsuario = new RegistrarUsuario(usuarioRepository, negocioRepository,
            planRepository, suscripcionRepository, contraseniaPort, new TransaccionDirecta(),
            Clock.fixed(FECHA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Normaliza los datos y guarda usuario y negocio con el instante del reloj")
    void executeShouldSaveUsuarioAndNegocioWithHash() {
        givenEmailAvailable();
        givenFreePlanAvailable();

        UsuarioResponse result = registrar();

        assertThat(result).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
        thenUsuarioAndNegocioSaved();
    }

    @Test
    @DisplayName("Asigna al negocio nuevo el gratuito activo con las fechas del instante, sin fecha de fin")
    void executeShouldAssignActiveFreeSubscription() {
        givenEmailAvailable();
        givenFreePlanAvailable();

        registrar();

        thenFreeSubscriptionSaved();
    }

    @Test
    @DisplayName("Rechaza el email repetido sin hashear ni crear negocio")
    void executeShouldRejectDuplicateEmailBeforeWriting() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(this::registrar).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya existe una cuenta con ese email");

        verifyNoInteractions(negocioRepository, contraseniaPort, planRepository, suscripcionRepository);
    }

    @Test
    @DisplayName("No crea una suscripción si falta el plan gratuito")
    void executeShouldRejectMissingFreePlan() {
        givenEmailAvailable();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());

        assertThatThrownBy(this::registrar)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan gratuito no encontrado");

        verifyNoInteractions(suscripcionRepository);
    }

    // --- arrange ---
    private void givenEmailAvailable() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(contraseniaPort.hashear(CONTRASENIA)).thenReturn(HASH);
        when(usuarioRepository.save(any())).thenReturn(usuario());
        when(negocioRepository.save(any())).thenReturn(Negocio.builder().id(NEGOCIO_ID).build());
    }

    private void givenFreePlanAvailable() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
    }

    // --- act ---
    private UsuarioResponse registrar() {
        return registrarUsuario.execute(
                new RegistrarUsuarioRequest(" Persona ", " Mi negocio ", " PERSONA@EXAMPLE.COM ", CONTRASENIA));
    }

    // --- assert ---
    private void thenUsuarioAndNegocioSaved() {
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Persona");
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
        assertThat(guardado.getHashContrasenia()).isEqualTo(HASH);
        assertThat(guardado.estaActivo()).isTrue();
        assertThat(guardado.getCreadoEn()).isEqualTo(FECHA);
        assertThat(guardado.getActualizadoEn()).isEqualTo(FECHA);
        ArgumentCaptor<Negocio> negocioCaptor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(negocioCaptor.capture());
        Negocio negocio = negocioCaptor.getValue();
        assertThat(negocio.getNombre()).isEqualTo("Mi negocio");
        assertThat(negocio.getDuenioId()).isEqualTo(usuario().getId());
        assertThat(negocio.getCreadoEn()).isEqualTo(FECHA);
        assertThat(negocio.getActualizadoEn()).isEqualTo(FECHA);
    }

    private void thenFreeSubscriptionSaved() {
        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());
        Suscripcion guardada = captor.getValue();
        assertThat(guardada.getId()).isNull();
        assertThat(guardada.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(guardada.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(guardada.getInicioEn()).isEqualTo(FECHA);
        assertThat(guardada.getFinEn()).isNull();
        assertThat(guardada.getCreadoEn()).isEqualTo(FECHA);
        assertThat(guardada.getActualizadoEn()).isEqualTo(FECHA);
    }
}
