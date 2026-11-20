package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.ProteccionDeEstructura;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.campo.CampoId;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class EliminarCampo {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarCampoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Campo campo = obtenerCampo(request.campoId(), negocioId);
        eliminarEnTransaccion(campo);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Campo obtenerCampo(CampoId campoId, NegocioId negocioId) {
        return campoRepository.obtener(campoId, negocioId);
    }

    private void eliminarEnTransaccion(Campo campo) {
        transaccionPort.ejecutar(() -> eliminar(campo));
    }

    private void eliminar(Campo campo) {
        validarQueNoSeaUnaColumnaFija(campo);
        campo.validarSinFormulasQueLoUsen(camposDelBloqueDe(campo), "eliminar");
        borrar(campo);
    }

    private void validarQueNoSeaUnaColumnaFija(Campo campo) {
        ProteccionDeEstructura.exigirQueSePuedaQuitarLaColumna(obtenerBloqueDe(campo), campo);
    }

    private Bloque obtenerBloqueDe(Campo campo) {
        return bloqueRepository.obtener(campo.getBloqueId(), campo.getNegocioId());
    }

    private List<Campo> camposDelBloqueDe(Campo campo) {
        return campoRepository.findByBloqueIdAndNegocioId(campo.getBloqueId(), campo.getNegocioId());
    }

    private void borrar(Campo campo) {
        campoRepository.deleteByIdAndNegocioId(campo.getId(), campo.getNegocioId());
    }
}
