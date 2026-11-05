package com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.model.reposicion.ItemReposicion;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
import com.ensambla.backend.domain.port.GeneradorPedidoPdfPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Arma en PDF el pedido de reposición que la persona le va a mandar a su proveedor. */
@RequiredArgsConstructor
public class ArmarPedidoProveedor {

    private final NegocioRepository negocioRepository;

    private final ReposicionService reposicionService;

    private final GeneradorPedidoPdfPort generadorPedidoPdfPort;

    private final Clock clock;

    public ArmarPedidoProveedorResponse execute(ArmarPedidoProveedorRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        PedidoReposicion pedido = pedidoDe(negocio, request, hoy());
        return ArmarPedidoProveedorResponse.de(generadorPedidoPdfPort.generar(pedido), pedido);
    }

    private PedidoReposicion pedidoDe(Negocio negocio, ArmarPedidoProveedorRequest request, LocalDate hoy) {
        Faltantes faltantes = reposicionService.faltantesDe(negocio.getId(), request.proyectoId());
        List<ItemReposicion> elegidos = faltantes.elegir(request.cantidadesPorProducto());
        return PedidoReposicion.armar(negocio.getNombre(), request.proveedor(), hoy, elegidos);
    }

    private LocalDate hoy() {
        return ZonaNegocio.dia(OffsetDateTime.now(clock));
    }
}
