package com.ensambla.backend.domain.useCase.orden.listarOrdenes;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Las órdenes de compra del negocio de la sesión, las más recientes primero, con el WhatsApp de cada cliente. */
@RequiredArgsConstructor
public class ListarOrdenes {

    private final NegocioRepository negocioRepository;
    private final OrdenCompraRepository ordenRepository;
    private final ClienteWhatsappRepository clienteRepository;

    public List<OrdenItem> execute(ListarOrdenesRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        List<OrdenCompra> ordenes = request.estado() == null
                ? ordenRepository.findAllByNegocioId(negocioId)
                : ordenRepository.findAllByNegocioIdAndEstado(negocioId, request.estado());
        Map<ClienteId, String> telefonos = telefonosDe(ordenes);
        return ordenes.stream().map(orden -> OrdenItem.de(orden, telefonos.get(orden.getClienteId()))).toList();
    }

    private Map<ClienteId, String> telefonosDe(List<OrdenCompra> ordenes) {
        List<ClienteId> clientes = ordenes.stream().map(OrdenCompra::getClienteId).distinct().toList();
        return clienteRepository.findAllByIds(clientes).stream()
                .collect(Collectors.toMap(ClienteWhatsapp::getId, ClienteWhatsapp::getTelefono));
    }
}
