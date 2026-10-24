package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class EditarCampo {

    private final NegocioService negocioService;

    private final CampoService campoService;

    private final BloqueService bloqueService;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public CampoItem execute(EditarCampoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Campo campo = campoService.obtener(request.campoId(), negocioId);
        Bloque bloque = bloqueService.obtener(campo.getBloqueId(), negocioId);
        ConfiguracionCampo configuracion = request.aConfiguracionCampo();
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CampoItem.de(transaccionPort.ejecutar(() -> editar(bloque, campo, configuracion, ahora)));
    }

    /** {@code bloque} es el dueño del campo; acá se cargan sus campos para validar el nombre. */
    private Campo editar(Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        campoService.cargarCampos(bloque);
        bloque.editarCampo(campo, configuracion, ahora);
        campoService.validarConfiguracion(bloque, campo);
        if (campo.getTipo() != TipoCampo.NUMERO) {
            campoService.validarSinFormulasQueLoUsen(bloque.getCampos(), campo, "cambiar el tipo de");
        }
        return campoRepository.save(campo);
    }
}
