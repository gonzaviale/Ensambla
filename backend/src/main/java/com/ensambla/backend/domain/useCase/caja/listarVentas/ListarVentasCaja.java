package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarVentasCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CobroCajaRepository cobroCajaRepository;

    public ListarVentasCajaResponse execute(ListarVentasCajaRequest request) {
        var negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
        return ListarVentasCajaResponse.de(ventaCajaRepository.listarTodas(
                negocioId, request.proyectoId(), request.inventarioId()), cobroCajaRepository.listarTodos(
                negocioId, request.proyectoId(), request.inventarioId()));
    }
}
