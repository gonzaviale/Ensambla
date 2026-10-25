package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
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
public class CrearCampo {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public CampoItem execute(CrearCampoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        ConfiguracionCampo configuracion = request.aConfiguracionCampo();
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CampoItem.de(transaccionPort.ejecutar(() -> crear(bloque, configuracion, ahora)));
    }

    /** {@code bloque} ya viene validado contra el negocio de la sesión; acá se cargan sus campos para validar. */
    private Campo crear(Bloque bloque, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
        Campo campo = bloque.agregarCampo(configuracion, ahora);
        bloqueRepository.validarBloqueRelacionado(bloque, campo);
        campo.validarReferenciasDeFormula(bloque.getCampos());
        return campoRepository.save(campo);
    }
}
