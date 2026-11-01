package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EliminarCampo {

    private final NegocioRepository negocioRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarCampoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Campo campo = campoRepository.obtener(request.campoId(), negocioId);
        transaccionPort.ejecutar(() -> eliminar(campo));
    }

    private void eliminar(Campo campo) {
        campo.validarSinFormulasQueLoUsen(
                campoRepository.findByBloqueIdAndNegocioId(campo.getBloqueId(), campo.getNegocioId()), "eliminar");
        campoRepository.deleteByIdAndNegocioId(campo.getId(), campo.getNegocioId());
    }
}
