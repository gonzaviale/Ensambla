package com.ensambla.backend.domain.useCase.negocio.eliminarNegocio;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class EliminarNegocio {

    private final NegocioRepository negocioRepository;

    private final NegocioActivoPort negocioActivoPort;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarNegocioRequest request) {
        transaccionPort.ejecutar(() -> eliminar(request));
    }

    private void eliminar(EliminarNegocioRequest request) {
        List<Negocio> negocios = negocioRepository.findAllByDuenioIdParaActualizar(request.usuarioId());
        Negocio objetivo = negocios.stream()
                .filter(negocio -> negocio.getId().equals(request.negocioId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado"));
        if (negocios.size() == 1) {
            throw new DuplicateResourceException("No podés eliminar el único negocio de tu cuenta.");
        }
        NegocioId activoId = negocioActivoPort.obtener(request.usuarioId())
                .filter(id -> negocios.stream().anyMatch(negocio -> negocio.getId().equals(id)))
                .orElse(negocios.getFirst().getId());
        negocioRepository.deleteById(objetivo.getId());
        if (objetivo.getId().equals(activoId)) {
            NegocioId siguiente = negocios.stream()
                    .map(Negocio::getId)
                    .filter(id -> !id.equals(objetivo.getId()))
                    .findFirst()
                    .orElseThrow();
            negocioActivoPort.activar(request.usuarioId(), siguiente);
        }
    }
}
