package com.ensambla.backend.domain.useCase.pago.consultarRenovacion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ConsultarRenovacion {

    private final NegocioRepository negocioRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    public ConsultarRenovacionResponse execute(UsuarioId usuarioId) {
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        return ConsultarRenovacionResponse.de(pasarelaDisponible(), ultimaRenovacionDe(negocioId).orElse(null));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private boolean pasarelaDisponible() {
        return pasarelaSuscripcionesPort.disponible();
    }

    private Optional<RenovacionPremium> ultimaRenovacionDe(NegocioId negocioId) {
        return renovacionPremiumService.consultar(negocioId);
    }
}
