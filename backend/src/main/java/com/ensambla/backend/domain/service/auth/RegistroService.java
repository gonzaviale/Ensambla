package com.ensambla.backend.domain.service.auth;

import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final ContraseniaPort contraseniaPort;
    private final SuscripcionService suscripcionService;

    @Transactional
    public Usuario registrar(RegistrarUsuarioRequest request, OffsetDateTime instante) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese email");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .hashContrasenia(contraseniaPort.hashear(request.contrasenia()))
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(instante)
                .actualizadoEn(instante)
                .build());
        Negocio negocio = negocioRepository.save(Negocio.builder()
                .nombre(request.nombreNegocio().trim())
                .duenioId(usuario.getId())
                .creadoEn(instante)
                .actualizadoEn(instante)
                .build());
        suscripcionService.asignarGratuito(negocio.getId(), instante);
        return usuario;
    }
}

