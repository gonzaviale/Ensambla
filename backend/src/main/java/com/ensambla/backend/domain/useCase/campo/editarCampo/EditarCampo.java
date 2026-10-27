package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class EditarCampo {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public CampoItem execute(EditarCampoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Campo campo = campoRepository.obtener(request.campoId(), negocioId);
        Bloque bloque = bloqueRepository.obtener(campo.getBloqueId(), negocioId);
        ConfiguracionCampo configuracion = request.aConfiguracionCampo();
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CampoItem.de(transaccionPort.ejecutar(() -> editar(bloque, campo, configuracion, ahora)));
    }

    /** {@code bloque} es el dueño del campo; acá se cargan sus campos para validar el nombre. */
    private Campo editar(Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
        bloque.editarCampo(campo, configuracion, ahora);
        bloqueRepository.validarBloqueRelacionado(bloque, campo);
        campo.validarReferenciasDeFormula(bloque.getCampos());
        if (campo.getTipo() != TipoCampo.NUMERO) {
            campo.validarSinFormulasQueLoUsen(bloque.getCampos(), "cambiar el tipo de");
        }
        return campoRepository.save(campo);
    }
}
