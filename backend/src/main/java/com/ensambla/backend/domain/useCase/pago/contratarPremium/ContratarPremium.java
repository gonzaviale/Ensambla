package com.ensambla.backend.domain.useCase.pago.contratarPremium;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PreparacionRenovacion;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ContratarPremium {

    private final NegocioService negocioService;

    private final UsuarioService usuarioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final SincronizarRenovacion sincronizarRenovacion;

    private final SuscripcionService suscripcionService;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UUID usuarioId) {
        pasarelaSuscripcionesPort.exigirDisponible();
        String email = usuarioService.obtenerActivo(usuarioId).getEmail();
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        PreparacionRenovacion preparacion = preparar(negocioId, OffsetDateTime.now(clock));
        if (preparacion.nueva()) {
            ContratoRemoto remoto = pasarelaSuscripcionesPort.crear(preparacion.renovacion(), email);
            renovacionPremiumService.actualizarContrato(preparacion.renovacion().getId(), remoto,
                    OffsetDateTime.now(clock));
        } else {
            sincronizarRenovacion.execute(preparacion.renovacion().getId());
        }
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(negocioId).orElseThrow());
    }

    /** Reutiliza la renovación en curso; si no hay o la anterior se canceló, prepara una nueva para la pasarela. */
    private PreparacionRenovacion preparar(UUID negocioId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Optional<RenovacionPremium> anterior = renovacionPremiumService.consultar(negocioId);
            if (anterior.isPresent() && !"CANCELADA".equals(anterior.get().getEstado())) {
                return new PreparacionRenovacion(anterior.get(), false);
            }
            Plan premium = suscripcionService.obtenerPlan(CodigoPlan.PREMIUM);
            premium.validarLimitesConfigurados();
            if (!Boolean.TRUE.equals(premium.getConfigurado()) || premium.getPrecioMensual() == null
                    || premium.getPrecioMensual().signum() <= 0 || !"ARS".equals(premium.getMoneda())) {
                throw new BusinessRuleException("El precio de Premium todavía no está configurado.");
            }
            Suscripcion suscripcion = suscripcionService.obtener(negocioId);
            OffsetDateTime primerCobro = ahora;
            if (suscripcion.estaVigente(ahora) && premium.getId().equals(suscripcion.getPlanId())) {
                if (suscripcion.getFinEn() == null) {
                    throw new BusinessRuleException("Tu negocio ya tiene Premium sin vencimiento.");
                }
                primerCobro = suscripcion.getFinEn();
            }
            // Mercado Pago conserva segundos: redondear hacia arriba evita anticipar el fin de la prueba.
            primerCobro = primerCobro.plusNanos(999_999_999).truncatedTo(ChronoUnit.SECONDS);
            RenovacionPremium renovacion = RenovacionPremium.builder()
                    .id(UUID.randomUUID())
                    .negocioId(negocioId)
                    .estado("PREPARANDO")
                    .importe(premium.getPrecioMensual())
                    .moneda(premium.getMoneda())
                    .primerCobroEn(primerCobro)
                    .creadoEn(ahora)
                    .actualizadoEn(ahora)
                    .build();
            return new PreparacionRenovacion(renovacionPremiumRepository.save(renovacion), true);
        });
    }
}
