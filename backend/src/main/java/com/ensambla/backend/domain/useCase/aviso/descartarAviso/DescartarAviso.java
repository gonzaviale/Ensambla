package com.ensambla.backend.domain.useCase.aviso.descartarAviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.objetivo.DecisionesObjetivoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class DescartarAviso {

    private final NegocioRepository negocioRepository;
    private final ControlPlanService controlPlanService;
    private final AvisoRepository avisoRepository;
    private final DecisionesObjetivoService decisionesObjetivoService;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Aviso aviso = obtenerAviso(request.avisoId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        exigirAccesoAlAviso(negocioId, aviso, ahora);
        return AvisoItem.de(descartarEnTransaccion(aviso, ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Aviso obtenerAviso(AvisoId avisoId, NegocioId negocioId) {
        return avisoRepository.obtener(avisoId, negocioId);
    }

    private void exigirAccesoAlAviso(NegocioId negocioId, Aviso aviso, OffsetDateTime ahora) {
        controlPlanService.exigirAccesoAlAviso(negocioId, aviso.getId(), ahora);
    }

    private Aviso descartarEnTransaccion(Aviso aviso, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> descartar(aviso, ahora));
    }

    private Aviso descartar(Aviso aviso, OffsetDateTime ahora) {
        aviso.descartar(ahora);
        Aviso descartado = guardar(aviso);
        registrarDecision(descartado, ahora);
        return descartado;
    }

    private Aviso guardar(Aviso aviso) {
        return avisoRepository.save(aviso);
    }

    private void registrarDecision(Aviso descartado, OffsetDateTime ahora) {
        decisionesObjetivoService.registrarAviso(descartado, TipoDecision.AVISO_DESCARTADO, ahora);
    }
}
