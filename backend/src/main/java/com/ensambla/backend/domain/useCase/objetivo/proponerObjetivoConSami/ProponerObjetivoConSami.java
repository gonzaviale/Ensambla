package com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.ContextoObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.ConversacionObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * La persona le cuenta un objetivo a Sami (o le hace una pregunta, como «¿puedo vender budines?») y Sami contesta con
 * los datos de su negocio, le muestra cómo lo entendió y le propone hasta 3 mini-objetivos: medidos con las métricas
 * del catálogo o manuales. No guarda nada; la conversación llega entera en cada pedido.
 * <p>
 * Con la cuota mensual del plan agotada no se consulta a la IA: se explica y se ofrece el seguimiento manual (o los
 * planes). La cuota se descuenta recién al guardar.
 * <p>
 * La IA recibe el texto de la persona como dato (sin correos ni teléfonos) y los números ya calculados por el servidor;
 * lo que devuelve se valida. Si la IA está apagada, no hay acceso, se agotó la reserva diaria, falla o responde algo
 * inválido, la propuesta es el seguimiento manual de respaldo.
 */
@Slf4j
@RequiredArgsConstructor
public class ProponerObjetivoConSami {

    /** Cuántas métricas del catálogo se le muestran a la IA, las que más suman. */
    private static final int MAXIMO_EN_EL_CATALOGO = 12;

    private final NegocioRepository negocioRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final ControlPlanService controlPlanService;

    private final ConsumoSamiRepository consumoSamiRepository;

    private final ConversacionObjetivosIaPort conversacionObjetivosIaPort;

    private final Clock clock;

    private final boolean iaHabilitada;

    private final int limiteDiarioIa;

    private final int maximoMensajes;

    public ProponerObjetivoConSamiResponse execute(ProponerObjetivoConSamiRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        ConversacionObjetivo conversacion = ConversacionObjetivo.de(request.mensajes(), maximoMensajes);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        MedidorMiniObjetivo medidor = medirElNegocio(negocioId, ahora);
        ContextoObjetivoIa contexto = ContextoObjetivoIa.de(medidor, obtenerZona(negocioId));
        CuotaObjetivosIa cuota = consultarCuota(negocioId, ahora);
        ConsultaObjetivoIa consulta = new ConsultaObjetivoIa(
                conversacion.paraLaIa(), catalogoDe(negocioId, medidor), contexto);
        PropuestaObjetivo propuesta = proponer(negocioId, consulta, conversacion, cuota, ahora);
        return ProponerObjetivoConSamiResponse.de(
                propuesta, contexto.datos(), conversacion.mensajesRestantes(), cuota);
    }

    private PropuestaObjetivo proponer(
            NegocioId negocioId, ConsultaObjetivoIa consulta, ConversacionObjetivo conversacion,
            CuotaObjetivosIa cuota, OffsetDateTime ahora) {
        if (!iaHabilitada) {
            return PropuestaObjetivo.respaldo(conversacion.objetivoDeLaPersona());
        }
        if (cuota.agotada()) {
            return PropuestaObjetivo.sinCuota(conversacion.objetivoDeLaPersona(), cuota.limite());
        }
        return proponerConIa(negocioId, consulta, conversacion, ahora);
    }

    private PropuestaObjetivo proponerConIa(
            NegocioId negocioId, ConsultaObjetivoIa consulta, ConversacionObjetivo conversacion,
            OffsetDateTime ahora) {
        try {
            return conversarSiHayReservaDiaria(negocioId, consulta, conversacion, ahora);
        } catch (RuntimeException e) {
            log.warn("No se pudo armar el objetivo con IA; se usa el seguimiento manual de respaldo", e);
            return PropuestaObjetivo.respaldo(conversacion.objetivoDeLaPersona());
        }
    }

    private PropuestaObjetivo conversarSiHayReservaDiaria(
            NegocioId negocioId, ConsultaObjetivoIa consulta, ConversacionObjetivo conversacion,
            OffsetDateTime ahora) {
        exigirAccesoALaIa(negocioId);
        LocalDate fecha = ZonaNegocio.dia(ahora);
        if (!reservarConsumoDiario(negocioId, fecha, ahora)) {
            return PropuestaObjetivo.respaldo(conversacion.objetivoDeLaPersona());
        }
        PropuestaObjetivoIa respuesta = conversarConSami(consulta);
        registrarConsumo(negocioId, fecha, respuesta, ahora);
        return PropuestaObjetivo.de(respuesta, consulta);
    }

    private List<CandidatoMiniObjetivo> catalogoDe(NegocioId negocioId, MedidorMiniObjetivo medidor) {
        return propuestaMiniObjetivosService.candidatos(negocioId, medidor).stream()
                .limit(MAXIMO_EN_EL_CATALOGO)
                .toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private MedidorMiniObjetivo medirElNegocio(NegocioId negocioId, OffsetDateTime ahora) {
        return propuestaMiniObjetivosService.medidor(negocioId, ZonaNegocio.dia(ahora));
    }

    private AnalisisZona obtenerZona(NegocioId negocioId) {
        return propuestaMiniObjetivosService.zona(negocioId);
    }

    private CuotaObjetivosIa consultarCuota(NegocioId negocioId, OffsetDateTime ahora) {
        return controlPlanService.cuotaObjetivosIa(negocioId, ahora);
    }

    private void exigirAccesoALaIa(NegocioId negocioId) {
        controlPlanService.exigirIa(negocioId);
    }

    private boolean reservarConsumoDiario(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora) {
        return consumoSamiRepository.reservar(negocioId, fecha, limiteDiarioIa, ahora);
    }

    private PropuestaObjetivoIa conversarConSami(ConsultaObjetivoIa consulta) {
        return conversacionObjetivosIaPort.conversar(consulta);
    }

    private void registrarConsumo(
            NegocioId negocioId, LocalDate fecha, PropuestaObjetivoIa respuesta, OffsetDateTime ahora) {
        consumoSamiRepository.registrar(negocioId, fecha, respuesta.consumo(), ahora);
    }
}
