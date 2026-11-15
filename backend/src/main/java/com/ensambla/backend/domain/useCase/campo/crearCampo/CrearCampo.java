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
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CampoItem.de(crearEnTransaccion(bloque, request.aConfiguracionCampo(), ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private Campo crearEnTransaccion(Bloque bloque, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> crear(bloque, configuracion, ahora));
    }

    /** {@code bloque} ya viene validado contra el negocio de la sesión; acá se cargan sus campos para validar. */
    private Campo crear(Bloque bloque, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        cargarCamposDe(bloque);
        Campo campo = bloque.agregarCampo(configuracion, ahora);
        validarConexion(bloque, campo);
        campo.validarReferenciasDeFormula(bloque.getCampos());
        return guardar(campo);
    }

    private void cargarCamposDe(Bloque bloque) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
    }

    private void validarConexion(Bloque bloque, Campo campo) {
        bloqueRepository.validarBloqueRelacionado(bloque, campo);
    }

    private Campo guardar(Campo campo) {
        return campoRepository.save(campo);
    }
}
