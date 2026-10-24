package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarCampo {

    private final NegocioService negocioService;

    private final CampoService campoService;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarCampoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Campo campo = campoService.obtener(request.campoId(), negocioId);
        transaccionPort.ejecutar(() -> eliminar(campo));
    }

    private void eliminar(Campo campo) {
        campoService.validarSinFormulasQueLoUsen(
                campoRepository.findByBloqueIdAndNegocioId(campo.getBloqueId(), campo.getNegocioId()),
                campo, "eliminar");
        campoRepository.deleteByIdAndNegocioId(campo.getId(), campo.getNegocioId());
    }
}
