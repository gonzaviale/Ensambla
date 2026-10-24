package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearCampo {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final Clock clock;

    public CampoItem execute(CrearCampoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        ConfiguracionCampo configuracion = request.aConfiguracionCampo();
        Campo campo = campoService.crear(bloque, configuracion, OffsetDateTime.now(clock));
        return CampoItem.de(campo);
    }
}
