package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import lombok.RequiredArgsConstructor;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

@RequiredArgsConstructor
public class ListarVentasCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CobroCajaRepository cobroCajaRepository;

    public ListarVentasCajaResponse execute(ListarVentasCajaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirInventarioPropio(negocioId, request);
        return listarCuentasDelInventario(negocioId, request);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirInventarioPropio(NegocioId negocioId, ListarVentasCajaRequest request) {
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
    }

    private ListarVentasCajaResponse listarCuentasDelInventario(NegocioId negocioId, ListarVentasCajaRequest request) {
        return ListarVentasCajaResponse.de(ventaCajaRepository.listarTodas(
                negocioId, request.proyectoId(), request.inventarioId()), cobroCajaRepository.listarTodos(
                negocioId, request.proyectoId(), request.inventarioId()));
    }
}
