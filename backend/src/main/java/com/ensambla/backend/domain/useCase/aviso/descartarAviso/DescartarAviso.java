package com.ensambla.backend.domain.useCase.aviso.descartarAviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class DescartarAviso {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final AccesoContenidoService accesoContenidoService;
    private final AvisoRepository avisoRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Aviso aviso = avisoService.obtener(request.avisoId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        accesoContenidoService.exigir(negocioId, TipoContenidoPlan.AVISOS, "servidor:" + aviso.getId(), ahora);
        return AvisoItem.de(transaccionPort.ejecutar(() -> descartar(aviso, ahora)));
    }

    private Aviso descartar(Aviso aviso, OffsetDateTime ahora) {
        aviso.descartar(ahora);
        return avisoRepository.save(aviso);
    }
}
