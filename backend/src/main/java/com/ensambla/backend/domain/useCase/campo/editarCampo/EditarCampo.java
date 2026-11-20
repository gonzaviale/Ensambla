package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.ProteccionDeEstructura;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.campo.CampoId;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class EditarCampo {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public CampoItem execute(EditarCampoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Campo campo = obtenerCampo(request.campoId(), negocioId);
        Bloque bloque = obtenerBloqueDelCampo(campo, negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CampoItem.de(editarEnTransaccion(bloque, campo, request.aConfiguracionCampo(), ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Campo obtenerCampo(CampoId campoId, NegocioId negocioId) {
        return campoRepository.obtener(campoId, negocioId);
    }

    private Bloque obtenerBloqueDelCampo(Campo campo, NegocioId negocioId) {
        return bloqueRepository.obtener(campo.getBloqueId(), negocioId);
    }

    private Campo editarEnTransaccion(
            Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> editar(bloque, campo, configuracion, ahora));
    }

    /** {@code bloque} es el dueño del campo; acá se cargan sus campos para validar el nombre. */
    private Campo editar(Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        cargarCamposDe(bloque);
        Campo editado = bloque.editarCampo(campo, configuracion, ahora);
        validarQueConserveSuFuncion(bloque, campo, editado);
        validarConexion(bloque, editado);
        editado.validarReferenciasDeFormula(bloque.getCampos());
        editado.validarCambioDeTipo(bloque.getCampos());
        return guardar(editado);
    }

    private void validarQueConserveSuFuncion(Bloque bloque, Campo campo, Campo editado) {
        ProteccionDeEstructura.exigirQueSePuedaCambiarLaColumna(
                bloque, campo, editado, obtenerBloquesDelSistema(bloque));
    }

    private List<Bloque> obtenerBloquesDelSistema(Bloque bloque) {
        return bloqueRepository.findByProyectoIdAndNegocioId(bloque.getProyectoId(), bloque.getNegocioId());
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
