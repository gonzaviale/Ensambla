package com.ensambla.backend.domain.useCase.pago.contratarPremium;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PreparacionRenovacion;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RequiredArgsConstructor
public class ContratarPremium {

    private final NegocioRepository negocioRepository;

    private final UsuarioRepository usuarioRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final SuscripcionRepository suscripcionRepository;

    private final PlanRepository planRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UsuarioId usuarioId) {
        exigirPasarelaDisponible();
        String email = obtenerEmailDelUsuario(usuarioId);
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        PreparacionRenovacion preparacion = preparar(negocioId, ahora());
        contratarOSincronizar(preparacion, email);
        return respuestaDeLaRenovacion(negocioId);
    }

    private void exigirPasarelaDisponible() {
        pasarelaSuscripcionesPort.exigirDisponible();
    }

    private String obtenerEmailDelUsuario(UsuarioId usuarioId) {
        return usuarioRepository.obtenerActivo(usuarioId).getEmail();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(clock);
    }

    private void contratarOSincronizar(PreparacionRenovacion preparacion, String email) {
        if (preparacion.nueva()) {
            crearContratoEnLaPasarela(preparacion.renovacion(), email);
        } else {
            sincronizarConLaPasarela(preparacion.renovacion());
        }
    }

    private void crearContratoEnLaPasarela(RenovacionPremium renovacion, String email) {
        ContratoRemoto contrato = pasarelaSuscripcionesPort.crear(renovacion, email);
        renovacionPremiumService.actualizarContrato(renovacion.getId(), contrato, ahora());
    }

    private void sincronizarConLaPasarela(RenovacionPremium renovacion) {
        renovacionPremiumService.sincronizar(renovacion.getId(), ahora());
    }

    private ConsultarRenovacionResponse respuestaDeLaRenovacion(NegocioId negocioId) {
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(), ultimaRenovacionDe(negocioId));
    }

    private RenovacionPremium ultimaRenovacionDe(NegocioId negocioId) {
        return renovacionPremiumService.consultar(negocioId).orElseThrow();
    }

    /** Reutiliza la renovación en curso; si no hay o la anterior se canceló, prepara una nueva para la pasarela. */
    private PreparacionRenovacion preparar(NegocioId negocioId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> prepararConNegocioBloqueado(negocioId, ahora));
    }

    private PreparacionRenovacion prepararConNegocioBloqueado(NegocioId negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        return renovacionEnCurso(negocioId)
                .map(PreparacionRenovacion::reutilizada)
                .orElseGet(() -> prepararNueva(negocioId, ahora));
    }

    private Optional<RenovacionPremium> renovacionEnCurso(NegocioId negocioId) {
        return renovacionPremiumService.consultar(negocioId).filter(renovacion -> !renovacion.estaCancelada());
    }

    private PreparacionRenovacion prepararNueva(NegocioId negocioId, OffsetDateTime ahora) {
        Plan premium = planPremiumConPrecio();
        Suscripcion suscripcion = suscripcionRepository.obtener(negocioId);
        OffsetDateTime primerCobro = primerCobroDe(suscripcion, premium, ahora);
        RenovacionPremium renovacion = RenovacionPremium.nueva(
                negocioId, premium.getPrecioMensual(), premium.getMoneda(), primerCobro, ahora);
        return PreparacionRenovacion.nueva(renovacionPremiumRepository.save(renovacion));
    }

    private Plan planPremiumConPrecio() {
        Plan premium = planRepository.obtener(CodigoPlan.PREMIUM);
        premium.validarLimitesConfigurados();
        premium.validarPrecioMensualConfigurado();
        return premium;
    }

    private OffsetDateTime primerCobroDe(Suscripcion suscripcion, Plan premium, OffsetDateTime ahora) {
        OffsetDateTime desde = suscripcion.vencimientoDePremium(premium.getId(), ahora).orElse(ahora);
        return alSegundoSiguiente(desde);
    }

    /** Mercado Pago conserva segundos: redondear hacia arriba evita anticipar el fin de la prueba. */
    private OffsetDateTime alSegundoSiguiente(OffsetDateTime momento) {
        return momento.plusNanos(999_999_999).truncatedTo(ChronoUnit.SECONDS);
    }
}
