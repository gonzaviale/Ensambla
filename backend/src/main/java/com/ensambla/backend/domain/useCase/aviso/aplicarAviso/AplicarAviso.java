package com.ensambla.backend.domain.useCase.aviso.aplicarAviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarAviso {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final Clock clock;
    private final AccesoContenidoService accesoContenidoService;

    public AvisoItem execute(ResolverAvisoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Aviso aviso = avisoService.obtener(request.avisoId(), negocioId);
        accesoContenidoService.exigir(negocioId, TipoContenidoPlan.AVISOS,
                "servidor:" + aviso.getId(), OffsetDateTime.now(clock));
        return AvisoItem.de(avisoService.aplicar(aviso, OffsetDateTime.now(clock)));
    }
}
