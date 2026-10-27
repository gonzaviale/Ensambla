package com.ensambla.backend.domain.useCase.aviso.descartarAviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Aviso aviso = avisoRepository.obtener(request.avisoId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        controlPlanService.exigir(negocioId, TipoContenidoPlan.AVISOS, "servidor:" + aviso.getId(), ahora);
        return AvisoItem.de(transaccionPort.ejecutar(() -> descartar(aviso, ahora)));
    }

    private Aviso descartar(Aviso aviso, OffsetDateTime ahora) {
        aviso.descartar(ahora);
        return avisoRepository.save(aviso);
    }
}
