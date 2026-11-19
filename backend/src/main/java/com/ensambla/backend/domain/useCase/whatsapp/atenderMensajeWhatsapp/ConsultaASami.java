package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Le pregunta a Sami qué entendió de un texto, con el mismo límite diario de consultas que el chat de la app. */
@RequiredArgsConstructor
final class ConsultaASami {

    private final ControlPlanService controlPlanService;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final InterpreteRegistrosIaPort interpreteIa;
    private final EmparejamientoDeFilas emparejamiento;
    private final PoliticaAtencionWhatsapp politica;

    InterpretacionRegistros interpretar(NegocioId negocioId, String texto, List<TablaDisponible> tablas,
                                        BloqueId destino, OffsetDateTime ahora) {
        exigirQueTengaIa(negocioId);
        LocalDate dia = ZonaNegocio.dia(ahora);
        reservarUnaConsulta(negocioId, dia, ahora);
        ConsultaInterpretacion consulta = new ConsultaInterpretacion(texto, tablas, dia, destino,
                emparejamiento.nombresConocidos(tablas, negocioId));
        InterpretacionRegistros interpretacion = consultarALaIa(consulta);
        registrarElConsumo(negocioId, dia, interpretacion, ahora);
        return interpretacion;
    }

    private void exigirQueTengaIa(NegocioId negocioId) {
        controlPlanService.exigirIa(negocioId);
    }

    private void reservarUnaConsulta(NegocioId negocioId, LocalDate dia, OffsetDateTime ahora) {
        if (!consumoSamiRepository.reservar(negocioId, dia, politica.limiteDiarioSami(), ahora)) {
            throw new BusinessRuleException(TextosWhatsapp.LIMITE_SAMI);
        }
    }

    private InterpretacionRegistros consultarALaIa(ConsultaInterpretacion consulta) {
        return interpreteIa.interpretar(consulta);
    }

    private void registrarElConsumo(NegocioId negocioId, LocalDate dia, InterpretacionRegistros interpretacion,
                                    OffsetDateTime ahora) {
        Optional.ofNullable(interpretacion.consumo())
                .ifPresent(consumo -> consumoSamiRepository.registrar(negocioId, dia, consumo, ahora));
    }
}
