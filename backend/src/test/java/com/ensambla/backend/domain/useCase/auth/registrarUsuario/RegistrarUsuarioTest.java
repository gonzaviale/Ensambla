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
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private UsuarioResponse cuenta;
    private Throwable error;

    @Test
    @DisplayName("Devuelve la identidad de la cuenta creada")
    void devuelveLaIdentidadDeLaCuentaCreada() {
        dadoUnEmailLibre();
        dadoQueExisteElPlanGratuito();

        cuandoSeRegistra();

        entoncesLaCuentaEsDeLaPersona();
    }

    @Test
    @DisplayName("Guarda el usuario con los datos normalizados, el hash y el instante del reloj")
    void guardaElUsuarioNormalizadoConElHashYElInstante() {
        dadoUnEmailLibre();
        dadoQueExisteElPlanGratuito();

        cuandoSeRegistra();

        entoncesSeGuardoElUsuarioActivo("Persona", EMAIL, HASH);
    }

    @Test
    @DisplayName("Guarda el negocio de la persona con su nombre recortado y el instante del reloj")
    void guardaElNegocioDeLaPersona() {
        dadoUnEmailLibre();
        dadoQueExisteElPlanGratuito();

        cuandoSeRegistra();

        entoncesSeGuardoElNegocio("Mi negocio");
    }

    @Test
    @DisplayName("Asigna al negocio nuevo el gratuito activo con las fechas del instante, sin fecha de fin")
    void asignaAlNegocioNuevoElPlanGratuito() {
        dadoUnEmailLibre();
        dadoQueExisteElPlanGratuito();

        cuandoSeRegistra();

        entoncesSeGuardoLaSuscripcionGratuita();
    }

    @Test
    @DisplayName("Rechaza el email repetido sin hashear ni crear negocio")
    void rechazaElEmailRepetidoSinEscribirNada() {
        dadoQueElEmailYaTieneCuenta();

        cuandoSeIntentaRegistrar();

        entoncesSeRechazaPor(DuplicateResourceException.class, "Ya existe una cuenta con ese email");
        entoncesNoSeHasheoNiSeCreoNada();
    }

    @Test
    @DisplayName("No crea una suscripción si falta el plan gratuito")
    void noCreaUnaSuscripcionSiFaltaElPlanGratuito() {
        dadoUnEmailLibre();
        dadoQueNoExisteElPlanGratuito();

        cuandoSeIntentaRegistrar();

        entoncesSeRechazaPor(ResourceNotFoundException.class, "Plan gratuito no encontrado");
        entoncesNoSeCreoNingunaSuscripcion();
    }

    private void dadoUnEmailLibre() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(contraseniaPort.hashear(CONTRASENIA)).thenReturn(HASH);
        when(usuarioRepository.save(any())).thenReturn(usuario());
        when(negocioRepository.save(any())).thenReturn(Negocio.builder().id(NEGOCIO_ID).build());
    }

    private void dadoQueElEmailYaTieneCuenta() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));
    }

    private void dadoQueExisteElPlanGratuito() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
    }

    private void dadoQueNoExisteElPlanGratuito() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());
    }

    private void cuandoSeRegistra() {
        cuenta = registrarUsuario.execute(
                new RegistrarUsuarioRequest(" Persona ", " Mi negocio ", " PERSONA@EXAMPLE.COM ", CONTRASENIA));
    }

    private void cuandoSeIntentaRegistrar() {
        error = catchThrowable(this::cuandoSeRegistra);
    }

    private void entoncesLaCuentaEsDeLaPersona() {
        assertThat(cuenta).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
    }

    private void entoncesSeGuardoElUsuarioActivo(String nombre, String email, String hash) {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo(nombre);
        assertThat(guardado.getEmail()).isEqualTo(email);
        assertThat(guardado.getHashContrasenia()).isEqualTo(hash);
        assertThat(guardado.estaActivo()).isTrue();
        assertThat(guardado.getCreadoEn()).isEqualTo(FECHA);
        assertThat(guardado.getActualizadoEn()).isEqualTo(FECHA);
    }

    private void entoncesSeGuardoElNegocio(String nombre) {
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        Negocio negocio = captor.getValue();
        assertThat(negocio.getNombre()).isEqualTo(nombre);
        assertThat(negocio.getDuenioId()).isEqualTo(usuario().getId());
        assertThat(negocio.getCreadoEn()).isEqualTo(FECHA);
        assertThat(negocio.getActualizadoEn()).isEqualTo(FECHA);
    }

    private void entoncesSeGuardoLaSuscripcionGratuita() {
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

    private void entoncesSeRechazaPor(Class<? extends Throwable> tipo, String mensaje) {
        assertThat(error).isInstanceOf(tipo).hasMessage(mensaje);
    }

    private void entoncesNoSeHasheoNiSeCreoNada() {
        verifyNoInteractions(negocioRepository, contraseniaPort, planRepository, suscripcionRepository);
    }

    private void entoncesNoSeCreoNingunaSuscripcion() {
        verifyNoInteractions(suscripcionRepository);
    }
}