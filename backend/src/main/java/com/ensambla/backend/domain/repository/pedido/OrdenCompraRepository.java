package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository {

    OrdenCompra save(OrdenCompra orden);

    /** Las 100 órdenes más recientes del negocio, la más nueva primero. */
    List<OrdenCompra> findAllByNegocioId(NegocioId negocioId);

    /** Las 100 órdenes más recientes del negocio en ese estado, la más nueva primero. */
    List<OrdenCompra> findAllByNegocioIdAndEstado(NegocioId negocioId, EstadoOrdenCompra estado);

    Optional<OrdenCompra> findByIdAndNegocioId(OrdenCompraId ordenId, NegocioId negocioId);

    /** La orden del negocio; una de otro negocio responde igual que una inexistente. */
    default OrdenCompra obtener(NegocioId negocioId, OrdenCompraId ordenId) {
        return findByIdAndNegocioId(ordenId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("La orden no existe"));
    }
}
