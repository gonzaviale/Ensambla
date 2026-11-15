package com.ensambla.backend.domain.useCase.auth.registrarUsuario;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Crea la cuenta, su negocio y la suscripción gratuita en una sola transacción: si algo falla no queda nada. */
@RequiredArgsConstructor
public class RegistrarUsuario {

    private final UsuarioRepository usuarioRepository;

    private final NegocioRepository negocioRepository;

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final ContraseniaPort contraseniaPort;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public UsuarioResponse execute(RegistrarUsuarioRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return UsuarioResponse.de(transaccionPort.ejecutar(() -> registrar(request, ahora)));
    }

    private Usuario registrar(RegistrarUsuarioRequest request, OffsetDateTime ahora) {
        String email = Usuario.normalizarEmail(request.email());
        validarEmailLibre(email);
        Usuario usuario = crearUsuario(request, email, ahora);
        Negocio negocio = crearNegocio(request, usuario, ahora);
        asignarPlanGratuito(negocio.getId(), ahora);
        return usuario;
    }

    private void validarEmailLibre(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese email");
        }
    }

    private Usuario crearUsuario(RegistrarUsuarioRequest request, String email, OffsetDateTime ahora) {
        return usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .hashContrasenia(contraseniaPort.hashear(request.contrasenia()))
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build());
    }

    private Negocio crearNegocio(RegistrarUsuarioRequest request, Usuario usuario, OffsetDateTime ahora) {
        return negocioRepository.save(Negocio.builder()
                .nombre(request.nombreNegocio().trim())
                .duenioId(usuario.getId())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build());
    }

    private void asignarPlanGratuito(NegocioId negocioId, OffsetDateTime ahora) {
        Plan plan = planRepository.findByCodigo(CodigoPlan.GRATUITO)
                .orElseThrow(() -> new ResourceNotFoundException("Plan gratuito no encontrado"));
        suscripcionRepository.save(Suscripcion.builder()
                .negocioId(negocioId)
                .planId(plan.getId())
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(ahora)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build());
    }
}
